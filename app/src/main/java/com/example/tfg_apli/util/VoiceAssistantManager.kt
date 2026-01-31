package com.example.tfg_apli.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.tfg_apli.network.dto.CitaMedicaDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class VoiceAssistantManager(
    private val context: Context,
    private val onListening: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false

    init {
        // Inicializar Text-to-Speech
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("es", "ES")
            }
        }

        // Inicializar Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    fun startListening(citas: List<CitaMedicaDTO>) {
        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "¿Qué necesitas saber?")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onListening(true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onListening(false)
            }

            override fun onError(error: Int) {
                isListening = false
                onListening(false)
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sin permisos de micrófono"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No te he entendido"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No he detectado voz"
                    else -> "Error desconocido"
                }
                onError(errorMsg)
                speak("Lo siento, no te he entendido. Intenta de nuevo.")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onListening(false)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].lowercase()
                    Log.d("VoiceAssistant", "Recognized: $spokenText")
                    processCommand(spokenText, citas)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onListening(false)
            onError("Error al iniciar el reconocimiento de voz")
        }
    }

    private fun processCommand(command: String, citas: List<CitaMedicaDTO>) {
        when {
            // Preguntas sobre citas
            command.contains("cita") || command.contains("médico") ||
                    command.contains("doctor") || command.contains("consulta") -> {
                handleAppointmentQuery(citas)
            }

            // Preguntas sobre hora
            command.contains("qué hora") || command.contains("a qué hora") -> {
                handleAppointmentQuery(citas)
            }

            // Preguntas sobre día/cuándo
            command.contains("cuándo") || command.contains("cuando") ||
                    command.contains("qué día") || command.contains("que dia") -> {
                handleAppointmentQuery(citas)
            }

            // Preguntas sobre dónde
            command.contains("dónde") || command.contains("donde") ||
                    command.contains("lugar") || command.contains("sitio") -> {
                handleAppointmentQuery(citas, focusOnLocation = true)
            }

            // No entendido
            else -> {
                speak("Lo siento, solo puedo ayudarte con información sobre tus citas médicas. Pregúntame cuándo tengo cita, a qué hora, o dónde es.")
            }
        }
    }

    private fun handleAppointmentQuery(citas: List<CitaMedicaDTO>, focusOnLocation: Boolean = false) {
        if (citas.isEmpty()) {
            speak("No tienes ninguna cita programada.")
            return
        }

        // Buscar la próxima cita (la más cercana en el futuro)
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val upcomingCita = citas
            .mapNotNull { cita ->
                try {
                    val citaDateTime = LocalDateTime.parse(cita.fechaHora.substringBefore(".").substringBefore("+"), formatter)
                    if (citaDateTime.isAfter(now)) cita to citaDateTime else null
                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { it.second }

        if (upcomingCita != null) {
            val (cita, dateTime) = upcomingCita

            // Formatear la respuesta de forma natural
            val dayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val dayText = dateTime.format(dayFormatter)
            val timeText = dateTime.format(timeFormatter)
            val location = cita.lugar ?: "ubicación no especificada"
            val title = cita.titulo ?: "cita médica"

            val response = if (focusOnLocation) {
                // Respuesta enfocada en la ubicación
                when {
                    dateTime.toLocalDate() == now.toLocalDate() -> {
                        "Tu cita de hoy es en $location. Es a las $timeText y se trata de $title."
                    }
                    dateTime.toLocalDate() == now.toLocalDate().plusDays(1) -> {
                        "Tu cita de mañana es en $location. Es a las $timeText y se trata de $title."
                    }
                    else -> {
                        "Tu próxima cita es en $location. Es el $dayText a las $timeText y se trata de $title."
                    }
                }
            } else {
                // Respuesta general con toda la información
                when {
                    dateTime.toLocalDate() == now.toLocalDate() -> {
                        "Tienes una cita hoy a las $timeText. Es $title en $location."
                    }
                    dateTime.toLocalDate() == now.toLocalDate().plusDays(1) -> {
                        "Tienes una cita mañana a las $timeText. Es $title en $location."
                    }
                    else -> {
                        "Tu próxima cita es el $dayText a las $timeText. Es $title en $location."
                    }
                }
            }

            speak(response)
        } else {
            speak("No tienes citas próximas programadas.")
        }
    }

    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            onListening(false)
        }
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
