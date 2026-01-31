package com.example.tfg_apli.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.ui.viewmodels.AddMedicationEvent
import com.example.tfg_apli.ui.viewmodels.AddMedicationState
import com.example.tfg_apli.ui.viewmodels.AddMedicationViewModel
import com.example.tfg_apli.util.AlarmScheduler
import com.example.tfg_apli.util.SessionManager
import java.util.Calendar
import java.util.Locale

private val HeaderBlue = Color(0xFF1565C0)
private val HeaderTeal = Color(0xFF00695C)
private val SoftBackground = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    seniorId: Long,
    nombre: String,
    codigoNacional: String,
    laboratorio: String,
    onMedicationAdded: () -> Unit,
    onBack: () -> Unit
) {
    val addMedicationViewModel: AddMedicationViewModel = viewModel()
    val addState by addMedicationViewModel.addState.collectAsState()
    val context = LocalContext.current
    val realSeniorId = if (seniorId == 0L) SessionManager.currentUser?.id ?: 0L else seniorId

    var nameState by remember { mutableStateOf(nombre) }
    var labState by remember { mutableStateOf(laboratorio) }
    var descriptionState by remember { mutableStateOf("") }
    var pauta by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("8") }
    var stockAlerta by remember { mutableStateOf("5") }
    var horaInicio by remember { mutableStateOf("09:00") }

    // Bloqueo manual del botón al pulsar
    var botonPulsado by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            horaInicio = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    // LÓGICA DE EVENTOS SIMPLE: ÉXITO -> CERRAR
    LaunchedEffect(Unit) {
        addMedicationViewModel.events.collect { event ->
            when (event) {
                is AddMedicationEvent.SavedSuccess -> {
                    try {
                        AlarmScheduler.scheduleAlarmsForMedication(context, event.med)
                    } catch (e: Exception) { e.printStackTrace() }

                    Toast.makeText(context, "Guardado correctamente", Toast.LENGTH_SHORT).show()

                    // ¡AQUÍ ESTÁ EL CIERRE! Simple y directo.
                    onBack()
                }
                is AddMedicationEvent.ShowError -> {
                    botonPulsado = false // Reactivamos botón por si quiere reintentar
                    Toast.makeText(context, " Error: ${event.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SoftBackground)) {
        // Cabecera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(HeaderBlue, HeaderTeal))),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 16.dp, start = 8.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Row(
                modifier = Modifier.padding(start = 24.dp).align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Nuevo medicamento", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Formulario
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Datos del fármaco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HeaderBlue)
                    OutlinedTextField(value = nameState, onValueChange = { nameState = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = labState, onValueChange = { labState = it }, label = { Text("Laboratorio") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = descriptionState, onValueChange = { descriptionState = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = codigoNacional, onValueChange = {}, label = { Text("Código Nacional") }, readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth())

                    Divider(color = Color.LightGray)

                    Text("Pauta y horarios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HeaderTeal)
                    OutlinedTextField(value = pauta, onValueChange = { pauta = it }, label = { Text("Dosis") }, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = frecuencia, onValueChange = { frecuencia = it }, label = { Text("Cada (h)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = horaInicio, onValueChange = {}, label = { Text("1ª Toma") }, readOnly = true, enabled = false, modifier = Modifier.weight(1f).clickable { timePickerDialog.show() })
                    }
                    OutlinedTextField(value = stockAlerta, onValueChange = { stockAlerta = it }, label = { Text("Stock alerta") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val frecuenciaInt = frecuencia.toIntOrNull()
                    val stockInt = stockAlerta.toIntOrNull()

                    if (nameState.isNotBlank() && frecuenciaInt != null && stockInt != null && realSeniorId != 0L) {
                        botonPulsado = true
                        addMedicationViewModel.addMedication(
                            seniorId = realSeniorId,
                            nombre = nameState,
                            codigoNacional = codigoNacional,
                            laboratorio = labState,
                            descripcion = descriptionState,
                            pauta = pauta,
                            frecuenciaHoras = frecuenciaInt,
                            stockAlerta = stockInt,
                            horaInicio = horaInicio
                        )
                    } else {
                        Toast.makeText(context, " Rellena todos los datos correctamente", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                enabled = (addState !is AddMedicationState.Loading) && !botonPulsado
            ) {
                if (addState is AddMedicationState.Loading || botonPulsado) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDANDO...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("GUARDAR MEDICAMENTO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}