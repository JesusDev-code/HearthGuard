package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.MedicamentoRequestDTO
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.network.dto.NuevaPatologiaDTO
import com.example.tfg_apli.network.dto.UsuarioPatologia
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


sealed class MyHealthEvent {
    data class MedicationAdded(val med: MedicamentoResponseDTO) : MyHealthEvent()
    data class Error(val message: String) : MyHealthEvent()
}

class MyHealthViewModel : ViewModel() {

    private val _healthScreenState = MutableStateFlow<HealthScreenState>(HealthScreenState.Loading)
    val healthScreenState: StateFlow<HealthScreenState> = _healthScreenState

    // --- NUEVO: Canal de eventos ---
    private val _events = Channel<MyHealthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _healthScreenState.value = HealthScreenState.Loading
            val userId = SessionManager.currentUser?.id

            if (userId == null) {
                _healthScreenState.value = HealthScreenState.Error("Usuario no logueado")
                return@launch
            }

            try {
                // Carga en paralelo
                val patologias = RetrofitClient.apiService.getPerfilSalud(userId)
                val medicamentos = RetrofitClient.apiService.listarMedicamentosPorSenior(userId)

                // IA Tip Dummy
                val aiTip = try {
                    RetrofitClient.apiService.getConsejoIA().consejo
                } catch (e: Exception) {
                    "Mantente hidratado y sigue tus horarios."
                }

                _healthScreenState.value = HealthScreenState.Success(patologias, medicamentos, aiTip)
            } catch (e: Exception) {
                _healthScreenState.value = HealthScreenState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    // --- FUNCIÓN PRINCIPAL ---
    fun guardarPatologiaCompleta(
        nombre: String,
        esCronica: Boolean,
        nota: String?,
        medNombre: String?,
        medCodigo: String?,
        medFrecuencia: Int?,
        medHoraInicio: String?
    ) {
        viewModelScope.launch {
            val userId = SessionManager.currentUser?.id ?: return@launch
            try {
                // 1. CREAR PATOLOGÍA
                val nuevaPatologia = NuevaPatologiaDTO(
                    nombrePatologia = nombre,
                    esCronica = esCronica,
                    recomendacionMedica = nota,
                    medicamentoNombre = null,
                    frecuenciaHoras = null
                )

                val patologiaCreada = RetrofitClient.apiService.registrarNuevaPatologia(userId, nuevaPatologia)

                // 2. CREAR MEDICAMENTO VINCULADO
                if (medNombre != null && medFrecuencia != null) {
                    val nuevoMed = MedicamentoRequestDTO(
                        nombre = medNombre,


                        descripcion = nombre,
                        dosis = "1 unidad",
                        stockAlerta = 5,
                        arModelRef = null,
                        seniorId = userId,
                        frecuenciaHoras = medFrecuencia,
                        duracionDias = 30,
                        horaInicio = medHoraInicio ?: "09:00",
                        patologiaId = patologiaCreada.patologia.id,
                        codigoNacional = medCodigo ?: "000000"
                    )

                    // --- NUEVO: Capturamos el objeto creado ---
                    val medCreado = RetrofitClient.apiService.crearMedicamento(nuevoMed)

                    // --- NUEVO: Enviamos evento para programar alarma ---
                    _events.send(MyHealthEvent.MedicationAdded(medCreado))
                }

                loadData()

            } catch (e: Exception) {
                e.printStackTrace()
                _events.send(MyHealthEvent.Error("Error al guardar: ${e.message}"))
            }
        }
    }

    fun markAsCured(id: Long) {
        viewModelScope.launch {
            try { RetrofitClient.apiService.curar(id); loadData() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            try { RetrofitClient.apiService.borrarMedicamento(id); loadData() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun agregarMedicamentoAPatologia(patId: Long, nombre: String, freq: Int) {
        viewModelScope.launch {
            val userId = SessionManager.currentUser?.id ?: return@launch
            try {
                val nuevoMed = MedicamentoRequestDTO(
                    nombre = nombre,
                    // Cambiamos "Añadido manualmente" por algo más bonito
                    descripcion = "Tratamiento adicional",
                    dosis = "1", stockAlerta = 5, arModelRef = null,
                    seniorId = userId, frecuenciaHoras = freq, duracionDias = 30,
                    horaInicio = "09:00", patologiaId = patId, codigoNacional = "000000"
                )

                val medCreado = RetrofitClient.apiService.crearMedicamento(nuevoMed)
                _events.send(MyHealthEvent.MedicationAdded(medCreado))

                loadData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Compatibilidad
    fun guardarPatologia(nombre: String, esCronica: Boolean, nota: String, medNombre: String?, frecuencia: Int?) {
        guardarPatologiaCompleta(nombre, esCronica, nota, medNombre, "000000", frecuencia, "09:00")
    }
}

sealed class HealthScreenState {
    object Loading : HealthScreenState()
    data class Success(
        val pathologies: List<UsuarioPatologia>,
        val medications: List<MedicamentoResponseDTO>,
        val aiTip: String
    ) : HealthScreenState()
    data class Error(val message: String) : HealthScreenState()
}