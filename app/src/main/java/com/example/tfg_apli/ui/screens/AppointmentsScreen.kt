package com.example.tfg_apli.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.network.dto.CitaMedicaDTO
import com.example.tfg_apli.ui.viewmodels.AppointmentsViewModel
import com.example.tfg_apli.util.AlarmScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentsScreen(
    onBack: () -> Unit,
    viewModel: AppointmentsViewModel = viewModel()
) {
    val citasState by viewModel.citasState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Colores y Estilo
    val azulOscuro = Color(0xFF1565C0)
    val azulClaro = Color(0xFFE3F2FD)

    // Gradiente de fondo
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color.White, azulClaro)
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // CABECERA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = azulOscuro)
                }

                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Citas Médicas",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulOscuro
                )
            }


            when (citasState) {
                is AppointmentsViewModel.CitasState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = azulOscuro)
                    }
                }
                is AppointmentsViewModel.CitasState.Error -> {
                    val error = (citasState as AppointmentsViewModel.CitasState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = error, color = Color.Red, fontSize = 16.sp)
                    }
                }
                is AppointmentsViewModel.CitasState.Success -> {
                    val citas = (citasState as AppointmentsViewModel.CitasState.Success).citas
                    if (citas.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.EventNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.Gray.copy(alpha = 0.3f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No tienes citas próximas",
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(citas) { cita ->
                                AppointmentItem(
                                    cita = cita,

                                    onDelete = {
                                        cita.id?.let { citaId ->
                                            viewModel.deleteCita(citaId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = azulOscuro)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AÑADIR NUEVA CITA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showDialog) {
            AddAppointmentDialog(
                onDismiss = { showDialog = false },
                onConfirm = { titulo, lugar, fechaIso ->
                    viewModel.addCita(titulo, lugar, fechaIso) {

                    }
                }
            )
        }
    }
}

@Composable
fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }


    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var hora by remember { mutableStateOf(LocalTime.now().plusHours(1)) }
    val context = LocalContext.current

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            fecha = LocalDate.of(year, month + 1, dayOfMonth)
        },
        fecha.year, fecha.monthValue - 1, fecha.dayOfMonth
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            hora = LocalTime.of(hourOfDay, minute)
        },
        hora.hour, hora.minute, true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Cita Médica", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Motivo (ej: Cardiólogo)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lugar,
                    onValueChange = { lugar = it },
                    label = { Text("Lugar (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fecha: ${fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                    Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector Hora
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() }
                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hora: ${hora.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                    Icon(Icons.Default.AccessTime, null, tint = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank()) {
                        val fechaIso = "${fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)}T${
                            hora.format(DateTimeFormatter.ofPattern("HH:mm"))
                        }:00"


                        onConfirm(titulo, lugar, fechaIso)


                        AlarmScheduler.scheduleAppointmentAlarm(
                            context,
                            titulo,
                            fechaIso
                        )

                        onDismiss()
                    } else {
                        Toast.makeText(context, "Escribe el motivo de la cita", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AppointmentItem(cita: CitaMedicaDTO, onDelete: () -> Unit) {
    val azulOscuro = Color(0xFF1565C0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cita.titulo ?: "Cita médica",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulOscuro
                )

                Spacer(modifier = Modifier.height(4.dp))

                val fechaBonita = try {
                    val partes = cita.fechaHora.split("T")
                    if (partes.size >= 2) {
                        "${partes[0]} a las ${partes[1].substring(0, 5)}"
                    } else {
                        cita.fechaHora
                    }
                } catch (e: Exception) {
                    cita.fechaHora
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, modifier = Modifier.size(18.dp), tint = azulOscuro)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(fechaBonita, fontSize = 16.sp, color = Color.Gray)
                }

                if (!cita.lugar.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cita.lugar, fontSize = 15.sp, color = Color.Gray)
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Borrar", tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}
