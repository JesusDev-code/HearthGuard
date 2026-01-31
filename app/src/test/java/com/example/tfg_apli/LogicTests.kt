package com.example.tfg_apli

import com.example.tfg_apli.util.HealthTipsProvider
import com.example.tfg_apli.util.PatientStatusLevel
import com.example.tfg_apli.util.TriageLogic
import com.example.tfg_apli.network.dto.PacienteResumenDTO
import org.junit.Assert.*
import org.junit.Test

/**
 * PRUEBAS UNITARIAS (RA8 - Pruebas Avanzadas)
 * * Estas pruebas verifican la lógica crítica de negocio sin necesidad de emulador.
 * Garantizan que el sistema de triaje y consejos funciona correctamente.
 */
class LogicTests {

    // --- PRUEBA 1: Lógica de Triaje (TriageLogic.kt) ---
    // Verificamos que si un paciente tiene adherencia baja, el sistema lo marca como CRÍTICO.
    @Test
    fun `triageLogic detecta estado CRITICO si adherencia es muy baja`() {
        // 1. GIVEN (Dado un escenario): Paciente que no se toma la medicación (10%)
        val pacienteGrave = PacienteResumenDTO(
            id = 1,
            nombre = "Juan Test",
            fotoUrl = null,
            estadoGeneral = "ESTABLE", // Aunque diga estable...
            mensajeAlerta = null,
            adherencia = 10 // ... su adherencia es peligrosa
        )

        // 2. WHEN (Cuando): Ejecutamos la lógica de análisis
        val resultado = TriageLogic.analyze(pacienteGrave)

        // 3. THEN (Entonces): El sistema debe forzar el estado CRITICO
        assertEquals("El nivel debería ser CRÍTICO", PatientStatusLevel.CRITICAL, resultado.level)
    }

    // --- PRUEBA 2: Detección de SOS ---
    // Verificamos que un mensaje de SOS invalida cualquier otro estado.
    @Test
    fun `triageLogic prioriza alerta SOS sobre todo lo demas`() {
        // GIVEN: Paciente sano (90% adherencia) pero con alerta de caída
        val pacienteSOS = PacienteResumenDTO(
            id = 2,
            nombre = "Maria SOS",
            fotoUrl = null,
            estadoGeneral = "ESTABLE",
            mensajeAlerta = "SOS: Caída detectada",
            adherencia = 90
        )

        // WHEN
        val resultado = TriageLogic.analyze(pacienteSOS)

        // THEN
        assertEquals(PatientStatusLevel.CRITICAL, resultado.level)
        assertEquals("SOS ACTIVO", resultado.label)
    }

    // --- PRUEBA 3: Motor de Consejos (HealthTipsProvider.kt) ---
    // Verificamos que el sistema nunca devuelve un consejo vacío.
    @Test
    fun `healthTips devuelve consejo valido para modo SENIOR`() {
        // WHEN
        val consejo = HealthTipsProvider.getRandomTip("SENIOR")

        // THEN
        assertNotNull("El consejo no debe ser nulo", consejo)
        assertTrue("El consejo debe tener texto", consejo.isNotEmpty())
        println("Consejo de prueba generado: $consejo")
    }
}