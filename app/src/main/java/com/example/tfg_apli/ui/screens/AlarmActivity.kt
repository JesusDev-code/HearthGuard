package com.example.tfg_apli.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tfg_apli.util.LocalIntakeManager

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguardOff()

        val medId = intent.getLongExtra("MED_ID", -1)
        val medName = intent.getStringExtra("MED_NAME") ?: "Aviso"
        val medDesc = intent.getStringExtra("MED_DESC") ?: ""
        val timeSlot = intent.getStringExtra("TIME_SLOT") ?: ""


        val tipoAviso = intent.getStringExtra("TIPO_AVISO") ?: "MEDICAMENTO"



        setContent {
            AlarmScreenContent(
                medName = medName,
                medDesc = medDesc,
                timeSlot = timeSlot,
                tipoAviso = tipoAviso,
                onConfirm = {
                    if (tipoAviso == "MEDICAMENTO" && medId != -1L) {
                        LocalIntakeManager.markAsTaken(this, medId, timeSlot)
                    }
                    finishAndRemoveTask()
                },
                onDismiss = {
                    finishAndRemoveTask()
                }
            )
        }
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}

@Composable
fun AlarmScreenContent(
    medName: String,
    medDesc: String,
    timeSlot: String,
    tipoAviso: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    val isCita = tipoAviso == "CITA"

    val backgroundColor = if (isCita) Color(0xFF7B1FA2) else Color(0xFFD32F2F)
    val icon = if (isCita) Icons.Default.Event else Icons.Default.Medication
    val titleText = if (isCita) "RECORDATORIO DE CITA" else "HORA DE MEDICARSE"
    val subtitleText = if (isCita) "Tienes una cita pendiente:" else "Es hora de tomar:"
    val buttonText = if (isCita) "ENTENDIDO" else "YA LA HE TOMADO"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = titleText,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitleText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = medName,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        if (medDesc.isNotEmpty()) {
            Text(
                text = medDesc,
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Hora: $timeSlot",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = if (isCita) Icons.Default.Check else Icons.Default.Medication,
                contentDescription = null,
                tint = backgroundColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                color = backgroundColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onDismiss) {
            Text(
                text = if (isCita) "CERRAR" else "POSPONER 10 MIN",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}