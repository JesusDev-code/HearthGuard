package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.PacienteResumenDTO
import com.example.tfg_apli.network.dto.VincularPacienteDTO
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CaregiverDashboardState {
    object Loading : CaregiverDashboardState()
    data class Success(val pacientes: List<PacienteResumenDTO>) : CaregiverDashboardState()
    data class Error(val message: String) : CaregiverDashboardState()
}

class CaregiverViewModel : ViewModel() {

    private val _state = MutableStateFlow<CaregiverDashboardState>(CaregiverDashboardState.Loading)
    val state: StateFlow<CaregiverDashboardState> = _state.asStateFlow()

    private var allPatients: List<PacienteResumenDTO> = emptyList()

    fun loadPacientes(cuidadorId: Long) {
        if (cuidadorId == 0L) return

        viewModelScope.launch {
            _state.value = CaregiverDashboardState.Loading
            try {
                val lista = RetrofitClient.apiService.listarPacientes(cuidadorId)
                allPatients = lista
                _state.value = CaregiverDashboardState.Success(lista)
            } catch (e: Exception) {
                _state.value = CaregiverDashboardState.Error("Error cargando pacientes: ${e.message}")
            }
        }
    }

    fun filterPacientes(query: String) {
        val currentState = _state.value
        if (currentState is CaregiverDashboardState.Success || currentState is CaregiverDashboardState.Loading) {
            val filtered = if (query.isBlank()) {
                allPatients
            } else {
                allPatients.filter {
                    it.nombre.contains(query, ignoreCase = true)
                }
            }
            _state.value = CaregiverDashboardState.Success(filtered)
        }
    }

    fun vincularPaciente(cuidadorId: Long, seniorId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val dto = VincularPacienteDTO(seniorId)
                RetrofitClient.apiService.vincularPaciente(cuidadorId, dto)
                loadPacientes(cuidadorId)
                onSuccess()
            } catch (e: Exception) {
                onError("No se pudo vincular: ${e.message}")
            }
        }
    }


    fun desvincularPaciente(seniorId: Long) {
        viewModelScope.launch {
            try {

                RetrofitClient.apiService.desvincularPaciente(seniorId)


                val currentId = SessionManager.currentUser?.id ?: 0L
                loadPacientes(currentId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}