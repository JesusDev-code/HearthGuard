package com.example.tfg_apli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.network.dto.PacienteResumenDTO
import com.example.tfg_apli.network.dto.UsuarioResponseDTO
import com.example.tfg_apli.ui.viewmodels.CaregiverDashboardState
import com.example.tfg_apli.ui.viewmodels.CaregiverViewModel
import com.example.tfg_apli.ui.viewmodels.ProfileViewModel
import com.example.tfg_apli.util.SessionManager
import com.example.tfg_apli.util.TriageLogic

// --- COLORES ZONA CUIDADOR ---
private val HeaderBlueStart = Color(0xFF1565C0)
private val HeaderBlueEnd = Color(0xFF0288D1)

@Composable
fun CaregiverHomeScreen(

    onNavigateToPatientTwin: (Long, String) -> Unit,
    onNavigateToScanQr: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: CaregiverViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val user = SessionManager.currentUser


    var pacienteABorrar by remember { mutableStateOf<PacienteResumenDTO?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        user?.id?.let { viewModel.loadPacientes(it) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(HeaderBlueStart, HeaderBlueEnd)))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Panel de control", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = {
                        profileViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hola, ${user?.nombre}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Tienes pacientes bajo supervisión",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = Color.White)
                    }
                }
            }
        }

        // --- CONTENIDO ---
        Column(modifier = Modifier.padding(16.dp)) {

            // BOTÓN VINCULAR (Usa onNavigateToScanQr)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                FloatingActionButton(
                    onClick = onNavigateToScanQr, // CAMBIO AQUI
                    containerColor = HeaderBlueEnd,
                    contentColor = Color.White
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular paciente")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is CaregiverDashboardState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is CaregiverDashboardState.Error -> Text(
                    (state as CaregiverDashboardState.Error).message,
                    color = Color(0xFFD32F2F)
                )
                is CaregiverDashboardState.Success -> {
                    val pacientes = (state as CaregiverDashboardState.Success).pacientes

                    if (pacientes.isEmpty()) {
                        EmptyDashboardState()
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(pacientes) { paciente ->
                                PatientCard(
                                    paciente = paciente,

                                    onClick = { onNavigateToPatientTwin(paciente.id, paciente.nombre) },
                                    onDeleteClick = { pacienteABorrar = paciente }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (pacienteABorrar != null) {
        AlertDialog(
            onDismissRequest = { pacienteABorrar = null },
            title = { Text("¿Dejar de cuidar?") },
            text = { Text("Vas a desvincular a ${pacienteABorrar?.nombre}. Dejarás de recibir sus alertas.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.desvincularPaciente(pacienteABorrar!!.id)
                        pacienteABorrar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // ✅ CAMBIADO
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pacienteABorrar = null }) { Text("Cancelar") }
            }
        )
    }

    if (showEditProfileDialog && user != null) {
        CaregiverEditProfileDialog(
            user = user,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { nombre, edad, peso, alergias ->
                profileViewModel.updateProfile(nombre, edad, peso, alergias)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun PatientCard(
    paciente: PacienteResumenDTO,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val triage = TriageLogic.analyze(paciente)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.fillMaxSize(), tint = Color.LightGray)
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(14.dp).clip(RoundedCornerShape(7.dp)).background(triage.color).border(2.dp, Color.White, RoundedCornerShape(7.dp)))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    paciente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Adherencia: ${paciente.adherencia}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if(paciente.adherencia < 50) Color(0xFFD32F2F) else Color.Gray
                    )
                    if (!paciente.mensajeAlerta.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            paciente.mensajeAlerta.take(15) + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Desvincular", tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun EmptyDashboardState() {
    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.PersonSearch, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No tienes pacientes asignados",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CaregiverEditProfileDialog(
    user: UsuarioResponseDTO,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, Double?, String?) -> Unit
) {
    var nombre by remember { mutableStateOf(user.nombre) }
    var edad by remember { mutableStateOf(user.edad?.toString() ?: "") }
    var peso by remember { mutableStateOf(user.peso?.toString() ?: "") }
    var alergias by remember { mutableStateOf(user.alergias ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar mi perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre (visible para el paciente)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(nombre, edad.toIntOrNull(), peso.toDoubleOrNull(), alergias) }) { Text("Guardar cambios") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}