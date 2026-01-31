package com.example.tfg_apli.network.dto

import com.google.gson.annotations.SerializedName

// --- USUARIO ---
data class UsuarioRegistroDTO(
    val nombre: String,
    val email: String,
    val rol: String,
    val modoInterfaz: String
)

data class UsuarioUpdateDTO(
    val nombre: String,
    val edad: Int?,
    val peso: Double?,
    val alergias: String?
)

data class UsuarioResponseDTO(
    val id: Long,
    val nombre: String,
    val email: String,
    val firebaseUid: String?,
    val rol: String,
    val modoInterfaz: String?,
    val fotoUrl: String?,
    val edad: Int?,
    val peso: Double?,
    val alergias: String?,
    val nombreCuidador: String?
)

data class NuevaPatologiaDTO(
    val nombrePatologia: String,
    val esCronica: Boolean,
    val recomendacionMedica: String?,
    val medicamentoNombre: String?,
    val frecuenciaHoras: Int?
)

// --- MEDICAMENTOS ---
data class MedicamentoRequestDTO(
    val nombre: String,
    val descripcion: String?,
    val dosis: String?,
    val stockAlerta: Int,
    val arModelRef: String?,
    val seniorId: Long,
    val frecuenciaHoras: Int,
    val duracionDias: Int,
    val horaInicio: String?,
    val patologiaId: Long?,
    val codigoNacional: String?
)

data class MedicamentoResponseDTO(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val dosis: String?,
    val stockAlerta: Int?,
    val arModelRef: String?,
    val seniorId: Long,
    val pauta: String?,
    val patologiaId: Long?,
    val frecuenciaHoras: Int?,
    val codigoNacional: String?,
    val horaInicio: String?
)

// --- SALUD Y ANALÍTICAS ---
data class UsuarioPatologia(
    val id: Long,
    val patologia: Patologia,
    val fechaDeteccion: String?,
    val fechaCura: String?,
    val notasMedico: String?,
    val activo: Boolean? = true,
    val medicacionHistorial: String?
)

data class Patologia(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val esCronica: Boolean
)

data class AnaliticaRequestDTO(
    val seniorId: Long,
    val tipoMetrica: String,
    val valor: Double,
    val unidad: String
)

data class AnaliticaResponseDTO(
    val id: Long,
    val tipoMetrica: String,
    val valor: Double,
    val unidad: String,
    val fechaRegistro: String,
)

data class VerificacionTomaResponseDTO(
    val mensaje: String,
    val veredicto: String,
    val medicamento: MedicamentoResponseDTO?
)

data class TomaResponseDTO(
    val id: Long,
    val medicamentoNombre: String,
    val fechaProgramada: String,
    val estado: String
)

data class ConsejoIAResponseDTO(
    val consejo: String
)

data class AsignarRequest(
    val cuidadorId: Long,
    val seniorId: Long
)

data class InformeCumplimientoDTO(
    val totalProgramadas: Long,
    val tomadas: Long,
    val omitidas: Long,
    val adherencia: Double
)

// --- ¡¡AQUÍ ESTABA EL FALLO!! CORREGIDO ---
data class LogEmergencia(
    val id: Long?,

    @SerializedName("tipoEvento")
    val tipo: String,

    @SerializedName("fechaEvento")
    val fecha: String?,

    @SerializedName("ubicacionGps")
    val gps: String?
)


// --- CITAS MÉDICAS ---
data class CitaMedicaDTO(
    val id: Long = 0,
    val seniorId: Long,
    val titulo: String,
    val lugar: String?,
    val fechaHora: String,
    val notificado: Boolean = false
)

// --- CUIDADORES ---
data class PacienteResumenDTO(
    val id: Long,
    val nombre: String,
    val fotoUrl: String?,
    val estadoGeneral: String,
    val mensajeAlerta: String?,
    val adherencia: Int
)

data class VincularPacienteDTO(
    val seniorId: Long
)