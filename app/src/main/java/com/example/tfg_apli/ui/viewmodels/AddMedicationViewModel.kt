package com.example.tfg_apli.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.MedicamentoRequestDTO
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AddMedicationViewModel : ViewModel() {

    private val _addState = MutableStateFlow<AddMedicationState>(AddMedicationState.Idle)
    val addState: StateFlow<AddMedicationState> = _addState


    private val _events = Channel<AddMedicationEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun addMedication(
        seniorId: Long,
        nombre: String,
        codigoNacional: String,
        laboratorio: String,
        descripcion: String,
        pauta: String,
        frecuenciaHoras: Int,
        stockAlerta: Int,
        horaInicio: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _addState.value = AddMedicationState.Loading
            try {

                val finalSeniorId = if (seniorId <= 0) {
                    SessionManager.currentUser?.id ?: throw Exception("No se pudo identificar al usuario")
                } else {
                    seniorId
                }


                val request = MedicamentoRequestDTO(
                    nombre = nombre,
                    descripcion = descripcion.ifBlank { "Pauta: $pauta" },
                    dosis = pauta,
                    stockAlerta = stockAlerta,
                    arModelRef = null,
                    seniorId = finalSeniorId,
                    frecuenciaHoras = frecuenciaHoras,
                    duracionDias = 30,
                    horaInicio = horaInicio,
                    patologiaId = null,
                    codigoNacional = codigoNacional
                )


                val nuevoMedicamento = RetrofitClient.apiService.crearMedicamento(request)


                _addState.value = AddMedicationState.Success
                _events.send(AddMedicationEvent.SavedSuccess(nuevoMedicamento))

            } catch (e: Exception) {
                e.printStackTrace()

                val errorMsg = if (e.message?.contains("404") == true) {
                    "Error: Usuario no encontrado en el servidor (ID inválido)"
                } else {
                    "Error al guardar: ${e.message}"
                }
                _addState.value = AddMedicationState.Error(errorMsg)
                _events.send(AddMedicationEvent.ShowError(errorMsg))
            }
        }
    }

    fun resetState() {
        _addState.value = AddMedicationState.Idle
    }
}

sealed class AddMedicationState {
    object Idle : AddMedicationState()
    object Loading : AddMedicationState()
    object Success : AddMedicationState()
    data class Error(val message: String) : AddMedicationState()
}

sealed class AddMedicationEvent {
    data class SavedSuccess(val med: MedicamentoResponseDTO) : AddMedicationEvent()
    data class ShowError(val message: String) : AddMedicationEvent()
}