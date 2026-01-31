package com.example.tfg_apli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.ui.viewmodels.BloodPressureViewModel
import com.example.tfg_apli.ui.viewmodels.RegistrationState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CalendarToday
import com.example.tfg_apli.network.dto.AnaliticaResponseDTO

// --- COLORES ---
private val HeaderBlue = Color(0xFF1565C0)
private val HeaderTeal = Color(0xFF00695C)
private val SoftBackground = Color(0xFFF5F7FA)

@Composable
fun BloodPressureScreen(
    viewModel: BloodPressureViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.registrationState.collectAsState()
    val analiticas by viewModel.analiticas.collectAsState()
    var showHistory by remember { mutableStateOf(false) }


    LaunchedEffect(state) {
        if (state is RegistrationState.Success) {
            kotlinx.coroutines.delay(3000)
            viewModel.resetState()
        }
    }



    val sistolicas = analiticas.filter { it.tipoMetrica.contains("Sistolica", true) }.sortedByDescending { it.fechaRegistro }
    val diastolicas = analiticas.filter { it.tipoMetrica.contains("Diastolica", true) }.sortedByDescending { it.fechaRegistro }

    val mediciones = sistolicas.mapNotNull { sistolica ->

        val diastolica = diastolicas.find { dia ->
            kotlin.math.abs(
                sistolica.fechaRegistro.take(16).compareTo(dia.fechaRegistro.take(16))
            ) <= 2
        }

        if (diastolica != null) {
            Triple(sistolica.fechaRegistro, sistolica, diastolica)
        } else null
    }

    val ultimaMedicion = mediciones.firstOrNull()
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }


    Column(modifier = Modifier.fillMaxSize().background(SoftBackground)) {


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(HeaderBlue, HeaderTeal))),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp, start = 8.dp).align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Row(modifier = Modifier.padding(start = 24.dp).align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Registrar tensión", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {


            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = systolic, onValueChange = { systolic = it },
                        label = { Text("Alta (sistólica)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = diastolic, onValueChange = { diastolic = it },
                        label = { Text("Baja (diastólica)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val sys = systolic.toDoubleOrNull(); val dia = diastolic.toDoubleOrNull()
                            if (sys != null && dia != null) viewModel.registerBloodPressure(sys, dia)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GUARDAR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            when (val s = state) {
                is RegistrationState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                is RegistrationState.Success -> {
                    if (s.isDanger) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Color.White)
                                Text(" Registro guardado. Tu cuidador ha sido informado.", color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.fillMaxWidth()) {
                            Text("Guardado correctamente.", color = Color.White, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {}
            }


            if (ultimaMedicion != null) {
                val (_, sistolica, diastolica) = ultimaMedicion
                LastMeasurementCard(sistolica, diastolica)
            }

            Spacer(modifier = Modifier.height(16.dp))


            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { showHistory = !showHistory },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = HeaderBlue, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Historial completo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${mediciones.size} mediciones", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                    Icon(
                        if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = Color.Gray
                    )
                }

                AnimatedVisibility(visible = showHistory) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)) {
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        if (mediciones.isEmpty()) {
                            Text("Sin mediciones registradas", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            mediciones.take(10).forEach { (fecha, sistolica, diastolica) ->
                                HistoryItem(fecha, sistolica, diastolica)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
@Composable
fun LastMeasurementCard(sistolica: AnaliticaResponseDTO, diastolica: AnaliticaResponseDTO) {
    val (statusColor, statusText) = when {
        sistolica.valor >= 135 || diastolica.valor >= 85 -> Pair(Color(0xFFD32F2F), "ALTA")
        sistolica.valor < 90 || diastolica.valor < 60 -> Pair(Color(0xFFFF9800), "BAJA")
        else -> Pair(Color(0xFF4CAF50), "NORMAL")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Favorite, null, tint = statusColor, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Última medición", fontSize = 14.sp, color = Color.Gray)
                Text(
                    "${sistolica.valor.toInt()}/${diastolica.valor.toInt()} mmHg",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    formatDateTime(sistolica.fechaRegistro),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryItem(fecha: String, sistolica: AnaliticaResponseDTO, diastolica: AnaliticaResponseDTO) {
    val (statusColor, statusText) = when {
        sistolica.valor >= 135 || diastolica.valor >= 85 -> Pair(Color(0xFFD32F2F), "ALTA")
        sistolica.valor < 90 || diastolica.valor < 60 -> Pair(Color(0xFFFF9800), "BAJA")
        else -> Pair(Color(0xFF4CAF50), "NORMAL")
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "${sistolica.valor.toInt()}/${diastolica.valor.toInt()} mmHg",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatDateTime(fecha), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Surface(
            color = statusColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun formatDateTime(isoString: String): String {
    return try {
        val parts = isoString.split("T")
        "${parts[0]} • ${parts[1].take(5)}"
    } catch (e: Exception) {
        isoString
    }
}
