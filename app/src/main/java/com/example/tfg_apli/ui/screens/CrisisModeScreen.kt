package com.example.tfg_apli.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrisisModeScreen(onBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()

    // Animación de Tamaño
    val size by infiniteTransition.animateFloat(
        initialValue = 150f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Sincronización del Texto
    val isBreathingInState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 8000
                0f at 0
                1f at 4000
                0f at 8000
            },
            repeatMode = RepeatMode.Restart
        )
    )

    val instructionText = if (isBreathingInState < 0.5f) "Inhala suavemente..." else "Exhala despacio..."
    val calmBackgroundColor = Color(0xFFE0F7FA)
    val calmAccentColor = Color(0xFF26C6DA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(calmBackgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Modo calma", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sigue el ritmo de la respiración", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF00838F))
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
            Box(modifier = Modifier.size(300.dp).clip(CircleShape).border(2.dp, calmAccentColor.copy(alpha = 0.3f), CircleShape))
            Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(calmAccentColor.copy(alpha = 0.6f)))
            Text(text = instructionText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0097A7)),
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Me siento mejor", fontSize = 18.sp)
        }
    }
}