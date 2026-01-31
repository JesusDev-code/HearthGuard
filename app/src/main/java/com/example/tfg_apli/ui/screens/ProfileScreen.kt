package com.example.tfg_apli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.network.dto.UsuarioPatologia
import com.example.tfg_apli.network.dto.UsuarioResponseDTO
import com.example.tfg_apli.ui.viewmodels.MedicalHistoryState
import com.example.tfg_apli.ui.viewmodels.ProfileViewModel
import com.example.tfg_apli.util.SessionManager
import com.example.tfg_apli.ui.components.PatientQrDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// --- PALETA DE COLORES MANUAL ---

// 1. MODO SENIOR
private val SeniorBlue = Color(0xFF1565C0)
private val SeniorTeal = Color(0xFF00695C)

// 2. MODO CRÓNICO
private val ChronicBlue = Color(0xFF1976D2)
private val ChronicCyan = Color(0xFF00BCD4)
private val AppleGray = Color(0xFFF2F2F7)

// 3. MODO MENTAL
private val MentalMint = Color(0xFFE0F2F1)
private val MentalDark = Color(0xFF4A148C)

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val viewModel: ProfileViewModel = viewModel()
    val user by viewModel.user.collectAsState()
    val medicalHistory by viewModel.medicalHistory.collectAsState()
    val logoutState by viewModel.logoutState.collectAsState()
    val isSelfManagement by SessionManager.isSelfManagementEnabled.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    // Dialogo de confirmación para desvincular
    var showUnlinkDialog by remember { mutableStateOf(false) }

    var showHistoryDialog by remember { mutableStateOf(false) }

    if (logoutState) { LaunchedEffect(Unit) { onLogout() } }

    val mode = user?.modoInterfaz ?: user?.rol ?: "SENIOR"

    // --- LÓGICA DE COLORES ---
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
        else -> Triple(
            MaterialTheme.colorScheme.background,
            listOf(SeniorBlue, SeniorTeal),
            Color.White
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {

        // --- CABECERA DINÁMICA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(gradient)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // PARTE IZQUIERDA: Icono + Nombre + Email
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Mi perfil",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = user?.nombre ?: "Usuario",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                        Text(
                            text = user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }

                // PARTE DERECHA: Edad, Peso, Alergias
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Edad
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cake,
                            null,
                            tint = contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${user?.edad ?: "-"} años",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Peso
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            null,
                            tint = contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${user?.peso ?: "-"} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alergias
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = if (!user?.alergias.isNullOrBlank()) Color(0xFFFFCA28) else contentColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user?.alergias?.take(10) ?: "Sin alergias",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // 1. Tarjeta de Modo Independiente
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo independiente",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gestionar mi propia medicación",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isSelfManagement,
                            onCheckedChange = { SessionManager.setSelfManagement(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // --- NUEVA SECCIÓN: CUIDADOR ---
            if (!user?.nombreCuidador.isNullOrEmpty()) {
                item {
                    Text(
                        text = "Tu cuidador",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Te cuida:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = user?.nombreCuidador ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF6C00)
                                )
                            }
                            // Botón de desvincular
                            IconButton(onClick = { showUnlinkDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Desvincular",
                                    tint = Color(0xFFD84315)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Historial
            item {
                when (val state = medicalHistory) {
                    is MedicalHistoryState.Loading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is MedicalHistoryState.Success -> {
                        val curadas = state.pathologies.filter { it.fechaCura != null }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showHistoryDialog = true }
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "Historial médico",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            if (curadas.isEmpty()) "Sin registros"
                                            else "${curadas.size} enfermedades curadas",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                    is MedicalHistoryState.Error -> {
                        Text(state.message, color = Color(0xFFD32F2F))
                    }
                }
            }

            // 3. Botones de Acción
            item { Spacer(modifier = Modifier.height(32.dp)) }
            item {
                if (user?.rol == "SENIOR") {
                    ProfileMenuItem(
                        title = "Ver código QR",
                        icon = Icons.Default.QrCode,
                        isDestructive = false
                    ) { showQrDialog = true }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                ProfileMenuItem("Editar mis datos", Icons.Default.Edit) { showEditDialog = true }
                Spacer(modifier = Modifier.height(16.dp))
                ProfileMenuItem("Cerrar sesión", Icons.Default.ExitToApp, isDestructive = true) { viewModel.logout() }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // --- DIÁLOGOS ---

    if (showEditDialog && user != null) {
        EditProfileDialog(user!!, { showEditDialog = false }) { n, e, p, a -> viewModel.updateProfile(n, e, p, a); showEditDialog = false }
    }

    if (showQrDialog && user != null) {
        PatientQrDialog(
            seniorId = user!!.id,
            seniorName = user!!.nombre,
            onDismiss = { showQrDialog = false }
        )
    }

    // Diálogo de Desvinculación
    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("¿Desvincular cuidador?") },
            text = { Text("Dejarás de compartir tus datos con ${user?.nombreCuidador}. ¿Estás seguro?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.desvincularCuidador()
                        showUnlinkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Sí, desvincular") }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancelar") }
            }
        )
    }
    if (showHistoryDialog) {
        when (val state = medicalHistory) {
            is MedicalHistoryState.Success -> {
                MedicalHistoryDialog(
                    pathologies = state.pathologies.filter { it.fechaCura != null },
                    onDismiss = { showHistoryDialog = false }
                )
            }
            else -> { /* No hacer nada */ }
        }
    }
}

@Composable
fun HistoryItem(pathology: UsuarioPatologia) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = pathology.patologia.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Curado: ${pathology.fechaCura?.take(10) ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                if (!pathology.medicacionHistorial.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Medicación: ${pathology.medicacionHistorial}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(title: String, icon: ImageVector, isDestructive: Boolean = false, onClick: () -> Unit) {
    val containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = contentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = contentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EditProfileDialog(user: UsuarioResponseDTO, onDismiss: () -> Unit, onConfirm: (String, Int?, Double?, String?) -> Unit) {
    var nombre by remember { mutableStateOf(user.nombre) }
    var edad by remember { mutableStateOf(user.edad?.toString() ?: "") }
    var peso by remember { mutableStateOf(user.peso?.toString() ?: "") }
    var alergias by remember { mutableStateOf(user.alergias ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar datos") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
            OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") }, keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
            )
            OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = alergias, onValueChange = { alergias = it }, label = { Text("Alergias") })
        }},
        confirmButton = { Button(onClick = { onConfirm(nombre, edad.toIntOrNull(), peso.toDoubleOrNull(), alergias) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalHistoryDialog(
    pathologies: List<UsuarioPatologia>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // CABECERA CON DEGRADADO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1565C0),
                                    Color(0xFF0288D1)
                                )
                            )
                        )
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    // Título + Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pathology.patologia.nombre,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
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

                                    // Fecha
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
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                pathology.medicacionHistorial,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Badge tipo
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = if (pathology.patologia.esCronica)
                                            Color(0xFF7B1FA2).copy(alpha = 0.1f)
                                        else
                                            Color.Gray.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (pathology.patologia.esCronica) "Crónica" else "Temporal",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pathology.patologia.esCronica) Color(0xFF7B1FA2) else Color.Gray
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
