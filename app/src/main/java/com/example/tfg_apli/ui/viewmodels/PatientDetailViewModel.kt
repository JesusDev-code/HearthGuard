package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate

sealed class PatientDetailState {
    object Loading : PatientDetailState()
    data class Success(
        val patientInfo: UsuarioResponseDTO? = null, // ✅ NUEVO
        val medicamentos: List<MedicamentoResponseDTO>,
        val historialSalud: List<UsuarioPatologia>,
        val alertas: List<LogEmergencia>,
        val citas: List<CitaMedicaDTO>,
        val tomasHoy: List<TomaResponseDTO>,
        val analiticas: List<AnaliticaResponseDTO> = emptyList()
    ) : PatientDetailState()
    data class Error(val message: String) : PatientDetailState()
}

class PatientDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow<PatientDetailState>(PatientDetailState.Loading)
    val state: StateFlow<PatientDetailState> = _state.asStateFlow()

    fun loadData(seniorId: Long) {
        viewModelScope.launch {
            _state.value = PatientDetailState.Loading
            try {
                supervisorScope {
                    // ✅ NUEVO: Cargar datos del paciente
                    val patientData = async {
                        try {
                            RetrofitClient.apiService.obtenerUsuarioPorId(seniorId)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val meds = async {
                        try {
                            RetrofitClient.apiService.listarMedicamentosPorSenior(seniorId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val history = async {
                        try {
                            RetrofitClient.apiService.getHistorialSalud(seniorId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val alerts = async {
                        try {
                            RetrofitClient.apiService.listarAlertas(seniorId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val appts = async {
                        try {
                            RetrofitClient.apiService.listarCitas(seniorId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val intakes = async {
                        try {
                            val hoy = LocalDate.now().toString()
                            RetrofitClient.apiService.getTomasPorFecha(seniorId, hoy)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val analytics = async {
                        try {
                            RetrofitClient.apiService.obtenerAnaliticasSenior(seniorId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    _state.value = PatientDetailState.Success(
                        patientInfo = patientData.await(), // ✅ NUEVO
                        medicamentos = meds.await(),
                        historialSalud = history.await(),
                        alertas = alerts.await(),
                        citas = appts.await(),
                        tomasHoy = intakes.await(),
                        analiticas = analytics.await()
                    )
                }
            } catch (e: Exception) {
                _state.value = PatientDetailState.Error(e.message ?: "Error cargando datos")
            }
        }
    }

    fun guardarPatologiaCompleta(
        seniorId: Long,
        nombre: String,
        esCronica: Boolean,
        nota: String,
        medNombre: String?,
        frecuencia: Int?,
        horaInicio: String,
        codigoNacional: String
    ) {
        viewModelScope.launch {
            try {
                val dto = NuevaPatologiaDTO(
                    nombrePatologia = nombre,
                    esCronica = esCronica,
                    recomendacionMedica = nota,
                    medicamentoNombre = null,
                    frecuenciaHoras = null
                )

                val patologiaCreada = RetrofitClient.apiService.registrarNuevaPatologia(seniorId, dto)
                if (!medNombre.isNullOrBlank() && frecuencia != null) {
                    val idParaVincular = patologiaCreada.patologia.id
                    val nuevoMed = MedicamentoRequestDTO(
                        nombre = medNombre,
                        descripcion = "Asociado a: $nombre",
                        dosis = "1 dosis",
                        stockAlerta = 5,
                        arModelRef = null,
                        seniorId = seniorId,
                        frecuenciaHoras = frecuencia,
                        duracionDias = 30,
                        horaInicio = horaInicio,
                        patologiaId = idParaVincular,
                        codigoNacional = codigoNacional
                    )
                    RetrofitClient.apiService.crearMedicamento(nuevoMed)
                }
                loadData(seniorId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registrarMedicamento(
        seniorId: Long,
        nombre: String,
        dosis: String,
        freq: Int,
        horaInicio: String,
        codigoNacional: String
    ) {
        viewModelScope.launch {
            try {
                val nuevoMed = MedicamentoRequestDTO(
                    nombre = nombre,
                    descripcion = "Añadido por cuidador",
                    dosis = dosis,
                    stockAlerta = 5,
                    arModelRef = null,
                    seniorId = seniorId,
                    frecuenciaHoras = freq,
                    duracionDias = 30,
                    horaInicio = horaInicio,
                    patologiaId = null,
                    codigoNacional = codigoNacional
                )
                RetrofitClient.apiService.crearMedicamento(nuevoMed)
                loadData(seniorId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registrarCita(seniorId: Long, titulo: String, lugar: String, fecha: String, hora: String) {
        viewModelScope.launch {
            try {
                val dto = CitaMedicaDTO(
                    seniorId = seniorId,
                    titulo = titulo,
                    lugar = lugar,
                    fechaHora = "${fecha}T${hora}:00"
                )
                RetrofitClient.apiService.crearCita(dto)
                loadData(seniorId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsCured(seniorId: Long, patologiaId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.curar(patologiaId)
                loadData(seniorId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarMedicamento(seniorId: Long, medicamentoId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.borrarMedicamento(medicamentoId)
                loadData(seniorId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registrarTensionArterial(seniorId: Long, sistolica: Double, diastolica: Double) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.registrarAnalitica(
                    AnaliticaRequestDTO(
                        seniorId = seniorId,
                        tipoMetrica = "Sistolica",
                        valor = sistolica,
                        unidad = "mmHg"
                    )
                )
                RetrofitClient.apiService.registrarAnalitica(
                    AnaliticaRequestDTO(
                        seniorId = seniorId,
                        tipoMetrica = "Diastolica",
                        valor = diastolica,
                        unidad = "mmHg"
                    )
                )
                loadData(seniorId)
            } catch (e: Exception) {
                android.util.Log.e("PatientDetailVM", "Error registrando tensión del senior $seniorId: ${e.message}")
            }
        }
    }
}
