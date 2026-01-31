package com.example.tfg_apli.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.CimaRetrofitClient
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.util.LocalIntakeManager
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min



class ScannerViewModel : ViewModel() {

    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val scannerState: StateFlow<ScannerState> = _scannerState

    private val userRole = SessionManager.currentUser?.rol
    private var cachedMeds: List<MedicamentoResponseDTO> = emptyList()

    private var pendingMedId: Long? = null
    private var pendingTimeSlot: String? = null

    private var lastScannedCode: String = ""
    private var lastScanTime: Long = 0

    init {

        resetState()
    }


    private suspend fun fetchMedsLocal() {
        try {
            val userId = SessionManager.currentUser?.id
            if (userId != null) {

                cachedMeds = RetrofitClient.apiService.listarMedicamentosPorSenior(userId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onBarcodeScanned(rawCode: String) {

        if (_scannerState.value !is ScannerState.Idle) return


        val currentTime = System.currentTimeMillis()
        if (rawCode == lastScannedCode && (currentTime - lastScanTime) < 2000) {
            return
        }

        lastScannedCode = rawCode
        lastScanTime = currentTime

        _scannerState.value = ScannerState.Loading

        val cleanCode = extractEANfromGS1(rawCode)
        val isIndependent = try { SessionManager.isSelfManagementEnabled.value } catch (e: Exception) { false }
        val isModeRegister = (userRole == "CUIDADOR") || (userRole == "SENIOR" && isIndependent)

        viewModelScope.launch {
            try {

                val medLocal = cachedMeds.find {
                    it.codigoNacional?.trim()?.trimStart('0') == cleanCode.trim().trimStart('0')
                }

                if (medLocal != null) {

                    val checkTime = checkTimeStrict(medLocal)
                    val uso = if (!medLocal.descripcion.isNullOrBlank())
                        "\nℹ️ Sirve para: ${formatText(medLocal.descripcion.substringBefore("|"))}"
                    else ""

                    if (!checkTime.isTime) {
                        _scannerState.value = ScannerState.VerificationResult(
                            message = " NO ES LA HORA.\nTe toca a las ${checkTime.scheduledTime}$uso",
                            isCorrect = false,
                            medName = medLocal.nombre,
                            showActions = false
                        )
                    } else {
                        pendingMedId = medLocal.id
                        pendingTimeSlot = checkTime.scheduledTime
                        _scannerState.value = ScannerState.VerificationResult(
                            message = " ¡CORRECTO! Toca ahora: ${checkTime.scheduledTime}$uso",
                            isCorrect = true,
                            medName = medLocal.nombre,
                            showActions = true
                        )
                    }
                } else if (!isModeRegister) {

                    val result = RetrofitClient.apiService.verificarToma(cleanCode)
                    _scannerState.value = ScannerState.VerificationResult(
                        message = if(result.veredicto == "TOCA_AHORA") " ¡Correcto!" else "⚠️ ${result.mensaje}",
                        isCorrect = result.veredicto == "TOCA_AHORA",
                        medName = result.medicamento?.nombre ?: "Desconocido",
                        showActions = false
                    )
                } else {

                    val info = RetrofitClient.apiService.buscarMedicamentoPorCN(cleanCode)


                    if (info.seniorId != 0L) {
                        _scannerState.value = ScannerState.Error(" Este medicamento ya está registrado.")
                    } else {
                        val nombreBonito = formatText(info.nombre.substringBefore("+"))
                        val infoLimpia = info.copy(
                            nombre = nombreBonito,
                            descripcion = formatText(info.descripcion?.substringBefore("|") ?: "")
                        )
                        _scannerState.value = ScannerState.MedicationInfo(infoLimpia, cleanCode)
                    }
                }
            } catch (e: HttpException) {
                if (isModeRegister && e.code() == 404) {

                    var nombreCima = ""; var labCima = ""
                    try {
                        val cnParaCima = if (cleanCode.startsWith("847000") && cleanCode.length == 13) cleanCode.substring(6, 12) else cleanCode
                        val resCima = CimaRetrofitClient.apiService.buscarMedicamentos(cn = cnParaCima)
                        if (resCima.resultados.isNotEmpty()) {
                            nombreCima = formatText(resCima.resultados.first().nombre.substringBefore(","))
                            labCima = formatText(resCima.resultados.first().labtitular)
                        }
                    } catch (ex: Exception) { Log.e("CIMA", "Error: ${ex.message}") }

                    val dummyMed = MedicamentoResponseDTO(
                        id = 0L, nombre = nombreCima, descripcion = labCima, dosis = null, stockAlerta = 5,
                        arModelRef = null, seniorId = 0L, pauta = null, patologiaId = null,
                        frecuenciaHoras = 8, codigoNacional = cleanCode, horaInicio = null
                    )
                    _scannerState.value = ScannerState.MedicationInfo(dummyMed, cleanCode)
                } else {
                    _scannerState.value = ScannerState.Error("No se encuentra en la base de datos, asegurese de registrar correctamente el medicamento.")
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun confirmIntake(context: Context) {
        if (pendingMedId == null || pendingTimeSlot == null) return


        LocalIntakeManager.markAsTaken(context, pendingMedId!!, pendingTimeSlot!!)


        viewModelScope.launch {
            try {
                val userId = SessionManager.currentUser?.id
                if (userId == null) {
                    Log.e("Scanner", "No hay usuario logueado")
                    return@launch
                }


                val hoy = java.time.LocalDate.now().toString()


                val med = cachedMeds.find { it.id == pendingMedId }
                if (med == null) {
                    Log.e("Scanner", "Medicamento no encontrado en caché")
                    return@launch
                }


                val tomasHoy = RetrofitClient.apiService.getTomasPorFecha(userId, hoy)


                val tomaCorrespondiente = tomasHoy.find { toma ->

                    toma.medicamentoNombre == med.nombre &&

                            toma.fechaProgramada.split("T").getOrNull(1)?.take(5) == pendingTimeSlot
                }

                if (tomaCorrespondiente != null) {

                    RetrofitClient.apiService.registrarToma(
                        tomaId = tomaCorrespondiente.id,
                        estado = "TOMADO"
                    )
                    Log.d("Scanner", " Toma registrada en backend correctamente: ID=${tomaCorrespondiente.id}")
                } else {
                    Log.w("Scanner", " No se encontró la toma en el backend para ${med.nombre} a las $pendingTimeSlot")
                }

            } catch (e: Exception) {
                Log.e("Scanner", " Error al sincronizar toma con backend: ${e.message}")
                e.printStackTrace()

            }
        }


        resetState()
    }



    fun resetState() {

        _scannerState.value = ScannerState.Loading

        viewModelScope.launch {

            fetchMedsLocal()

            _scannerState.value = ScannerState.Idle

            pendingMedId = null
            pendingTimeSlot = null
        }
    }

    private fun extractEANfromGS1(raw: String): String { if (raw.length <= 13) return raw; val numeric = raw.filter { it.isDigit() }; if (numeric.startsWith("01") && numeric.length >= 16) { val gtin14 = numeric.substring(2, 16); return if (gtin14.startsWith("0")) gtin14.substring(1) else gtin14 }; return raw }
    private fun formatText(text: String): String { if (text.isBlank()) return ""; return text.trim().lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }

    data class TimeCheck(val isTime: Boolean, val scheduledTime: String)
    private fun checkTimeStrict(med: MedicamentoResponseDTO): TimeCheck {
        val now = LocalTime.now(); val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startHourString = med.horaInicio ?: "09:00"
        val startParts = startHourString.split(":").map { it.toInt() }
        val startTime = LocalTime.of(startParts[0], startParts[1])
        val freq = med.frecuenciaHoras ?: 24
        val slots = mutableListOf<LocalTime>(); var slot = startTime; var count = 0
        while (count < (24/freq)) { slots.add(slot); slot = slot.plusHours(freq.toLong()); count++ }
        val closestSlot = slots.minByOrNull { targetTime -> val diff = abs(ChronoUnit.MINUTES.between(targetTime, now)); min(diff, 1440 - diff) } ?: return TimeCheck(false, "Unknown")
        val rawDiff = abs(ChronoUnit.MINUTES.between(closestSlot, now)); val trueDiff = min(rawDiff, 1440 - rawDiff)
        return TimeCheck(trueDiff <= 5, closestSlot.format(formatter))
    }
}

sealed class ScannerState {
    object Idle : ScannerState()
    object Loading : ScannerState()
    data class VerificationResult(val message: String, val isCorrect: Boolean, val medName: String, val showActions: Boolean) : ScannerState()
    data class MedicationInfo(val medication: MedicamentoResponseDTO, val barcode: String) : ScannerState()
    data class Error(val message: String) : ScannerState()
}