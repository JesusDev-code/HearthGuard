package com.example.tfg_apli.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.R
import com.example.tfg_apli.ui.viewmodels.LoginState
import com.example.tfg_apli.ui.viewmodels.LoginViewModel
import com.example.tfg_apli.util.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit,
                onNavigateToForgotPassword: () -> Unit = {}
) {
    val loginViewModel: LoginViewModel = viewModel()
    val loginState by loginViewModel.loginState.collectAsState()
    val context = LocalContext.current

    var isRegistering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("SENIOR") }

    // --- BIOMETRÍA ---
    var canUseBiometric by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {

        val result = withContext(Dispatchers.IO) {
            val biometricManager = BiometricManager.from(context)


            val hasEncryptedData = SecureStorage.getCredentials(context) != null


            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

            val canAuth = (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS)


            canAuth && hasEncryptedData
        }


        canUseBiometric = result
    }

    fun launchBiometric() {
        val activity = context.findActivity() ?: return
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    loginViewModel.loginBiometric(context)

                    val creds = SecureStorage.getCredentials(context)
                    if (creds != null) {
                        Toast.makeText(context, "Identidad verificada", Toast.LENGTH_SHORT).show()
                        loginViewModel.login(creds.first, creds.second, context)
                    }
                }
                override fun onAuthenticationFailed() { super.onAuthenticationFailed() }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Health Guard")
            .setSubtitle("Usa tu huella o cara")
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // LOGO
            if (!isRegistering) {

                Image(
                    painter = painterResource(id = R.drawable.logo_app),
                    contentDescription = "Logo Health Guard",
                    modifier = Modifier.size(250.dp).padding(bottom = 16.dp)
                )
            }

            Text(
                text = if (isRegistering) "Elige tu perfil" else "HealthGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // REGISTRO
            if (isRegistering) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿Cuál es tu situación?", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoleSelectionCard("Mayor", "Letra grande", Icons.Default.AccessibilityNew, selectedRole == "SENIOR", { selectedRole = "SENIOR" }, Modifier.weight(1f))
                        RoleSelectionCard("Crónico", "Gestión diaria", Icons.Default.Medication, selectedRole == "CRONICO", { selectedRole = "CRONICO" }, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoleSelectionCard("Salud Mental", "Apoyo", Icons.Default.SelfImprovement, selectedRole == "APOYO", { selectedRole = "APOYO" }, Modifier.weight(1f))
                        RoleSelectionCard("Cuidador", "Supervisión", Icons.Default.AssignmentInd, selectedRole == "CUIDADOR", { selectedRole = "CUIDADOR" }, Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // BOTÓN LOGIN / REGISTRO
            Button(
                onClick = {
                    if (isRegistering) {
                        loginViewModel.register(name, email, password, selectedRole)
                    } else {
                        loginViewModel.login(email, password, context)
                    }
                },
                enabled = loginState != LoginState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (loginState == LoginState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isRegistering) "Crear cuenta" else "Entrar", fontSize = 18.sp)
                }
            }

            // BOTÓN BIOMÉTRICO
            if (!isRegistering && canUseBiometric && loginState != LoginState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { launchBiometric() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acceso biométrico", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { isRegistering = !isRegistering }) {
                Text(if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "Crear cuenta nueva")
            }

            if (!isRegistering) {
                TextButton(
                    onClick = { onNavigateToForgotPassword() }
                ) {
                    Text("¿Olvidaste tu contraseña?")
                }
            }

            if (loginState is LoginState.Error) {
                Text((loginState as LoginState.Error).message, color = MaterialTheme.colorScheme.error)
            }
            if (loginState is LoginState.Success) {
                LaunchedEffect(Unit) { onLoginSuccess() }
            }
        }
    }
}

@Composable
fun RoleSelectionCard(title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
    Column(modifier = modifier.clip(RoundedCornerShape(8.dp)).border(2.dp, borderColor, RoundedCornerShape(8.dp)).background(backgroundColor).clickable { onClick() }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
    }
}


fun LoginViewModel.loginBiometric(context: Context) {

}

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}