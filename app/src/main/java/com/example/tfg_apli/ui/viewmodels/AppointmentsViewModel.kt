package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.CitaMedicaDTO
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppointmentsViewModel : ViewModel() {

    private val _citasState = MutableStateFlow<CitasState>(CitasState.Loading)
    val citasState: StateFlow<CitasState> = _citasState

    init {
        loadCitas()
    }

    fun loadCitas() {
        viewModelScope.launch {
            _citasState.value = CitasState.Loading
            val userId = SessionManager.currentUser?.id
            if (userId == null) {
                _citasState.value = CitasState.Error("Usuario no identificado")
                return@launch
            }

            try {

                val citas = RetrofitClient.apiService.listarCitas(userId)
                _citasState.value = CitasState.Success(citas)
            } catch (e: Exception) {
                _citasState.value = CitasState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    fun addCita(titulo: String, lugar: String, fechaIso: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = SessionManager.currentUser?.id ?: return@launch
            try {
                val nuevaCita = CitaMedicaDTO(
                    seniorId = userId,
                    titulo = titulo,
                    lugar = lugar,
                    fechaHora = fechaIso
                )

                // 1. GUARDAR EN NUBE
                RetrofitClient.apiService.crearCita(nuevaCita)
                // 2. AVISAR A LA UI PARA QUE PONGA LA ALARMA
                onSuccess()
                // 3. RECARGAR LISTA
                loadCitas()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCita(citaId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.borrarCita(citaId)
                loadCitas() // Recargar tras borrar
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun getCitas(): List<CitaMedicaDTO> {
        return when (val state = _citasState.value) {
            is CitasState.Success -> state.citas
            else -> emptyList()
        }
    }

    sealed class CitasState {
        object Loading : CitasState()
        data class Success(val citas: List<CitaMedicaDTO>) : CitasState()
        data class Error(val message: String) : CitasState()
    }
}
