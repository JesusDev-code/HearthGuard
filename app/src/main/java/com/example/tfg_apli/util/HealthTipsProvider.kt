package com.example.tfg_apli.util

import java.time.LocalDate

object HealthTipsProvider {

    // 🎯 CONSEJOS PARA PERSONAS MAYORES (SENIOR)
    private val seniorTips = listOf(
        "💧 Bebe agua regularmente aunque no tengas sed. La hidratación es clave para tu salud.",
        "🚶 Camina 15-20 minutos al día. El movimiento mantiene tus articulaciones activas.",
        "🌞 Toma el sol 10 minutos diarios para tu vitamina D. Protege tu piel con crema.",
        "😴 Duerme 7-8 horas cada noche. El descanso reparador mejora tu memoria.",
        "🥗 Come frutas y verduras de colores variados cada día. Nutrición para tu cuerpo.",
        "👥 Mantén contacto con familia y amigos. Las relaciones sociales son medicina.",
        "📖 Lee, haz crucigramas o sudokus. Mantén tu mente activa y ágil.",
        "🦷 Cuida tu salud dental. Cepíllate 3 veces al día y visita al dentista.",
        "👂 Revisa tu audición y visión anualmente. La prevención es importante.",
        "💊 Toma tus medicamentos a la misma hora cada día. La constancia es clave."
    )

    // 🏥 CONSEJOS PARA ENFERMEDADES CRÓNICAS
    private val chronicTips = listOf(
        "📊 Lleva un registro diario de tus síntomas. Ayuda a tu médico a tratarte mejor.",
        "⏰ Mantén horarios regulares para comidas, medicación y descanso.",
        "🧘 Practica técnicas de relajación. El estrés afecta a tu condición crónica.",
        "🥙 Sigue tu dieta prescrita. La alimentación adecuada controla tu enfermedad.",
        "💪 Realiza ejercicio adaptado a tu condición. Consulta con tu médico qué actividades son seguras.",
        "🩺 No faltes a tus controles médicos. El seguimiento previene complicaciones.",
        "🚫 Evita el tabaco y alcohol. Ambos empeoran las enfermedades crónicas.",
        "😊 Mantén una actitud positiva. Tu bienestar mental influye en tu salud física.",
        "🌡️ Controla tus constantes vitales según te indique tu médico: tensión, glucosa, peso.",
        "👨‍⚕️ Comunica cualquier cambio en tu estado a tu equipo médico inmediatamente."
    )

    // 🧠 CONSEJOS PARA SALUD MENTAL
    private val mentalTips = listOf(
        "🧘‍♀️ Practica 5 minutos de respiración profunda. Inhala 4 seg, mantén 4, exhala 4.",
        "📝 Escribe 3 cosas por las que estás agradecido hoy. El agradecimiento alivia la ansiedad.",
        "🚶‍♂️ Sal a caminar sin destino. El movimiento libera endorfinas, las hormonas de la felicidad.",
        "📵 Desconecta 1 hora antes de dormir. Las pantallas alteran tu descanso mental.",
        "🎨 Dedica tiempo a un hobby que disfrutes. La creatividad reduce el estrés.",
        "💬 Habla de tus emociones con alguien de confianza. Compartir alivia la carga emocional.",
        "🛁 Date una ducha o baño relajante. El agua calma la mente y el cuerpo.",
        "🎵 Escucha música que te relaje. El sonido adecuado regula tus emociones.",
        "☕ Limita la cafeína después de las 14h. Puede aumentar tu ansiedad y nerviosismo.",
        "🌱 Establece pequeñas metas diarias. Los logros pequeños construyen confianza."
    )

    /**
     * Obtiene el consejo del día según el modo del usuario.
     * El consejo cambia cada día del mes (1-31).
     *
     * @param mode Modo del usuario: "SENIOR", "CRONICO", "CONTROL", "MENTAL", "APOYO"
     * @return Consejo de salud personalizado del día
     */
    fun getTipOfTheDay(mode: String): String {
        val dayOfMonth = LocalDate.now().dayOfMonth

        return when (mode.uppercase()) {
            "SENIOR" -> seniorTips[dayOfMonth % seniorTips.size]
            "CRONICO", "CONTROL" -> chronicTips[dayOfMonth % chronicTips.size]
            "APOYO", "MENTAL" -> mentalTips[dayOfMonth % mentalTips.size]
            else -> seniorTips[dayOfMonth % seniorTips.size] // Default
        }
    }

    /**
     * Obtiene un consejo aleatorio (para testing)
     */
    fun getRandomTip(mode: String): String {
        return when (mode.uppercase()) {
            "SENIOR" -> seniorTips.random()
            "CRONICO", "CONTROL" -> chronicTips.random()
            "APOYO", "MENTAL" -> mentalTips.random()
            else -> seniorTips.random()
        }
    }
}