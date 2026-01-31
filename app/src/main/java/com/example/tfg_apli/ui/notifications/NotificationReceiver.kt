package com.example.tfg_apli.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.tfg_apli.MainActivity
import com.example.tfg_apli.R
import com.example.tfg_apli.ui.screens.AlarmActivity
import com.example.tfg_apli.util.LocalIntakeManager

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "ACTION_DISMISS"
        const val ACTION_TAKE = "ACTION_TAKE"
        const val SHOW_NOTIFICATION = "SHOW_NOTIFICATION"
        // ¡IMPORTANTE! Mismo ID que en MainActivity
        const val CHANNEL_ID = "MEDICATION_CHANNEL_V3"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medId = intent.getLongExtra("MED_ID", -1)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            SHOW_NOTIFICATION -> {
                val medName = intent.getStringExtra("MED_NAME") ?: "Aviso"
                val medDesc = intent.getStringExtra("MED_DESC") ?: ""
                val timeSlot = intent.getStringExtra("TIME_SLOT") ?: ""
                val tipoAviso = intent.getStringExtra("TIPO_AVISO") ?: "MEDICAMENTO"

                showNotification(context, notificationManager, medId, medName, timeSlot, medDesc, tipoAviso)
            }
            ACTION_TAKE -> {
                val medName = intent.getStringExtra("MED_NAME") ?: "Medicación"
                val timeSlot = intent.getStringExtra("TIME_SLOT") ?: ""
                if (medId != -1L && timeSlot.isNotEmpty()) {
                    LocalIntakeManager.markAsTaken(context, medId, timeSlot)
                    Toast.makeText(context, "✅ $medName tomada", Toast.LENGTH_SHORT).show()
                    notificationManager.cancel(medId.toInt())
                }
            }
            ACTION_DISMISS -> {
                notificationManager.cancel(medId.toInt())
            }
        }
    }

    private fun showNotification(
        context: Context,
        manager: NotificationManager,
        medId: Long,
        name: String,
        timeSlot: String,
        desc: String?,
        tipo: String
    ) {
        // Asegurar que el canal existe
        ensureChannelExists(context, manager)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context, medId.toInt(), openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = if (tipo == "CITA") "📅 Cita: $timeSlot" else "⏰ ¡Hora de Medicación!"
        val texto = if (tipo == "CITA") "Tienes cita con $name. $desc" else "Toca tomar: $name. $desc"
        val btnTexto = if (tipo == "CITA") "ENTENDIDO" else "CONFIRMAR TOMA"

        val actionIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = if (tipo == "CITA") ACTION_DISMISS else ACTION_TAKE
            putExtra("MED_ID", medId)
            putExtra("MED_NAME", name)
            putExtra("TIME_SLOT", timeSlot)
        }
        val pendingActionIntent = PendingIntent.getBroadcast(
            context, medId.toInt(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            data = Uri.parse("content://aviso/${medId}")
            putExtra("MED_ID", medId)
            putExtra("MED_NAME", name)
            putExtra("MED_DESC", desc ?: "")
            putExtra("TIME_SLOT", timeSlot)
            putExtra("TIPO_AVISO", tipo)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, (medId * 100).toInt(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // URI segura usando R.raw.alerta_medica
        val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.alerta_medica}")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri) // SONIDO SEGURO
            .setAutoCancel(true)
            .setContentIntent(pendingOpenApp)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_send, btnTexto, pendingActionIntent)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(medId.toInt(), builder.build())
        }
    }

    private fun ensureChannelExists(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Solo creamos si no existe, o si queremos forzar recreación (cambiamos ID a V3)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Avisos de Salud",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarmas de medicación y citas"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)

                    val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.alerta_medica}")
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}