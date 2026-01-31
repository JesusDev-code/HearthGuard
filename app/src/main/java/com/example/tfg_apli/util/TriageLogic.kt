package com.example.tfg_apli.util

import androidx.compose.ui.graphics.Color
import com.example.tfg_apli.network.dto.PacienteResumenDTO

// Estados posibles del paciente
enum class PatientStatusLevel {
    CRITICAL,   // Rojo
    WARNING,    // Amarillo
    STABLE      // Verde
}

// Objeto de resultado visual
data class TriageResult(
    val level: PatientStatusLevel,
    val color: Color,
    val label: String,
    val iconRes: Int? = null
)

object TriageLogic {

    fun analyze(patient: PacienteResumenDTO): TriageResult {
        // 1. REGLA CRÍTICA: Mensajes de alerta directa (SOS, Caída)
        if (!patient.mensajeAlerta.isNullOrBlank() && patient.mensajeAlerta.contains("SOS", ignoreCase = true)) {
            return TriageResult(PatientStatusLevel.CRITICAL, Color(0xFFD32F2F), "SOS ACTIVO")
        }

        // 2. REGLA CRÍTICA: Estado general marcado por servidor o Adherencia muy baja
        if (patient.estadoGeneral == "CRITICO" || patient.adherencia < 30) {
            return TriageResult(PatientStatusLevel.CRITICAL, Color(0xFFD32F2F), "RIESGO ALTO")
        }

        // 3. REGLA ATENCIÓN: Adherencia regular o alertas leves
        if (patient.estadoGeneral == "ALERTA" || patient.adherencia < 80) {
            return TriageResult(PatientStatusLevel.WARNING, Color(0xFFFFA000), "REVISAR")
        }

        // 4. ESTABLE
        return TriageResult(PatientStatusLevel.STABLE, Color(0xFF388E3C), "ESTABLE")
    }
}