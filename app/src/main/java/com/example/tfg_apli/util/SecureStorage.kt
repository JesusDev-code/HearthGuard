package com.example.tfg_apli.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

object SecureStorage {

    private const val PREFS_NAME = "secret_health_guard_prefs"
    private const val KEY_EMAIL = "encrypted_email"
    private const val KEY_PASS = "encrypted_pass"


    private fun getEncryptedPrefs(context: Context): SharedPreferences? {
        try {
            return createPrefs(context)
        } catch (e: Exception) {
            e.printStackTrace()

            deleteSharedPreferences(context, PREFS_NAME)
            try {

                return createPrefs(context)
            } catch (e2: Exception) {
                e2.printStackTrace()

                return null
            }
        }
    }

    private fun createPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    private fun deleteSharedPreferences(context: Context, name: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(name)
            } else {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
                val dir = File(context.applicationInfo.dataDir, "shared_prefs")
                File(dir, "$name.xml").delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Guardar credenciales
    fun saveCredentials(context: Context, email: String, pass: String) {
        try {
            val prefs = getEncryptedPrefs(context)
            prefs?.edit()?.apply {
                putString(KEY_EMAIL, email)
                putString(KEY_PASS, pass)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Recuperar credenciales
    fun getCredentials(context: Context): Pair<String, String>? {
        return try {
            val prefs = getEncryptedPrefs(context) ?: return null
            val email = prefs.getString(KEY_EMAIL, null)
            val pass = prefs.getString(KEY_PASS, null)

            if (!email.isNullOrBlank() && !pass.isNullOrBlank()) {
                Pair(email, pass)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Borrar datos
    fun clear(context: Context) {
        try {
            getEncryptedPrefs(context)?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}