package com.example.tfg_apli.util

import com.example.tfg_apli.network.dto.UsuarioResponseDTO
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _currentUser = MutableStateFlow<UsuarioResponseDTO?>(null)
    val currentUserFlow: StateFlow<UsuarioResponseDTO?> = _currentUser.asStateFlow()

    private val _isSelfManagementEnabled = MutableStateFlow(false)
    val isSelfManagementEnabled: StateFlow<Boolean> = _isSelfManagementEnabled.asStateFlow()

    var currentUser: UsuarioResponseDTO?
        get() = _currentUser.value
        set(value) {
            _currentUser.value = value
        }

    fun login(user: UsuarioResponseDTO) {
        currentUser = user
        _isSelfManagementEnabled.value = false
    }

    fun logout() {
        _currentUser.value = null
        _isSelfManagementEnabled.value = false
        FirebaseAuth.getInstance().signOut()
    }


    fun isSessionValid(): Boolean {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser != null && _currentUser.value != null
    }


    fun validateSession() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null && _currentUser.value != null) {

            _currentUser.value = null
            _isSelfManagementEnabled.value = false
        }
    }

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun setSelfManagement(enabled: Boolean) {
        _isSelfManagementEnabled.value = enabled
    }
}
