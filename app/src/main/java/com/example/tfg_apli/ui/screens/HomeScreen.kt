package com.example.tfg_apli.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tfg_apli.R
import com.example.tfg_apli.ui.viewmodels.HomeViewModel
import com.example.tfg_apli.ui.theme.*
import com.example.tfg_apli.util.AlarmScheduler
import com.example.tfg_apli.util.LocalIntakeManager
import com.example.tfg_apli.util.SessionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.tfg_apli.ui.viewmodels.AppointmentsViewModel
import com.example.tfg_apli.util.VoiceAssistantManager


// --- COLORES ESPECÍFICOS ---
private val HeaderBlue = Color(0xFF1565C0)
private val HeaderTeal = Color(0xFF00695C)
private val SoftBackground = Color(0xFFF5F7FA)
private val MentalDark = Color(0xFF4A148C)
private val AppleGray = Color(0xFFF2F2F7)
private val SoftSalmon = Color(0xFFFF8A80)

@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateToBloodPressure: () -> Unit
) {
    val sessionUser by SessionManager.currentUserFlow.collectAsState()
    if (sessionUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.White))
    } else {
        HomeScreenContent(navController, onNavigateToBloodPressure)
    }
}

@Composable
private fun HomeScreenContent(
    navController: NavController,
    onNavigateToBloodPressure: () -> Unit
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val userState by homeViewModel.userState.collectAsState()
    val aiTipState by homeViewModel.aiTipState.collectAsState()
    val medicationState by homeViewModel.medicationState.collectAsState()
    val bloodPressureState by homeViewModel.bloodPressureState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    // 1. SOLICITUD DE PERMISO DE NOTIFICACIONES
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 2. SOLICITUD DE PERMISO ALARMAS EXACTAS
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    }

    LaunchedEffect(medicationState) {
        if (medicationState is HomeViewModel.MedicationListState.Success) {
            val meds = (medicationState as HomeViewModel.MedicationListState.Success).medications
            meds.forEach { med -> AlarmScheduler.scheduleAlarmsForMedication(context, med) }
        }
    }

    val userMode = if (userState is HomeViewModel.UserState.UserSuccess) {
        val user = (userState as HomeViewModel.UserState.UserSuccess).user
        user.modoInterfaz ?: user.rol ?: "SENIOR"
    } else "SENIOR"

    val backgroundColor = when (userMode) {
        "APOYO" -> ApoyoBackground
        "SENIOR" -> SoftBackground
        "CRONICO", "CONTROL" -> AppleGray
        else -> SoftBackground
    }

    val onSosAction = { homeViewModel.reportEmergency("SOS_MANUAL") }

    Scaffold(containerColor = backgroundColor) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (userMode) {
                "SENIOR" -> HomeContentSenior(
                    userState, aiTipState, onSosAction, onBloodPressureClick = onNavigateToBloodPressure, navController
                )
                "CRONICO", "CONTROL" -> HomeContentChronic(
                    userState, medicationState, aiTipState, bloodPressureState, onBloodPressureClick = onNavigateToBloodPressure, navController, onSos = onSosAction
                )
                "APOYO", "MENTAL" -> HomeContentMental(
                    userState, medicationState, aiTipState, navController, onSos = onSosAction
                )
                else -> HomeContentSenior(
                    userState, aiTipState, onSosAction, onBloodPressureClick = onNavigateToBloodPressure, navController
                )
            }
        }
    }
}

// ----------------------------------------------------
// 1. MODO SENIOR
// ----------------------------------------------------
@Composable
fun HomeContentSenior(
    userState: HomeViewModel.UserState,
    aiTipState: HomeViewModel.AiTipState,
    onSosClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    navController: NavController
) {
    val userName = if (userState is HomeViewModel.UserState.UserSuccess) {
        userState.user.nombre ?: "Usuario"
    } else "Usuario"

    var showSosDialog by remember { mutableStateOf(false) }

    //ASISTENTE DE VOZ
    val context = LocalContext.current
    val appointmentsViewModel: AppointmentsViewModel = viewModel()
    val citasState by appointmentsViewModel.citasState.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val voiceAssistant = remember {
        VoiceAssistantManager(
            context = context,
            onListening = { listening -> isListening = listening },
            onError = { error -> voiceError = error }
        )
    }

    //CARGAR CITAS AL INICIAR
    LaunchedEffect(Unit) {
        appointmentsViewModel.loadCitas()
    }

    DisposableEffect(Unit) {
        onDispose { voiceAssistant.shutdown() }
    }

    // Permiso de micrófono
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // OBTENER CITAS DEL ESTADO ACTUAL
            val citas = when (val state = citasState) {
                is AppointmentsViewModel.CitasState.Success -> state.citas
                else -> emptyList()
            }

            //DEBUG: Ver cuántas citas hay
            android.util.Log.d("VoiceAssistant", "Citas disponibles: ${citas.size}")
            citas.forEach { cita ->
                android.util.Log.d("VoiceAssistant", "Cita: ${cita.titulo} - ${cita.fechaHora}")
            }

            voiceAssistant.startListening(citas)
        } else {
            voiceError = "Necesitas dar permiso de micrófono"
        }
    }

    voiceError?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            voiceError = null
        }
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = Color(0xFFD32F2F),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("¡SOS ACTIVADO!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Se está enviando una alerta de emergencia a tus contactos.", color = Color.White, fontSize = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showSosDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("CERRAR", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    //Box para el layout con botón flotante
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.verticalGradient(colors = listOf(HeaderBlue, HeaderTeal)))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_app),
                        contentDescription = "Logo",
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hola,", color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)
                        Text(userName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Emergencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            Text("Avisar a mi cuidador", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = { onSosClick(); showSosDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("SOS", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("¿Qué necesitas hoy?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SeniorBigButton("Mis Pastillas", Icons.Default.Medication, Color(0xFF009688), Modifier.weight(1f)) { navController.navigate("my_health") }
                    SeniorBigButton("Escanear", Icons.Default.QrCodeScanner, Color(0xFF1976D2), Modifier.weight(1f)) { navController.navigate("scanner") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SeniorBigButton("Citas Médicas", Icons.Default.Event, Color(0xFF9C27B0), Modifier.weight(1f)) { navController.navigate("appointments") }
                    SeniorBigButton("Mi Tensión", Icons.Default.Favorite, Color(0xFFE91E63), Modifier.weight(1f), onBloodPressureClick)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SeniorBigButton("Mi Perfil", Icons.Default.Person, Color(0xFFFF9800), Modifier.weight(1f)) { navController.navigate("profile") }
                    Spacer(modifier = Modifier.weight(1f))
                }

                AiTipCard(aiTipState, isSenior = true)
                Spacer(modifier = Modifier.height(80.dp)) // Espacio para el botón flotante
            }
        }

        //BOTÓN FLOTANTE DE VOZ
        FloatingActionButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    // OBTENER CITAS DEL ESTADO ACTUAL
                    val citas = when (val state = citasState) {
                        is AppointmentsViewModel.CitasState.Success -> state.citas
                        else -> emptyList()
                    }

                    // DEBUG: Ver cuántas citas hay
                    android.util.Log.d("VoiceAssistant", "Iniciando voz. Citas: ${citas.size}")

                    voiceAssistant.startListening(citas)
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp),
            containerColor = if (isListening) Color(0xFFFF3B30) else HeaderBlue,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Asistente de voz",
                modifier = Modifier.size(32.dp)
            )
        }

        // Mensaje de error
        voiceError?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                containerColor = Color(0xFFD32F2F)
            ) {
                Text(error, color = Color.White)
            }
        }
    }
}


// ----------------------------------------------------
// 2. MODO CRÓNICO
// ----------------------------------------------------
@Composable
fun HomeContentChronic(
    userState: HomeViewModel.UserState,
    medicationState: HomeViewModel.MedicationListState,
    aiTipState: HomeViewModel.AiTipState,
    bloodPressureState: HomeViewModel.BloodPressureState,
    onBloodPressureClick: () -> Unit,
    navController: NavController,
    onSos: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var totalMeds by remember { mutableIntStateOf(0) }

    LaunchedEffect(medicationState) {
        if (medicationState is HomeViewModel.MedicationListState.Success) {
            val meds = medicationState.medications
            var totalTomas = 0
            var tomasHechas = 0

            meds.forEach { med ->
                val freq = med.frecuenciaHoras ?: 24
                val vecesAlDia = (24 / freq).coerceAtLeast(1)
                totalTomas += vecesAlDia

                val startHour = (med.horaInicio ?: "09:00").split(":").first().toInt()
                val startMinute = (med.horaInicio ?: "09:00").split(":").last().toInt()

                for (i in 0 until vecesAlDia) {
                    val timeString = String.format("%02d:%02d", (startHour + (i * freq)) % 24, startMinute)
                    if (LocalIntakeManager.isTaken(context, med.id, timeString)) tomasHechas++
                }
            }

            totalMeds = meds.size
            progress = if (totalTomas > 0) tomasHechas.toFloat() / totalTomas.toFloat() else 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleGray)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Resumen", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM")), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }

            FilledIconButton(
                onClick = { navController.navigate("profile") },
                modifier = Modifier.size(50.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CaregiverPrimary)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = Color.White)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Medicación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${(progress * 100).toInt()}% Completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = CaregiverPrimary)
                    Text(if (progress == 1f) "¡Objetivo cumplido!" else "Sigue con tu plan", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(70.dp), color = Color(0xFFE5E5EA), strokeWidth = 8.dp)
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(70.dp), color = CaregiverPrimary, strokeWidth = 8.dp, strokeCap = StrokeCap.Round)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Accesos Rápidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Widget Tensión (Izquierda)
            Card(
                onClick = { onBloodPressureClick() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).height(180.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tensión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    when (bloodPressureState) {
                        is HomeViewModel.BloodPressureState.Success -> {
                            val sys = bloodPressureState.systolic
                            val dia = bloodPressureState.diastolic

                            Column {
                                Text("$sys/$dia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("mmHg", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            sys >= 135 || dia >= 85 -> Color(0xFFFFEBEE)
                                            sys < 90 || dia < 60 -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFE8F5E9)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp).background(
                                        when {
                                            sys >= 135 || dia >= 85 -> Color(0xFFD32F2F)
                                            sys < 90 || dia < 60 -> Color(0xFFFF6F00)
                                            else -> Color(0xFF4CAF50)
                                        },
                                        CircleShape
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when {
                                        sys >= 135 || dia >= 85 -> "Alta"
                                        sys < 90 || dia < 60 -> "Baja"
                                        else -> "Normal"
                                    },
                                    color = when {
                                        sys >= 135 || dia >= 85 -> Color(0xFFD32F2F)
                                        sys < 90 || dia < 60 -> Color(0xFFFF6F00)
                                        else -> Color(0xFF2E7D32)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is HomeViewModel.BloodPressureState.NoData -> {
                            Column {
                                Text("--/--", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("Sin datos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        is HomeViewModel.BloodPressureState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFFF2D55))
                        }
                        is HomeViewModel.BloodPressureState.Error -> {
                            Column {
                                Text("Error", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                                Text("No disponible", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Columna Derecha
            Column(modifier = Modifier.weight(1f).height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
                DashboardSmallButton("Fármacos", "$totalMeds Activos", Icons.Default.Medication, Color(0xFF5856D6), Modifier.weight(1f)) {
                    navController.navigate("my_health")
                }
                Spacer(Modifier.height(8.dp))
                DashboardSmallButton("Citas", "Ver agenda", Icons.Default.CalendarToday, Color(0xFF9C27B0), Modifier.weight(1f)) {
                    navController.navigate("appointments")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSos,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Warning, null)
            Spacer(Modifier.width(8.dp))
            Text("EMERGENCIA SOS", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFF9500))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Sugerencia Inteligente", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    when (aiTipState) {
                        is HomeViewModel.AiTipState.AiTipSuccess -> Text(aiTipState.tip, style = MaterialTheme.typography.bodyMedium)
                        else -> Text("Analizando datos...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ----------------------------------------------------
// 3. MODO MENTAL
// ----------------------------------------------------
@Composable
fun HomeContentMental(
    userState: HomeViewModel.UserState,
    medicationState: HomeViewModel.MedicationListState,
    aiTipState: HomeViewModel.AiTipState,
    navController: NavController,
    onSos: () -> Unit
) {
    val userName = if (userState is HomeViewModel.UserState.UserSuccess) {
        userState.user.nombre?.split(" ")?.firstOrNull() ?: "Amigo"
    } else "Amigo"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.8f), ApoyoBackground)))
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Respira, $userName.", style = MaterialTheme.typography.headlineLarge, color = MentalDark)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Un paso a la vez.", style = MaterialTheme.typography.bodyLarge, color = MentalDark.copy(alpha = 0.7f))
            }

            IconButton(
                onClick = { navController.navigate("profile") },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MentalDark)
            }
        }

        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ApoyoPrimary),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth().height(110.dp).clickable { navController.navigate("crisis_mode") },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SelfImprovement, null, tint = ApoyoOnPrimary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Necesito Calma", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ApoyoOnPrimary)
                }
            }

            Button(
                onClick = onSos,
                colors = ButtonDefaults.buttonColors(containerColor = SoftSalmon, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Support, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contactar Ayuda (SOS)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("appointments") }
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(ApoyoSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Event, null, tint = MentalDark)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Mis Citas y Terapia", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MentalDark)
                        Text("Revisa tu agenda", color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }

            Text("Tu medicación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            if (medicationState is HomeViewModel.MedicationListState.Success && medicationState.medications.isNotEmpty()) {
                val nextMed = medicationState.medications.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(ApoyoSecondary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Medication, null, tint = MentalDark)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(nextMed.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MentalDark)
                            Text("Cuando estés listo", color = Color.Gray)
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF81C784))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Todo en orden por ahora.", fontSize = 16.sp, color = MentalDark)
                    }
                }
            }

            AiTipCard(aiTipState, isSenior = false)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- COMPONENTES AUXILIARES ---
@Composable
fun AiTipCard(aiTipState: HomeViewModel.AiTipState, isSenior: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isSenior) SeniorSurface else Color.White),
        elevation = CardDefaults.cardElevation(if (isSenior) 4.dp else 0.dp),
        shape = RoundedCornerShape(if (isSenior) 12.dp else 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, null, tint = if (isSenior) Color(0xFFFBC02D) else MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Consejo de Salud", fontWeight = FontWeight.Bold, fontSize = if (isSenior) 22.sp else 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            when (aiTipState) {
                is HomeViewModel.AiTipState.AiTipSuccess -> Text(aiTipState.tip, fontSize = if (isSenior) 20.sp else 16.sp, lineHeight = if (isSenior) 28.sp else 24.sp)
                is HomeViewModel.AiTipState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> Text("No disponible")
            }
        }
    }
}

@Composable
fun SeniorBigButton(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        modifier = modifier.height(110.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}

@Composable
fun DashboardSmallButton(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}
