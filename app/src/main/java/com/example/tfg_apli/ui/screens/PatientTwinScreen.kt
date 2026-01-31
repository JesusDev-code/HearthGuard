package com.example.tfg_apli.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tfg_apli.network.dto.*
import com.example.tfg_apli.ui.viewmodels.PatientDetailState
import com.example.tfg_apli.ui.viewmodels.PatientDetailViewModel
import java.util.Calendar

// Imports para gráficos (versión 2.1.4)
import androidx.compose.foundation.Canvas



// --- PALETA DE COLORES (AHORA IDÉNTICA A CAREGIVER) ---
private val HeaderGradientStart = Color(0xFF1565C0) // Azul oscuro
private val HeaderGradientEnd = Color(0xFF0288D1)   // Azul claro (Igual que Caregiver)
private val BackgroundGray = Color(0xFFF5F5F5)      // Gris fondo estándar
private val CardWhite = Color.White

// Colores de Estado
private val AlertRed = Color(0xFFD32F2F)
private val ChronicPurple = Color(0xFF7B1FA2)
private val MedBlue = Color(0xFF1976D2)
private val ApptOrange = Color(0xFFEF6C00)

@Composable
fun PatientTwinScreen(
    seniorId: Long,
    name: String,
    navController: NavController,
    viewModel: PatientDetailViewModel = viewModel()
) {
    // Carga de datos
    LaunchedEffect(seniorId) { viewModel.loadData(seniorId) }

    // Gestión del Escáner (Retorno de datos)
    val currentBackStack = navController.currentBackStackEntry
    val scannedNameState = currentBackStack?.savedStateHandle?.getStateFlow<String?>("scanned_name", null)?.collectAsState()
    val scannedName = scannedNameState?.value
    val scannedCodeState = currentBackStack?.savedStateHandle?.getStateFlow<String?>("scanned_code", null)?.collectAsState()
    val scannedCode = scannedCodeState?.value

    // Estados de UI
    var activeDialog by rememberSaveable { mutableIntStateOf(0) }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showBloodPressureHistory by remember { mutableStateOf(false) }
    var showAlertsHistory by remember { mutableStateOf(false) }


    // Limpieza de argumentos del escáner
    LaunchedEffect(scannedName, scannedCode) {
        if (!scannedName.isNullOrEmpty()) {
            currentBackStack?.savedStateHandle?.remove<String>("scanned_name")
            currentBackStack?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    val state by viewModel.state.collectAsState()
    val tabs = listOf("Resumen", "Enfermedades", "Medicación", "Agenda","Tensión")

    Scaffold(
        containerColor = BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. HEADER (CON DATOS DEL PACIENTE)
            when (val currentState = state) {
                is PatientDetailState.Success -> {
                    PatientDetailHeader(
                        name = name,
                        patientInfo = currentState.patientInfo, // ✅ NUEVO
                        onBack = { navController.popBackStack() }
                    )
                }
                else -> {
                    PatientDetailHeader(
                        name = name,
                        patientInfo = null, // ✅ Mientras carga
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // 2. PESTAÑAS
            Surface(
                color = CardWhite,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.zIndex(1f)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MedBlue,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[tabIndex])
                                .width(30.dp),
                            color = MedBlue,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            },
                            selectedContentColor = MedBlue,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
            }

            // 3. CONTENIDO
            Box(modifier = Modifier.fillMaxSize()) {
                when (val currentState = state) {
                    is PatientDetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MedBlue) }
                    is PatientDetailState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${currentState.message}", color = AlertRed) }
                    is PatientDetailState.Success -> {
                        when (tabIndex) {
                            0 -> DashboardSummaryView(
                                alertas = currentState.alertas,
                                patologias = currentState.historialSalud,
                                medicamentos = currentState.medicamentos,
                                citas = currentState.citas,
                                tomasHoy = currentState.tomasHoy,
                                onAlertsHistoryClick = { showAlertsHistory = true },
                                analiticas = currentState.analiticas,
                                onBloodPressureHistoryClick = { showBloodPressureHistory = true },
                                onHistoryClick = { showHistoryDialog = true }
                            )
                            1 -> DiseasesListView(
                                historial = currentState.historialSalud,
                                medicamentos = currentState.medicamentos,
                                onAddClick = { activeDialog = 1 },
                                viewModel = viewModel,
                                seniorId = seniorId
                            )
                            2 -> MedicationListView(
                                medicamentos = currentState.medicamentos,
                                onAddClick = { activeDialog = 2 },
                                viewModel = viewModel,
                                seniorId = seniorId
                            )
                            3 -> AppointmentsListView(
                                citas = currentState.citas,
                                onAddClick = { activeDialog = 3 }
                            )
                            4 -> BloodPressureListView(
                                    analiticas = currentState.analiticas,
                                    onAddClick = { activeDialog = 4 },
                                    seniorId = seniorId
                                )
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS---
    if (activeDialog == 1) {
        AddCompleteDiseaseDialog(
            initialMedName = scannedName ?: "",
            initialCode = scannedCode ?: "000000",
            onDismiss = { activeDialog = 0 },
            onScanRequest = {
                navController.currentBackStackEntry?.savedStateHandle?.set("targetSeniorId", seniorId)
                navController.navigate("scanner_picker")
            },
            onConfirm = { patName, isChronic, note, medName, freq, horaInicio, codigoNacional ->
                viewModel.guardarPatologiaCompleta(seniorId, patName, isChronic, note, medName, freq, horaInicio, codigoNacional)
                activeDialog = 0
            }
        )
    }

    if (activeDialog == 2) {
        AddMedicationDialog(
            initialName = scannedName ?: "",
            initialCode = scannedCode ?: "000000",
            onDismiss = { activeDialog = 0 },
            onScanRequest = {
                navController.currentBackStackEntry?.savedStateHandle?.set("targetSeniorId", seniorId)
                navController.navigate("scanner_picker")
            },
            onConfirm = { nombre, dosis, freq, horaInicio, codigoNacional ->
                viewModel.registrarMedicamento(seniorId, nombre, dosis, freq, horaInicio, codigoNacional)
                activeDialog = 0
            }
        )
    }

    if (activeDialog == 3) {
        AddAppointmentDialog(
            onDismiss = { activeDialog = 0 },
            onConfirm = { t, l, f, h ->
                viewModel.registrarCita(seniorId, t, l, f, h)
                activeDialog = 0
            }
        )
    }
    if (showHistoryDialog) {
        when (val currentState = state) {
            is PatientDetailState.Success -> {
                val curadas = currentState.historialSalud.filter { it.fechaCura != null }
                PatientMedicalHistoryDialog(
                    pathologies = curadas,
                    onDismiss = { showHistoryDialog = false }
                )
            }
            else -> { /* No hacer nada */ }
        }
    }
    //  HISTORIAL DE TENSIÓN
    if (showBloodPressureHistory) {
        when (val currentState = state) {
            is PatientDetailState.Success -> {
                BloodPressureHistoryDialog(
                    analiticas = currentState.analiticas,
                    onDismiss = { showBloodPressureHistory = false }
                )
            }
            else -> { /* No hacer nada */ }
        }
    }
    // HISTORIAL DE ALARMAS
    if (showAlertsHistory) {
        when (val currentState = state) {
            is PatientDetailState.Success -> {
                AlertsHistoryDialog(
                    alertas = currentState.alertas,
                    onDismiss = { showAlertsHistory = false }
                )
            }
            else -> { /* No hacer nada */ }
        }
    }

    if (activeDialog == 4) {
        AddBloodPressureDialog(
            onDismiss = { activeDialog = 0 },
            onConfirm = { sistolica, diastolica ->
                viewModel.registrarTensionArterial(seniorId, sistolica, diastolica)
                activeDialog = 0
            }
        )
    }
}

// ==========================================
// COMPONENTE DE CABECERA
// ==========================================

@Composable
fun PatientDetailHeader(
    name: String,
    patientInfo: UsuarioResponseDTO?,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(HeaderGradientStart, HeaderGradientEnd)))
            .padding(24.dp)
    ) {
        // Botón atrás
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).offset(x = (-12).dp, y = (-8).dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
        }

        // Row con 2 columnas
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // COLUMNA IZQUIERDA: Nombre
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Expediente Digital",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // ✅ COLUMNA DERECHA: Edad, Peso, Alergias
            if (patientInfo != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Edad
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cake,
                            null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${patientInfo.edad ?: "-"} años",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Peso
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${patientInfo.peso ?: "-"} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alergias
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = if (!patientInfo.alergias.isNullOrBlank())
                                Color(0xFFFFCA28)
                            else
                                Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = patientInfo.alergias?.take(10) ?: "Sin alerg.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Icono decorativo
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .offset(x = 15.dp, y = (-10).dp)
        )
    }
}

// ==========================================
// RESTO DE COMPONENTES
// ==========================================

@Composable
fun DashboardSummaryView(
    alertas: List<LogEmergencia>,
    patologias: List<UsuarioPatologia>,
    medicamentos: List<MedicamentoResponseDTO>,
    citas: List<CitaMedicaDTO>,
    tomasHoy: List<TomaResponseDTO>,
    analiticas: List<AnaliticaResponseDTO>,
    onHistoryClick: () -> Unit,
    onBloodPressureHistoryClick: () -> Unit,
    onAlertsHistoryClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SOS
        item { SectionHeader("📢 Alarmas reciente", AlertRed) }
        if (alertas.isEmpty()) item { EmptySectionText("Sin incidencias.") }
        else items(alertas.take(3)) { DashboardCard(it) }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlertsHistoryClick() }  // ⚠️ Añadiremos este parámetro
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            null,
                            tint = AlertRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Historial de alarmas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${alertas.size} alertas totales", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }
        }

        //TENSIÓN ARTERIAL
        item { SectionHeader("🩺 Última tensión", Color.Black) }
        item {
            val sistolica = analiticas.filter { it.tipoMetrica.contains("Sistolica", true) }.maxByOrNull { it.fechaRegistro }
            val diastolica = analiticas.filter { it.tipoMetrica.contains("Diastolica", true) }.maxByOrNull { it.fechaRegistro }

            BloodPressureCard(
                sistolica = sistolica,
                diastolica = diastolica,
                onHistoryClick = onBloodPressureHistoryClick  // ✅ CORRECTO: usa el parámetro de la función
            )
        }


        // ENFERMEDADES
        item { SectionHeader("🏥 Diagnósticos", Color.Black) }
        if (patologias.isEmpty()) item { EmptySectionText("Sin historial médico.") }
        else items(patologias.take(3)) {
            InfoCard(
                title = it.patologia.nombre,
                subtitle = if (it.patologia.esCronica) "Crónica" else "temporal",
                icon = Icons.Default.Healing,
                color = Color.Black
            )
        }
        //  HISTORIAL DE ENFERMEDADES CURADAS
        item {
            val curadas = patologias.filter { it.fechaCura != null }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {  onHistoryClick() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Historial médico", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (curadas.isEmpty()) "Sin registros" else "${curadas.size} curadas",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }
        }


        item { SectionHeader("💊 Control de tomas hoy", Color.Black) }
        if (medicamentos.isEmpty()) {
            item { EmptySectionText("Sin medicación programada.") }
        } else {
            items(medicamentos) { med ->
                MedicationIntakeCard(med, tomasHoy)
            }
        }

        // MEDICACIÓN (Resumen)
        item { SectionHeader("💊 Tratamiento activo", Color.Black) }
        if (medicamentos.isEmpty()) item { EmptySectionText("Sin medicación.") }
        else items(medicamentos.take(3)) {
            InfoCard(
                title = it.nombre,
                subtitle = "${it.dosis} - Cada ${it.frecuenciaHoras}h",
                icon = Icons.Default.Medication,
                color = Color.Black
            )
        }

        // AGENDA
        item { SectionHeader("Próximas citas", Color.Black) }
        if (citas.isEmpty()) item { EmptySectionText("Agenda libre.") }
        else items(citas.take(3)) {
            InfoCard(
                title = it.titulo,
                subtitle = formatDateTime(it.fechaHora),
                icon = Icons.Default.Event,
                color = Color.Black
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun DiseasesListView(
    historial: List<UsuarioPatologia>,
    medicamentos: List<MedicamentoResponseDTO>,
    onAddClick: () -> Unit,
    viewModel: PatientDetailViewModel,
    seniorId: Long
) {
    val activePathologies = historial.filter { it.fechaCura == null }
    val chronic = activePathologies.filter { it.patologia.esCronica }
    val temporary = activePathologies.filter { !it.patologia.esCronica }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ActionButton("Registrar enfermedad", Icons.Default.AddCircle, Color.Black, onAddClick)
        }

        if (activePathologies.isEmpty()) {
            item { EmptyStateMessage("Sin enfermedades activas", Icons.Default.Healing) }
        } else {
            // ========== ENFERMEDADES CRÓNICAS ==========
            if (chronic.isNotEmpty()) {
                item {
                    Text(
                        "Enfermedades Crónicas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(chronic) { pathology ->
                    ExpandablePathologyCard(
                        pathology = pathology,
                        medications = medicamentos.filter { it.patologiaId == pathology.patologia.id }, // ✅ FILTRAR
                        viewModel = viewModel,
                        seniorId = seniorId,
                        showCureButton = false
                    )
                }
            }

            // ========== ENFERMEDADES TEMPORALES ==========
            if (temporary.isNotEmpty()) {
                item {
                    Text(
                        "Malestar temporal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(
                            top = if (chronic.isNotEmpty()) 16.dp else 8.dp,
                            bottom = 8.dp
                        )
                    )
                }

                items(temporary) { pathology ->
                    ExpandablePathologyCard(
                        pathology = pathology,
                        medications = medicamentos.filter { it.patologiaId == pathology.patologia.id }, // ✅ FILTRAR
                        viewModel = viewModel,
                        seniorId = seniorId,
                        showCureButton = true
                    )
                }
            }
        }
    }
}

// ==========================================
// TARJETA EXPANDIBLE PARA PATOLOGÍAS
// ==========================================
@Composable
fun ExpandablePathologyCard(
    pathology: UsuarioPatologia,
    medications: List<MedicamentoResponseDTO>,
    viewModel: PatientDetailViewModel,
    seniorId: Long,
    showCureButton: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = if (pathology.patologia.esCronica) ChronicPurple else Color.Black
    val statusText = if (pathology.patologia.esCronica) "CRÓNICA" else "TEMPORAL"

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral de color
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(borderColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                // ===== CABECERA=====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nombre de la enfermedad
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (pathology.patologia.esCronica) Icons.Default.Shield else Icons.Default.Healing,
                            null,
                            tint = borderColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pathology.patologia.nombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Badge de tipo
                    Surface(
                        color = borderColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            statusText,
                            color = borderColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // ===== CONTENIDO EXPANDIBLE =====
                if (expanded) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Notas del médico
                    if (!pathology.notasMedico.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Note,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(y = 2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    "Notas del médico:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                Text(
                                    pathology.notasMedico,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // MEDICAMENTOS ACTIVOS
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Medication,
                            null,
                            tint = if (medications.isNotEmpty()) MedBlue else Color.LightGray,
                            modifier = Modifier.size(16.dp).offset(y = 2.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                "Tratamiento:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )

                            if (medications.isEmpty()) {
                                Text(
                                    "Sin medicación activa",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                medications.forEach { med ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                med.nombre,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MedBlue
                                            )
                                            Text(
                                                "${med.dosis} • Cada ${med.frecuenciaHoras}h",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // Botón de curar (solo para temporales)
                    if (showCureButton) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.markAsCured(seniorId, pathology.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Marcar como curada", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Indicador de "toca para ver más"
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Toca para ver detalles",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}



@Composable
fun MedicationListView(medicamentos: List<MedicamentoResponseDTO>, onAddClick: () -> Unit,
                       viewModel: PatientDetailViewModel,
                       seniorId: Long                ) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ActionButton("Añadir medicamento", Icons.Default.Medication, Color.Black, onAddClick) }
        if (medicamentos.isEmpty()) item { EmptyStateMessage("Sin medicación", Icons.Default.Medication) }
        else items(medicamentos) { med ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(Color.Black)
                    )

                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = med.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "${med.frecuenciaHoras}h",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Dosis: ${med.dosis} • Inicio: ${med.horaInicio}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        if (!med.codigoNacional.isNullOrBlank() && med.codigoNacional != "000000") {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(med.codigoNacional ?: "", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // BOTÓN ELIMINAR
                        Button(
                            onClick = { viewModel.eliminarMedicamento(seniorId, med.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF5350)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }

}

@Composable
fun AppointmentsListView(citas: List<CitaMedicaDTO>, onAddClick: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ActionButton("Agendar Cita", Icons.Default.EditCalendar, Color.Black, onAddClick) }
        if (citas.isEmpty()) item { EmptyStateMessage("Sin citas pendientes", Icons.Default.EventBusy) }
        else items(citas) { cita ->
            DetailedCard(
                title = cita.titulo,
                status = formatTime(cita.fechaHora),
                detail = formatDate(cita.fechaHora) + if (cita.lugar != null) " • ${cita.lugar}" else "",
                icon = Icons.Default.Event,
                color = Color.Black
            )
        }
    }
}

@Composable
fun InfoCard(title: String, subtitle: String, icon: ImageVector, color: Color) {
    val finalColor = if (color == AlertRed) AlertRed else Color.Black

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(finalColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = finalColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF263238))
                Text(subtitle, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DetailedCard(title: String, status: String, detail: String?, icon: ImageVector, color: Color, hasQr: Boolean = false) {
    val finalColor = if (color == AlertRed) AlertRed else Color.Black

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(finalColor))
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Surface(
                        color = finalColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(status, color = finalColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                if (detail != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(detail, fontSize = 14.sp, color = Color.Gray)
                }
            }
            if (hasQr) {
                Icon(Icons.Default.QrCode, null, tint = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 16.dp))
            }
        }
    }
}

@Composable
fun DashboardCard(alerta: LogEmergencia) {
    val time = formatTime(alerta.fecha ?: "")
    val isSOS = alerta.tipo.contains("SOS")
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSOS) Color(0xFFFFEBEE) else CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSOS) androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = AlertRed)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isSOS) "🚨 EMERGENCIA SOS" else alerta.tipo, fontWeight = FontWeight.Bold, color = Color.Black)
                if (!alerta.gps.isNullOrEmpty()) Text("Ubicación registrada", fontSize = 12.sp, color = Color.Gray)
            }
            Text(time, fontWeight = FontWeight.Bold, color = AlertRed, fontSize = 14.sp)
        }
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(text: String, color: Color = Color.Black) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        color = if (color == AlertRed) AlertRed else Color.Black,
        fontSize = 18.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
@Composable
fun EmptySectionText(text: String) { Text(text, color = Color.Gray, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
@Composable
fun EmptyStateMessage(text: String, icon: ImageVector) { Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.LightGray); Spacer(Modifier.height(16.dp)); Text(text, color = Color.Gray, textAlign = TextAlign.Center) } }

private fun formatDateTime(isoString: String): String { return try { val parts = isoString.split("T"); "${parts[0]} - ${parts[1].take(5)}" } catch (e: Exception) { isoString } }
private fun formatTime(isoString: String): String { return try { isoString.split("T").getOrNull(1)?.take(5) ?: "--:--" } catch (e: Exception) { "" } }
private fun formatDate(isoString: String): String { return try { isoString.split("T").getOrNull(0) ?: "" } catch (e: Exception) { "" } }

// ==========================================
// COMPONENTE TOMAS DEL DÍA
// ==========================================

@Composable
fun MedicationIntakeCard(
    med: MedicamentoResponseDTO,
    tomasHoy: List<TomaResponseDTO>
) {
    // Filtrar tomas por nombre del medicamento (ya que no tenemos ID)
    val tomasMed = tomasHoy.filter { it.medicamentoNombre == med.nombre }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nombre del medicamento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = med.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF263238)
                    )

                    Text(
                        text = "Frecuencia: cada ${med.frecuenciaHoras}h",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Badge de cumplimiento
                val tomadas = tomasMed.count { it.estado == "TOMADA" }
                val total = tomasMed.size
                val adherenciaColor = when {
                    total == 0 -> Color.Gray
                    tomadas == total -> Color(0xFF4CAF50)
                    tomadas > 0 -> Color(0xFFFF9800)
                    else -> Color(0xFFEF5350)
                }

                Surface(
                    color = adherenciaColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$tomadas/$total",
                        color = adherenciaColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estados de las tomas
            if (tomasMed.isEmpty()) {
                Text(
                    "Sin tomas registradas hoy",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tomasMed.forEach { toma ->
                        IntakeStatusIndicator(toma)
                    }
                }
            }
        }
    }
}

@Composable
fun IntakeStatusIndicator(toma: TomaResponseDTO) {
    // Extraer hora
    val horaStr = try {
        toma.fechaProgramada.split("T").getOrNull(1)?.take(5) ?: "??:??"
    } catch (e: Exception) {
        "??:??"
    }


    val (bgColor, icon, contentColor) = when (toma.estado.toString()) {
        "TOMADA" -> Triple(Color(0xFF4CAF50), Icons.Default.Check, Color.White)
        "OMITIDA" -> Triple(Color(0xFFEF5350), Icons.Default.Close, Color.White)
        else -> Triple(Color(0xFFEEEEEE), null, Color.Black) // PENDIENTE
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgColor)
                .then(
                    if (toma.estado.toString() == "PENDIENTE")
                        Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = horaStr,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (toma.estado.toString() == "PENDIENTE") Color.Gray else bgColor
        )
    }
}
// --- DIÁLOGOS DE ENTRADA ---
@Composable
fun AddCompleteDiseaseDialog(initialMedName: String, initialCode: String, onDismiss: () -> Unit, onScanRequest: () -> Unit, onConfirm: (String, Boolean, String, String, Int?, String, String) -> Unit) {
    var nombrePatologia by remember { mutableStateOf("") }
    var esCronica by remember { mutableStateOf(false) }
    var notas by remember { mutableStateOf("") }
    var nombreMedicamento by remember(initialMedName) { mutableStateOf(initialMedName) }
    var codigoNacional by remember(initialCode) { mutableStateOf(initialCode) }
    var frecuencia by remember { mutableStateOf("8") }
    var horaInicio by remember { mutableStateOf("09:00") }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Enfermedad", color = MedBlue, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Datos Patología", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = nombrePatologia, onValueChange = { nombrePatologia = it }, label = { Text("Nombre Patología") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = esCronica, onCheckedChange = { esCronica = it }); Text("Es Crónica") }
                OutlinedTextField(value = notas, onValueChange = { notas = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                Text("Tratamiento Asociado", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = nombreMedicamento, onValueChange = { nombreMedicamento = it }, label = { Text("Medicamento (Opcional)") }, modifier = Modifier.weight(1f), placeholder = { Text("Escanear o escribir") })
                    IconButton(onClick = onScanRequest) { Icon(Icons.Default.QrCodeScanner, null, tint = MedBlue) }
                }
                if (nombreMedicamento.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = frecuencia, onValueChange = { if(it.all{c->c.isDigit()}) frecuencia = it }, label = { Text("Cada (h)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = horaInicio, onValueChange = {}, label = { Text("Inicio") }, readOnly = true, modifier = Modifier.weight(1f), trailingIcon = { IconButton(onClick = { TimePickerDialog(context, { _, h, m -> horaInicio = String.format("%02d:%02d", h, m) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show() }) { Icon(Icons.Default.AccessTime, null) } })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (nombrePatologia.isNotBlank()) onConfirm(nombrePatologia, esCronica, notas, nombreMedicamento, frecuencia.toIntOrNull() ?: 8, horaInicio, codigoNacional) else Toast.makeText(context, "Escribe el nombre", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = MedBlue)) { Text("Guardar Todo") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddMedicationDialog(initialName: String, initialCode: String, onDismiss: () -> Unit, onScanRequest: () -> Unit, onConfirm: (String, String, Int, String, String) -> Unit) {
    var nombre by remember(initialName) { mutableStateOf(initialName) }
    var codigoNacional by remember(initialCode) { mutableStateOf(initialCode) }
    var dosis by remember { mutableStateOf("1") }
    var freq by remember { mutableStateOf("8") }
    var hora by remember { mutableStateOf("09:00") }
    val ctx = LocalContext.current
    val cal = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Medicamento", color = MedBlue, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.weight(1f)); IconButton(onClick = onScanRequest) { Icon(Icons.Default.QrCodeScanner, null, tint = MedBlue) } }
                OutlinedTextField(value = dosis, onValueChange = { dosis = it }, label = { Text("Dosis") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = freq, onValueChange = { if(it.all{c->c.isDigit()}) freq = it }, label = { Text("Cada (h)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = hora, onValueChange = {}, label = { Text("Inicio") }, readOnly = true, modifier = Modifier.weight(1f), trailingIcon = { IconButton(onClick = { TimePickerDialog(ctx, {_,h,m -> hora = String.format("%02d:%02d",h,m)}, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show() }) { Icon(Icons.Default.AccessTime, null) } })
                }
            }
        },
        confirmButton = { Button(onClick = { if (nombre.isNotBlank()) onConfirm(nombre, dosis, freq.toIntOrNull() ?: 8, hora, codigoNacional) }, colors = ButtonDefaults.buttonColors(containerColor = MedBlue)) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddAppointmentDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var t by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var h by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val cal = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Cita", color = ApptOrange, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("Motivo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = l, onValueChange = { l = it }, label = { Text("Lugar") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = f, onValueChange = {}, label = { Text("Fecha") }, readOnly = true, modifier = Modifier.weight(1f), trailingIcon = { IconButton(onClick = { DatePickerDialog(ctx, {_,y,m,d -> f = String.format("%04d-%02d-%02d",y,m+1,d)}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }) { Icon(Icons.Default.DateRange, null) } })
                    OutlinedTextField(value = h, onValueChange = {}, label = { Text("Hora") }, readOnly = true, modifier = Modifier.weight(1f), trailingIcon = { IconButton(onClick = { TimePickerDialog(ctx, {_,hh,mm -> h = String.format("%02d:%02d",hh,mm)}, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show() }) { Icon(Icons.Default.AccessTime, null) } })
                }
            }
        },
        confirmButton = { Button(onClick = { if(t.isNotEmpty() && f.isNotEmpty()) onConfirm(t,l,f,h) }, colors = ButtonDefaults.buttonColors(containerColor = ApptOrange)) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientMedicalHistoryDialog(
    pathologies: List<UsuarioPatologia>,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = BackgroundGray,
            topBar = {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(Brush.verticalGradient(listOf(HeaderGradientStart, HeaderGradientEnd)))
                ) {
                    // Botón cerrar
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                    }

                    // Texto del título
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Historial Médico",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Enfermedades curadas",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pathologies.isEmpty()) {
                    item {
                        // Estado vacío mejorado
                        Box(
                            Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.HealthAndSafety,
                                    null,
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Sin historial médico",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    "Las enfermedades curadas aparecerán aquí",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(pathologies) { pathology ->

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                                // Barra lateral verde
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(6.dp)
                                        .background(Color(0xFF4CAF50))
                                )

                                Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                                    // Título + Badge de "CURADA"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pathology.patologia.nombre,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF263238),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Surface(
                                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    null,
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "CURADA",
                                                    color = Color(0xFF4CAF50),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Fecha de curación
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Curado el ${pathology.fechaCura?.take(10) ?: "-"}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    // Medicación
                                    if (!pathology.medicacionHistorial.isNullOrBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                Icons.Default.Medication,
                                                null,
                                                tint = MedBlue,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                pathology.medicacionHistorial,
                                                fontSize = 13.sp,
                                                color = MedBlue,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Badge de tipo (crónica o temporal)
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = if (pathology.patologia.esCronica)
                                            ChronicPurple.copy(alpha = 0.1f)
                                        else
                                            Color.Gray.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (pathology.patologia.esCronica) "Crónica" else "Temporal",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pathology.patologia.esCronica) ChronicPurple else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BloodPressureCard(
    sistolica: AnaliticaResponseDTO?,
    diastolica: AnaliticaResponseDTO?,
    onHistoryClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onHistoryClick() }  // ✅ CLICKABLE
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icono
            val (bgColor, statusText, iconColor) = when {
                sistolica == null || diastolica == null -> Triple(
                    Color.Gray.copy(alpha = 0.1f),
                    "Sin registros",
                    Color.Gray
                )
                sistolica.valor >= 135 || diastolica.valor >= 85 -> Triple(
                    AlertRed.copy(alpha = 0.1f),
                    "ALTA",
                    AlertRed
                )
                sistolica.valor < 90 || diastolica.valor < 60 -> Triple(
                    Color(0xFFFF9800).copy(alpha = 0.1f),
                    "BAJA",
                    Color(0xFFFF9800)
                )
                else -> Triple(
                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                    "NORMAL",
                    Color(0xFF4CAF50)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Favorite, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (sistolica != null && diastolica != null) {
                    Text(
                        "${sistolica.valor.toInt()}/${diastolica.valor.toInt()} mmHg",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF263238)
                    )
                    Text(
                        "Última: ${formatDateTime(sistolica.fechaRegistro)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Text("No hay mediciones", fontSize = 16.sp, color = Color.Gray)
                }
            }


            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureHistoryDialog(
    analiticas: List<AnaliticaResponseDTO>,
    onDismiss: () -> Unit
) {
    // Agrupar mediciones cercanas en el tiempo
    val sistolicas = analiticas.filter { it.tipoMetrica.contains("Sistolica", true) }
        .sortedBy { it.fechaRegistro } // Orden ascendente para el gráfico
    val diastolicas = analiticas.filter { it.tipoMetrica.contains("Diastolica", true) }
        .sortedBy { it.fechaRegistro }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = BackgroundGray,
            topBar = {
                // Header con degradado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(HeaderGradientStart, HeaderGradientEnd)
                            )
                        )
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Historial de Tensión",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Evolución temporal",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (mediciones.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Favorite,
                                    null,
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Sin mediciones",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    "Las mediciones de tensión aparecerán aquí",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {

                    item {
                        BloodPressureChart(mediciones = mediciones)
                    }

                    // Separador
                    item {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Mediciones detalladas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Lista de mediciones
                    items(mediciones.reversed()) { (fecha, sistolica, diastolica) ->
                        BloodPressureHistoryItem(fecha, sistolica, diastolica)
                    }
                }
            }
        }
    }
}


@Composable
fun BloodPressureHistoryItem(
    fecha: String,
    sistolica: AnaliticaResponseDTO,
    diastolica: AnaliticaResponseDTO
) {
    val (statusColor, statusText) = when {
        sistolica.valor >= 135 || diastolica.valor >= 85 -> Pair(AlertRed, "ALTA")
        sistolica.valor < 90 || diastolica.valor < 60 -> Pair(Color(0xFFFF9800), "BAJA")
        else -> Pair(Color(0xFF4CAF50), "NORMAL")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral de color
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tensión
                    Text(
                        "${sistolica.valor.toInt()}/${diastolica.valor.toInt()} mmHg",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF263238)
                    )

                    // Badge de estado
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

                Spacer(Modifier.height(8.dp))

                // Fecha y hora
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatDate(fecha),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.width(16.dp))

                    Icon(
                        Icons.Default.AccessTime,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatTime(fecha),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsHistoryDialog(
    alertas: List<LogEmergencia>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = BackgroundGray,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(Brush.verticalGradient(listOf(HeaderGradientStart, HeaderGradientEnd)))
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Historial de Alarmas",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Todas las alertas registradas",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (alertas.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Sin alarmas",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    "No hay alertas registradas",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(alertas) { alerta ->
                        AlertHistoryItem(alerta)
                    }
                }
            }
        }
    }
}

// ========== VISTA DE LISTA DE TENSIÓN ARTERIAL ==========
@Composable
fun BloodPressureListView(
    analiticas: List<AnaliticaResponseDTO>,
    onAddClick: () -> Unit,
    seniorId: Long
) {
    // Agrupar mediciones
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // BOTÓN AÑADIR
        item {
            ActionButton("Registrar tensión", Icons.Default.Favorite, Color(0xFF1976D2), onAddClick)
        }

        if (mediciones.isEmpty()) {
            item {
                EmptyStateMessage("Sin mediciones de tensión", Icons.Default.Favorite)
            }
        } else {
            // GRÁFICO DE EVOLUCIÓN
            item {
                Text(
                    "Evolución temporal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                BloodPressureChart(mediciones)
            }

            // SEPARADOR
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Historial completo (${mediciones.size} mediciones)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // LISTA DE MEDICIONES
            items(mediciones) { (fecha, sistolica, diastolica) ->
                BloodPressureListItem(fecha, sistolica, diastolica)
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}
// ========== ITEM DE LISTA DE TENSIÓN ==========
@Composable
fun BloodPressureListItem(
    fecha: String,
    sistolica: AnaliticaResponseDTO,
    diastolica: AnaliticaResponseDTO
) {
    val (statusColor, statusText) = when {
        sistolica.valor >= 135 || diastolica.valor >= 85 -> Pair(Color(0xFFD32F2F), "ALTA")
        sistolica.valor < 90 || diastolica.valor < 60 -> Pair(Color(0xFFFF9800), "BAJA")
        else -> Pair(Color(0xFF4CAF50), "NORMAL")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral de color
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                // Valores de tensión
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${sistolica.valor.toInt()}/${diastolica.valor.toInt()} mmHg",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF263238)
                        )
                    }

                    // Badge de estado
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

                Spacer(Modifier.height(8.dp))

                // Fecha y hora
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(formatDate(fecha), fontSize = 14.sp, color = Color.Gray)

                    Spacer(Modifier.width(16.dp))

                    Icon(
                        Icons.Default.AccessTime,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(formatTime(fecha), fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ========== DIÁLOGO PARA AÑADIR TENSIÓN ==========
@Composable
fun AddBloodPressureDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var sistolica by remember { mutableStateOf("") }
    var diastolica by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Registrar Tensión Arterial",
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Introduce los valores de tensión arterial del paciente:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = sistolica,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) sistolica = it },
                    label = { Text("Sistólica (alta)") },
                    placeholder = { Text("Ej: 120") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            tint = Color(0xFFD32F2F)
                        )
                    }
                )

                OutlinedTextField(
                    value = diastolica,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) diastolica = it },
                    label = { Text("Diastólica (baja)") },
                    placeholder = { Text("Ej: 80") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            tint = Color(0xFF1976D2)
                        )
                    }
                )

                // Información de referencia
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Valores de referencia:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("• Normal: < 135/85", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        Text("• Alta: ≥ 135/85", fontSize = 11.sp, color = Color(0xFFD32F2F))
                        Text("• Baja: < 90/60", fontSize = 11.sp, color = Color(0xFFFF9800))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sys = sistolica.toDoubleOrNull()
                    val dia = diastolica.toDoubleOrNull()

                    if (sys != null && dia != null && sys > 0 && dia > 0) {
                        onConfirm(sys, dia)
                    } else {
                        Toast.makeText(
                            context,
                            "Introduce valores válidos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
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
fun BloodPressureChart(mediciones: List<Triple<String, AnaliticaResponseDTO, AnaliticaResponseDTO>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Evolución temporal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF263238))
                    Text("${mediciones.size} mediciones", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (mediciones.isNotEmpty()) {
                val allSistolicas = mediciones.map { it.second.valor.toFloat() }
                val allDiastolicas = mediciones.map { it.third.valor.toFloat() }
                val maxValue = maxOf(allSistolicas.maxOrNull() ?: 140f, 140f) + 10
                val minValue = minOf(allDiastolicas.minOrNull() ?: 60f, 60f) - 10

                Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.White)) {
                    val width = size.width
                    val height = size.height
                    val padding = 50f
                    val graphWidth = width - padding * 2
                    val graphHeight = height - padding * 2

                    for (i in 0..5) {
                        val y = padding + (graphHeight * i / 5)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(padding, y),
                            end = androidx.compose.ui.geometry.Offset(width - padding, y),
                            strokeWidth = 1f
                        )
                    }

                    val stepX = graphWidth / (mediciones.size - 1).coerceAtLeast(1)

                    fun valueToY(value: Float): Float {
                        val normalized = (value - minValue) / (maxValue - minValue)
                        return padding + graphHeight * (1 - normalized)
                    }

                    val sistolicaPath = androidx.compose.ui.graphics.Path().apply {
                        mediciones.forEachIndexed { index, (_, sistolica, _) ->
                            val x = padding + stepX * index
                            val y = valueToY(sistolica.valor.toFloat())
                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }

                    drawPath(path = sistolicaPath, color = Color(0xFFD32F2F), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                    mediciones.forEachIndexed { index, (_, sistolica, _) ->
                        val x = padding + stepX * index
                        val y = valueToY(sistolica.valor.toFloat())
                        drawCircle(color = Color(0xFFD32F2F), radius = 6f, center = androidx.compose.ui.geometry.Offset(x, y))
                        drawCircle(color = Color.White, radius = 3f, center = androidx.compose.ui.geometry.Offset(x, y))
                    }

                    val diastolicaPath = androidx.compose.ui.graphics.Path().apply {
                        mediciones.forEachIndexed { index, (_, _, diastolica) ->
                            val x = padding + stepX * index
                            val y = valueToY(diastolica.valor.toFloat())
                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }

                    drawPath(path = diastolicaPath, color = Color(0xFF1976D2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                    mediciones.forEachIndexed { index, (_, _, diastolica) ->
                        val x = padding + stepX * index
                        val y = valueToY(diastolica.valor.toFloat())
                        drawCircle(color = Color(0xFF1976D2), radius = 6f, center = androidx.compose.ui.geometry.Offset(x, y))
                        drawCircle(color = Color.White, radius = 3f, center = androidx.compose.ui.geometry.Offset(x, y))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mediciones.first().first.take(10).substring(5), fontSize = 11.sp, color = Color.Gray)
                    if (mediciones.size > 1) {
                        Text(mediciones.last().first.take(10).substring(5), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text("Sin datos", color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFD32F2F), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Sistólica", fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF1976D2), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Diastólica", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
}



@Composable
fun AlertHistoryItem(alerta: LogEmergencia) {
    val isSOS = alerta.tipo.contains("SOS", ignoreCase = true)
    val alertColor = if (isSOS) Color(0xFFD32F2F) else Color(0xFFFF9800)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSOS) Color(0xFFFFEBEE) else CardWhite
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (isSOS) androidx.compose.foundation.BorderStroke(1.dp, alertColor.copy(alpha = 0.3f)) else null
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral de color
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(alertColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isSOS) "🚨 EMERGENCIA SOS" else alerta.tipo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF263238)
                    )

                    Surface(
                        color = alertColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (isSOS) "CRÍTICA" else "ALERTA",
                            color = alertColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Fecha y hora
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatDateTime(alerta.fecha ?: ""),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // GPS si existe
                if (!alerta.gps.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            alerta.gps,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }


}



