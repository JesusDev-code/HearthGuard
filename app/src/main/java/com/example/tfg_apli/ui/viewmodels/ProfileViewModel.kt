package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.UsuarioPatologia
import com.example.tfg_apli.network.dto.UsuarioResponseDTO
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.tfg_apli.network.dto.UsuarioUpdateDTO

class ProfileViewModel : ViewModel() {

    private val _user = MutableStateFlow(SessionManager.currentUser)
    val user: StateFlow<UsuarioResponseDTO?> = _user

    private val _medicalHistory = MutableStateFlow<MedicalHistoryState>(MedicalHistoryState.Loading)
    val medicalHistory: StateFlow<MedicalHistoryState> = _medicalHistory

    private val _logoutState = MutableStateFlow(false)
    val logoutState: StateFlow<Boolean> = _logoutState

    init {
        fetchMedicalHistory()
    }

    private fun fetchMedicalHistory() {
        viewModelScope.launch {
            _medicalHistory.value = MedicalHistoryState.Loading
            val userId = _user.value?.id
            if (userId == null) {
                _medicalHistory.value = MedicalHistoryState.Error("Usuario no encontrado")
                return@launch
            }
            try {
                val historial = RetrofitClient.apiService.getHistorialSalud(userId)
                _medicalHistory.value = MedicalHistoryState.Success(historial)
            } catch (e: Exception) {
                _medicalHistory.value = MedicalHistoryState.Error(e.message ?: "Error al cargar historial")
            }
        }
    }

    fun updateProfile(nombre: String, edad: Int?, peso: Double?, alergias: String?) {
        viewModelScope.launch {
            try {

                val updateDTO = UsuarioUpdateDTO(
                    nombre = nombre,
                    edad = edad,
                    peso = peso,
                    alergias = alergias
                )


                val updated = RetrofitClient.apiService.actualizarPerfil(updateDTO)


                _user.value = updated
                SessionManager.currentUser = updated

            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    fun desvincularCuidador() {
        viewModelScope.launch {
            try {
                val myId = _user.value?.id ?: return@launch


                RetrofitClient.apiService.desvincularMiCuidador()


                val usuarioActualizado = _user.value?.copy(nombreCuidador = null)
                _user.value = usuarioActualizado
                SessionManager.currentUser = usuarioActualizado

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        SessionManager.logout()
        _logoutState.value = true
    }
}

sealed class MedicalHistoryState {
    object Loading : MedicalHistoryState()
    data class Success(val pathologies: List<UsuarioPatologia>) : MedicalHistoryState()
    data class Error(val message: String) : MedicalHistoryState()
}