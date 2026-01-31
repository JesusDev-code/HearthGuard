package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.network.dto.UsuarioResponseDTO
import com.example.tfg_apli.util.SessionManager
import com.example.tfg_apli.util.HealthTipsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    // Estado del Usuario
    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState

    // Estado de Medicación
    private val _medicationState = MutableStateFlow<MedicationListState>(MedicationListState.Loading)
    val medicationState: StateFlow<MedicationListState> = _medicationState

    // Estado de IA
    private val _aiTipState = MutableStateFlow<AiTipState>(AiTipState.Loading)
    val aiTipState: StateFlow<AiTipState> = _aiTipState

    // Estado de Emergencia
    private val _emergencyState = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState

    // Estado de Tensión Arterial
    private val _bloodPressureState = MutableStateFlow<BloodPressureState>(BloodPressureState.Loading)
    val bloodPressureState: StateFlow<BloodPressureState> = _bloodPressureState

    val userMode = SessionManager.currentUser?.modoInterfaz ?: "SENIOR"

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        SessionManager.currentUser?.let { user ->
            _userState.value = UserState.UserSuccess(user)
            fetchAiTip()
            fetchActiveMedications(user.id)
            fetchBloodPressure(user.id)
        }
    }

    // --- Carga de Medicación ---
    private fun fetchActiveMedications(userId: Long) {
        viewModelScope.launch {
            _medicationState.value = MedicationListState.Loading
            try {
                val meds = RetrofitClient.apiService.listarMedicamentosPorSenior(userId)
                _medicationState.value = MedicationListState.Success(meds)
            } catch (e: Exception) {
                _medicationState.value = MedicationListState.Error("No se pudo cargar la medicación.")
            }
        }
    }

    // Carga de Tensión Arterial
    private fun fetchBloodPressure(userId: Long) {
        viewModelScope.launch {
            _bloodPressureState.value = BloodPressureState.Loading
            try {
                val analiticas = RetrofitClient.apiService.obtenerAnaliticasSenior(userId)

                // Filtrar solo las analíticas de tensión
                val sistolica = analiticas
                    .filter { it.tipoMetrica.contains("Sistolica", ignoreCase = true) }
                    .maxByOrNull { it.fechaRegistro }

                val diastolica = analiticas
                    .filter { it.tipoMetrica.contains("Diastolica", ignoreCase = true) }
                    .maxByOrNull { it.fechaRegistro }

                if (sistolica != null && diastolica != null) {
                    _bloodPressureState.value = BloodPressureState.Success(
                        systolic = sistolica.valor.toInt(),
                        diastolic = diastolica.valor.toInt()
                    )
                } else {
                    _bloodPressureState.value = BloodPressureState.NoData
                }
            } catch (e: Exception) {
                _bloodPressureState.value = BloodPressureState.Error("Error al cargar tensión")
            }
        }
    }

    // --- Consejo IA ---
    private fun fetchAiTip() {
        viewModelScope.launch {
            _aiTipState.value = AiTipState.Loading
            try {
                val response = RetrofitClient.apiService.getConsejoIA()
                _aiTipState.value = AiTipState.AiTipSuccess(response.consejo)
            } catch (e: Exception) {
                val localTip = HealthTipsProvider.getTipOfTheDay(userMode)
                _aiTipState.value = AiTipState.AiTipSuccess(localTip)
            }
        }
    }

    // --- Botón SOS ---
    fun reportEmergency(type: String, gps: String? = null) {
        viewModelScope.launch {
            _emergencyState.value = EmergencyState.Loading
            try {
                RetrofitClient.apiService.reportarEmergencia(tipo = type, gps = gps)
                _emergencyState.value = EmergencyState.EmergencySuccess
            } catch (e: Exception) {
                _emergencyState.value = EmergencyState.Error(e.message ?: "Error al enviar alerta.")
            }
        }
    }

    fun resetEmergencyState() {
        _emergencyState.value = EmergencyState.Idle
    }

    // --- Definición de Estados ---
    sealed class UserState {
        object Loading : UserState()
        data class UserSuccess(val user: UsuarioResponseDTO) : UserState()
        data class Error(val message: String) : UserState()
    }

    sealed class MedicationListState {
        object Loading : MedicationListState()
        data class Success(val medications: List<MedicamentoResponseDTO>) : MedicationListState()
        data class Error(val message: String) : MedicationListState()
    }

    sealed class AiTipState {
        object Loading : AiTipState()
        data class AiTipSuccess(val tip: String) : AiTipState()
        data class Error(val message: String) : AiTipState()
    }

    sealed class EmergencyState {
        object Idle : EmergencyState()
        object Loading : EmergencyState()
        object EmergencySuccess : EmergencyState()
        data class Error(val message: String) : EmergencyState()
    }

    //Estado de Tensión Arterial
    sealed class BloodPressureState {
        object Loading : BloodPressureState()
        data class Success(val systolic: Int, val diastolic: Int) : BloodPressureState()
        object NoData : BloodPressureState()
        data class Error(val message: String) : BloodPressureState()
    }
}
