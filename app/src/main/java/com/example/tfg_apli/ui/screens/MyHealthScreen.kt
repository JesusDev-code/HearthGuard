package com.example.tfg_apli.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.network.dto.UsuarioPatologia
import com.example.tfg_apli.ui.viewmodels.HealthScreenState
import com.example.tfg_apli.ui.viewmodels.MyHealthEvent // Importar evento
import com.example.tfg_apli.ui.viewmodels.MyHealthViewModel
import com.example.tfg_apli.util.AlarmScheduler // Importar scheduler
import com.example.tfg_apli.util.SessionManager
import com.example.tfg_apli.util.LocalIntakeManager
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

// --- COLORES ESPECÍFICOS
private val SeniorBlue = Color(0xFF1565C0)
private val SeniorTeal = Color(0xFF00695C)
private val ChronicBlue = Color(0xFF1976D2)
private val ChronicCyan = Color(0xFF00BCD4)
private val AppleGray = Color(0xFFF2F2F7)
private val MentalMint = Color(0xFFE0F2F1)
private val MentalDark = Color(0xFF4A148C)

private val StatusPending = Color(0xFFEEEEEE)
private val StatusTaken = Color(0xFF4CAF50)

@Composable
fun MyHealthScreen(
    onNavigateToAddMedication: () -> Unit,
    onNavigateToScanner: () -> Unit,
    navController: NavController
) {
    val myHealthViewModel: MyHealthViewModel = viewModel()
    val healthState by myHealthViewModel.healthScreenState.collectAsState()
    val isSelfManagement by SessionManager.isSelfManagementEnabled.collectAsState()


    val context = LocalContext.current
    LaunchedEffect(Unit) {
        myHealthViewModel.events.collect { event ->
            when (event) {
                is MyHealthEvent.MedicationAdded -> {
                    AlarmScheduler.scheduleAlarmsForMedication(context, event.med)

                }
                is MyHealthEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val user by SessionManager.currentUserFlow.collectAsState()
    val mode = user?.modoInterfaz ?: user?.rol ?: "SENIOR"

    // --- LÓGICA DE COLORES TRI-MODAL ---
    val (bg, gradient, contentColor) = when (mode) {
        "APOYO", "MENTAL" -> Triple(
            MaterialTheme.colorScheme.background,
            listOf(Color.White.copy(alpha = 0.9f), MentalMint),
            MentalDark
        )
        "CRONICO", "CONTROL" -> Triple(
            AppleGray,
            listOf(ChronicBlue, ChronicCyan),
            Color.White
        )
        else -> Triple( // SENIOR
            MaterialTheme.colorScheme.background,
            listOf(SeniorBlue, SeniorTeal),
            Color.White
        )
    }

    var showAddPathologyDialog by remember { mutableStateOf(false) }
    var pathologyTypeIsChronic by remember { mutableStateOf(false) }
    var formNombreEnfermedad by remember { mutableStateOf("") }
    var formNotaDoctor by remember { mutableStateOf("") }
    var formLlevaMedicacion by remember { mutableStateOf(false) }
    var formNombreMedicamento by remember { mutableStateOf("") }
    var formCodigoNacional by remember { mutableStateOf("") }
    var formFrecuenciaHoras by remember { mutableStateOf("") }
    var formHoraInicio by remember { mutableStateOf("09:00") }

    val savedState = navController.currentBackStackEntry?.savedStateHandle
    val scannedNameState = savedState?.getStateFlow<String?>("scanned_name", null)?.collectAsState()
    val scannedCodeState = savedState?.getStateFlow<String?>("scanned_code", null)?.collectAsState()

    LaunchedEffect(scannedNameState?.value, scannedCodeState?.value) {
        val name = scannedNameState?.value
        val code = scannedCodeState?.value
        if (name != null && code != null) {
            formNombreMedicamento = name
            formCodigoNacional = code


            val savedChronic = savedState?.get<Boolean>("is_chronic")
            if (savedChronic != null) {
                pathologyTypeIsChronic = savedChronic
            }

            showAddPathologyDialog = true
            savedState?.remove<String>("scanned_name")
            savedState?.remove<String>("scanned_code")
            savedState?.remove<Boolean>("is_chronic")  // ✅ Con tipo explícito
        }
    }

    var refreshTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { refreshTrigger++ }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {

        // CABECERA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(gradient)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = contentColor, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Mi salud", color = contentColor, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)  // ✅ CAMBIADO
                    Text("Tu plan de hoy", color = contentColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge)  // ✅ CAMBIADO
                }
            }
        }

        when (val state = healthState) {
            is HealthScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            is HealthScreenState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Text(
                            "Tomas de hoy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.medications.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Sin medicación activa",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        else state.medications.forEach { med -> DailyIntakeCard(med, myHealthViewModel, onNavigateToScanner, refreshTrigger) }
                    }

                    if (isSelfManagement) {
                        item { Text("¿Cómo estás hoy?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SeniorBigActionCard("Estoy Mal\n(Temporal)", Icons.Default.Healing, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f)) {
                                    pathologyTypeIsChronic = false
                                    if(!showAddPathologyDialog) { formNombreEnfermedad=""; formNombreMedicamento="" }
                                    showAddPathologyDialog = true
                                }
                                SeniorBigActionCard("Nueva\nCrónica", Icons.Default.MedicalServices, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f)) {
                                    pathologyTypeIsChronic = true
                                    if(!showAddPathologyDialog) { formNombreEnfermedad=""; formNombreMedicamento="" }
                                    showAddPathologyDialog = true
                                }
                            }
                        }
                    }
                    item { PathologiesCard(myHealthViewModel, state.pathologies, state.medications, isSelfManagement) }


                    item {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                                    append("Resumen de ")
                                }
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                    append("medicación")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item { ActiveMedicationCard(state.medications) }


                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
            is HealthScreenState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
        }
    }

    if (showAddPathologyDialog) {
        AddPathologyDialogStateful(
            isChronic = pathologyTypeIsChronic,
            nombre = formNombreEnfermedad, onNombreChange = { formNombreEnfermedad = it },
            nota = formNotaDoctor, onNotaChange = { formNotaDoctor = it },
            llevaMed = formLlevaMedicacion, onLlevaMedChange = { formLlevaMedicacion = it },
            nomMed = formNombreMedicamento, onNomMedChange = { formNombreMedicamento = it },
            freq = formFrecuenciaHoras, onFreqChange = { formFrecuenciaHoras = it },
            horaInicio = formHoraInicio, onHoraInicioChange = { formHoraInicio = it },
            onDismiss = { showAddPathologyDialog = false },
            onScanClick = {

                navController.currentBackStackEntry?.savedStateHandle?.set("is_chronic", pathologyTypeIsChronic)
                showAddPathologyDialog = false
                navController.navigate("scanner_picker")
            },
            onConfirm = {
                myHealthViewModel.guardarPatologiaCompleta(
                    nombre = formNombreEnfermedad,
                    esCronica = pathologyTypeIsChronic,
                    nota = formNotaDoctor,
                    medNombre = if(formLlevaMedicacion) formNombreMedicamento else null,
                    medCodigo = if(formLlevaMedicacion) formCodigoNacional else null,
                    medFrecuencia = formFrecuenciaHoras.toIntOrNull(),
                    medHoraInicio = formHoraInicio
                )
                showAddPathologyDialog = false
            }
        )
    }
}

// --- SUB-COMPONENTES ---

@Composable
fun AddPathologyDialogStateful(
    isChronic: Boolean,
    nombre: String, onNombreChange: (String) -> Unit,
    nota: String, onNotaChange: (String) -> Unit,
    llevaMed: Boolean, onLlevaMedChange: (Boolean) -> Unit,
    nomMed: String, onNomMedChange: (String) -> Unit,
    freq: String, onFreqChange: (String) -> Unit,
    horaInicio: String, onHoraInicioChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onScanClick: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(context, { _, h, m -> onHoraInicioChange(String.format(Locale.getDefault(), "%02d:%02d", h, m)) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isChronic) "Nueva enfermedad crónica" else "Nueva enfermedad temporal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = onNombreChange, label = { Text("Nombre enfermedad") })
                OutlinedTextField(value = nota, onValueChange = onNotaChange, label = { Text("Nota doctor") })
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = llevaMed, onCheckedChange = onLlevaMedChange); Text("Lleva medicación") }
                if (llevaMed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = nomMed, onValueChange = onNomMedChange, label = { Text("Fármaco") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = onScanClick) { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = freq, onValueChange = onFreqChange, label = { Text("Cada (h)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = horaInicio, onValueChange = {}, label = { Text("Inicio") }, readOnly = true, enabled = false, modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledContainerColor = Color.Transparent))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable fun DailyIntakeCard(med: MedicamentoResponseDTO, viewModel: MyHealthViewModel, onNavigateToScanner: () -> Unit, refreshTrigger: Int) {
    val context = LocalContext.current
    val frequency = med.frecuenciaHoras ?: 24
    val timesPerDay = (24 / frequency).coerceAtLeast(1)
    val startHourString = med.horaInicio ?: "09:00"
    val startHour = startHourString.split(":").firstOrNull()?.toIntOrNull() ?: 9
    val startMinute = startHourString.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = med.nombre,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Cada $frequency horas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                Button(
                    onClick = { onNavigateToScanner() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificar", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until timesPerDay) {
                    val hour = (startHour + (i * frequency)) % 24
                    val timeString = String.format("%02d:%02d", hour, startMinute)
                    val isTaken = remember(refreshTrigger) {
                        LocalIntakeManager.isTaken(context, med.id, timeString)
                    }
                    val status = if (isTaken) 1 else 0
                    IntakeButtonLocked(timeString, status)
                }
            }
        }
    }
}

@Composable
fun IntakeButtonLocked(time: String, status: Int) {
    val (bgColor, contentColor) = when (status) {
        1 -> Pair(Color(0xFF4CAF50), Color.White)
        else -> Pair(Color(0xFFF5F5F5), Color.Black)
    }

    val border = if (status == 0) BorderStroke(1.dp, Color.LightGray) else null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor)
                .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (status == 1) {
                Icon(Icons.Default.Check, null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (status == 0) Color.Gray else bgColor
        )
    }
}

@Composable fun SeniorBigActionCard(title: String, icon: ImageVector, color: Color, iconColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(20.dp), modifier = modifier.height(110.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp)) { Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(36.dp)); Spacer(modifier = Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = iconColor, fontSize = 16.sp, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } } }

@Composable
fun PathologiesCard(
    viewModel: MyHealthViewModel,
    pathologies: List<UsuarioPatologia>,
    medications: List<MedicamentoResponseDTO>,
    isEditable: Boolean
) {
    val activePath = pathologies.filter { it.fechaCura == null }
    val (chronic, temporary) = activePath.partition { it.patologia.esCronica }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ========== ENFERMEDADES CRÓNICAS ==========
        if (chronic.isNotEmpty()) {
            Text(
                "Enfermedades crónicas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // MOSTRAR LAS ENFERMEDADES CRÓNICAS
            chronic.forEach { pat ->
                PathologyItem(
                    pathology = pat,
                    meds = medications.filter { it.patologiaId == pat.patologia.id },
                    isEditable = isEditable,
                    viewModel = viewModel,
                    onCureClick = null // Las crónicas NO se pueden curar
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ========== MALESTAR TEMPORAL ==========
        Text(
            "Malestar temporal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (temporary.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "¡Estás sano! todo bien.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            temporary.forEach { pat ->
                PathologyItem(
                    pathology = pat,
                    meds = medications.filter { it.patologiaId == pat.patologia.id },
                    isEditable = isEditable,
                    viewModel = viewModel,
                    onCureClick = { viewModel.markAsCured(pat.id) } // Las temporales SÍ se pueden curar
                )
            }
        }
    }
}

@Composable
fun PathologyItem(
    pathology: UsuarioPatologia,
    meds: List<MedicamentoResponseDTO>,
    isEditable: Boolean,
    viewModel: MyHealthViewModel,
    onCureClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddMedDialog by remember { mutableStateOf(false) }
    val mainColor = MaterialTheme.colorScheme.primary  // ✅ CAMBIADO: mismo color para crónico y temporal

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (pathology.patologia.esCronica) Icons.Default.Shield else Icons.Default.Healing,
                        null,
                        tint = MaterialTheme.colorScheme.primary  // ✅ Azul uniforme
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        pathology.patologia.nombre,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onCureClick != null && isEditable) {
                    Button(
                        onClick = onCureClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Curar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (!pathology.notasMedico.isNullOrBlank()) {
                    Text("Nota:", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(pathology.notasMedico, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("Medicamentos:", fontWeight = FontWeight.Bold, color = Color.Black)

                if (meds.isEmpty()) {
                    Text("Ninguno asignado.", color = Color.Gray)
                } else {
                    meds.forEach { med ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "• ${med.nombre}",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!med.descripcion.isNullOrBlank()) {
                                    Text(
                                        med.descripcion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (isEditable) {
                                IconButton(onClick = { viewModel.deleteMedication(med.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFD32F2F))  // ✅ ROJO solo para borrar
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun AddMedDialogInternal(patName: String, onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) { var name by remember { mutableStateOf("") }; var freq by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Añadir a $patName") }, text = { Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }); OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Horas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) } }, confirmButton = { Button(onClick = { freq.toIntOrNull()?.let { onConfirm(name, it) } }) { Text("Añadir") } }) }
@Composable
fun ActiveMedicationCard(medications: List<MedicamentoResponseDTO>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (medications.isEmpty()) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sin medicación programada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                medications.forEach {
                    Text(
                        "• ${it.nombre}",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}