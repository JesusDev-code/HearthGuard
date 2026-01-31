package com.example.tfg_apli.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.ui.notifications.NotificationReceiver
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarmsForMedication(context: Context, med: MedicamentoResponseDTO) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val freq = med.frecuenciaHoras ?: 24


        val startHourString = med.horaInicio ?: "09:00"
        val parts = startHourString.split(":")
        val startH = parts[0].toInt()
        val startM = parts[1].toInt()


        var slotTime = LocalTime.of(startH, startM)
        val timesPerDay = (24 / freq).coerceAtLeast(1)

        for (i in 0 until timesPerDay) {

            scheduleSingleAlarm(context, alarmManager, med, slotTime, i)

            slotTime = slotTime.plusHours(freq.toLong())
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        med: MedicamentoResponseDTO,
        time: LocalTime,
        index: Int
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.SHOW_NOTIFICATION
            putExtra("MED_ID", med.id)
            putExtra("MED_NAME", med.nombre)
            putExtra("MED_DESC", med.descripcion ?: "")
            putExtra("TIME_SLOT", time.format(DateTimeFormatter.ofPattern("HH:mm")))
            putExtra("TIPO_AVISO", "MEDICAMENTO")
        }


        val uniqueId = (med.id * 10000 + index).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }


        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        scheduleAlarmWithFallback(context, alarmManager, calendar.timeInMillis, pendingIntent)
    }

    // --- CITAS MÉDICAS ---
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAppointmentAlarm(context: Context, titulo: String, fechaIso: String) {

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


            val parts = fechaIso.split("T")
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")

            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()

            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()


            val timeString = String.format("%02d:%02d", hour, minute)


            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.SHOW_NOTIFICATION
                putExtra("MED_ID", System.currentTimeMillis())
                putExtra("MED_NAME", "Cita Médica")
                putExtra("MED_DESC", titulo)
                putExtra("TIME_SLOT", timeString)
                putExtra("TIPO_AVISO", "CITA")
            }

            val uniqueId = titulo.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) return

            scheduleAlarmWithFallback(context, alarmManager, calendar.timeInMillis, pendingIntent)
            Toast.makeText(context, "Cita programada", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun scheduleAlarmWithFallback(
        context: Context,
        alarmManager: AlarmManager,
        timeInMillis: Long,
        pendingIntent: PendingIntent
    ) {

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }


        if (!canScheduleExact) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                10 * 60 * 1000L,
                pendingIntent
            )
            return
        }


        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                10 * 60 * 1000L,
                pendingIntent
            )
        }
    }
}