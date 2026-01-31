package com.example.tfg_apli.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_apli.network.client.RetrofitClient
import com.example.tfg_apli.network.dto.UsuarioRegistroDTO
import com.example.tfg_apli.util.SecureStorage
import com.example.tfg_apli.util.SessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // --- LOGIN: USAMOS Dispatchers.IO PARA NO CONGELAR LA PANTALLA ---
    fun login(email: String, password: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = LoginState.Loading
            try {
                // 1. Autenticación Real (Firebase)
                auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUid = auth.currentUser?.uid ?: throw Exception("Error UID")

                // 2. Obtener perfil de NUESTRO Backend (PostgreSQL)
                val userProfile = RetrofitClient.apiService.miPerfil()

                // 3. Guardar en Sesión Local
                SessionManager.login(userProfile)

                // 4. Guardar credenciales encriptadas para biometría futura
                SecureStorage.saveCredentials(context, email, password)

                _loginState.value = LoginState.Success

            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    // --- REGISTRO
    fun register(nombre: String, email: String, password: String, rolSeleccionado: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = LoginState.Loading
            try {
                // 1. Crear usuario en Firebase Auth
                auth.createUserWithEmailAndPassword(email, password).await()

                // 2. Decidir el ROL OFICIAL para la base de datos (Permisos)
                // En base de datos 'CRONICO' o 'APOYO' se guardan con rol 'SENIOR'
                val rolParaBackend = when (rolSeleccionado) {
                    "CRONICO", "APOYO", "CONTROL", "MENTAL" -> "SENIOR"
                    else -> rolSeleccionado
                }

                // 3. Preparar el DTO con el NUEVO CAMPO 'modoInterfaz'
                val registroDTO = UsuarioRegistroDTO(
                    nombre = nombre,
                    email = email,
                    rol = rolParaBackend,
                    modoInterfaz = rolSeleccionado // <--- ¡AQUÍ ENVIAMOS EL MODO VISUAL!
                )

                // 4. Enviar al Backend
                val userProfileDesdeBackend = RetrofitClient.apiService.registro(registroDTO)

                // 5. Iniciar sesión localmente
                val usuarioConModoCorrecto = userProfileDesdeBackend.copy(modoInterfaz = rolSeleccionado)
                SessionManager.login(usuarioConModoCorrecto)

                _loginState.value = LoginState.Success

            } catch (e: Exception) {

                try { auth.currentUser?.delete() } catch (_: Exception) {}

                _loginState.value = LoginState.Error(e.message ?: "Error al registrarse.")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}