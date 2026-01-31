package com.example.tfg_apli.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tfg_apli.network.client.RetrofitClient
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.example.tfg_apli.R
import com.example.tfg_apli.MainActivity
import android.app.PendingIntent

class CaregiverAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Obtenemos el ID del cuidador guardado (asumiendo que sigue logueado)
            val cuidadorId = SessionManager.currentUser?.id
            if (cuidadorId == null) return Result.success()

            // 2. Consultamos la lista de pacientes
            val pacientes = RetrofitClient.apiService.listarPacientes(cuidadorId)

            // 3. Buscamos emergencias
            val emergencias = pacientes.filter {
                it.estadoGeneral == "CRITICO" ||
                        (it.mensajeAlerta?.contains("SOS", true) == true)
            }

            if (emergencias.isNotEmpty()) {
                // 4. Lanzamos notificación por CADA emergencia encontrada
                emergencias.forEach { paciente ->
                    lanzarNotificacionEmergencia(applicationContext, paciente.nombre, paciente.mensajeAlerta ?: "Alerta crítica")
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun lanzarNotificacionEmergencia(context: Context, nombre: String, mensaje: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir la app directamente
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "MEDICATION_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 ¡SOS: $nombre!")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()


        notificationManager.notify(nombre.hashCode(), notification)
    }
}