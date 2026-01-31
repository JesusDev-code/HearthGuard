package com.example.tfg_apli.util

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.example.tfg_apli.network.client.RetrofitClient



object LocalIntakeManager {
    private const val PREFS_NAME = "daily_intakes_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }


    private fun getKey(medId: Long, timeSlot: String): String {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return "${today}_${medId}_${timeSlot}"
    }

    // Marca una pastilla como tomada hoy
    fun markAsTaken(context: Context, medId: Long, timeSlot: String) {
        // 1. Guarda local
        getPrefs(context).edit().putBoolean(getKey(medId, timeSlot), true).apply()

        // 2. Sincroniza con backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Busca la toma correspondiente
                val userId = SessionManager.currentUser?.id ?: return@launch
                val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val tomas = RetrofitClient.apiService.getTomasPorFecha(userId, today)

                // Encuentra la toma por hora programada
                val toma = tomas.firstOrNull {
                    it.fechaProgramada?.contains(timeSlot) == true &&
                            it.estado == "PENDIENTE"
                }

                if (toma?.id != null) {

                    RetrofitClient.apiService.registrarToma(toma.id, "TOMADA")
                    Log.d("Scanner", " Toma ${toma.id} sincronizada con backend")
                }
            } catch (e: Exception) {
                Log.e("Scanner", "Error al sincronizar toma con backend", e)
            }
        }
    }

    // Comprueba si ya te has tomado esa pastilla hoy
    fun isTaken(context: Context, medId: Long, timeSlot: String): Boolean {
        return getPrefs(context).getBoolean(getKey(medId, timeSlot), false)
    }
}