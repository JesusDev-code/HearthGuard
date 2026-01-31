package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.AnaliticaRequestDTO
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.tfg_apli.network.dto.AnaliticaResponseDTO

class BloodPressureViewModel : ViewModel() {

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState


    private val _analiticas = MutableStateFlow<List<AnaliticaResponseDTO>>(emptyList())
    val analiticas: StateFlow<List<AnaliticaResponseDTO>> = _analiticas


    init {
        loadBloodPressureHistory()
    }

    fun registerBloodPressure(systolic: Double, diastolic: Double) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val userId = SessionManager.currentUser?.id ?: throw IllegalStateException("Usuario no logueado")

                // 1. Enviar datos al servidor
                val systolicRequest = AnaliticaRequestDTO(userId, "Tension Sistolica", systolic, "mmHg")
                RetrofitClient.apiService.registrarAnalitica(systolicRequest)

                val diastolicRequest = AnaliticaRequestDTO(userId, "Tension Diastolica", diastolic, "mmHg")
                RetrofitClient.apiService.registrarAnalitica(diastolicRequest)

                // 2. Detectar anomalías
                val isHigh = systolic >= 135 || diastolic >= 85
                val isLow = systolic < 90 || diastolic < 60
                val isDanger = isHigh || isLow

                if (isDanger) {
                    try {
                        val tipoAlerta = if (isHigh) "Tensión elevada" else "Tensión baja"
                        RetrofitClient.apiService.reportarEmergencia(tipo = tipoAlerta, gps = null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. Actualizar estado con la variable 'isDanger'
                _registrationState.value = RegistrationState.Success(systolic, diastolic, isDanger)
                loadBloodPressureHistory()

            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Error al registrar")
            }
        }
    }

    fun resetState() {
        _registrationState.value = RegistrationState.Idle
    }
    private fun loadBloodPressureHistory() {
        viewModelScope.launch {
            try {
                val userId = SessionManager.currentUser?.id ?: return@launch
                val data = RetrofitClient.apiService.obtenerAnaliticasSenior(userId)
                _analiticas.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()

    data class Success(val systolic: Double, val diastolic: Double, val isDanger: Boolean) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}