package com.example.tfg_apli.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.tfg_apli.util.SessionManager





private val SeniorColorScheme = lightColorScheme(
    primary = SeniorPrimary,
    onPrimary = SeniorOnPrimary,
    background = SeniorBackground,
    onBackground = SeniorOnBackground,
    surface = SeniorSurface,
    onSurface = SeniorOnBackground,
    error = SeniorError
)

private val CaregiverColorScheme = lightColorScheme(
    primary = CaregiverPrimary,
    onPrimary = CaregiverOnPrimary,
    background = CaregiverBackground,
    onBackground = CaregiverOnBackground,
    surface = CaregiverSurface,
    onSurface = CaregiverOnBackground,
    secondary = CaregiverSecondary
)

private val ApoyoColorScheme = lightColorScheme(
    primary = ApoyoPrimary,
    onPrimary = ApoyoOnPrimary,
    background = ApoyoBackground,
    onBackground = ApoyoOnBackground,
    surface = ApoyoSurface,
    onSurface = ApoyoOnBackground,
    secondary = ApoyoSecondary
)

// Definición de Formas

// Senior: Formas cuadradas y robustas
val SeniorShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

// Apoyo: Formas muy redondas y suaves
val ApoyoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp)
)

// Estándar
val StandardShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun HealthGuardTheme(

    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 1. ESCUCHAMOS EL CAMBIO DE USUARIO EN TIEMPO REAL
    val user by SessionManager.currentUserFlow.collectAsState()

    // 2. DECIDIMOS EL MODO (Si no hay usuario, por defecto Senior para login)

    val mode = user?.modoInterfaz ?: user?.rol ?: "SENIOR"

    // 3. SELECCIONAMOS LA COMBINACIÓN GANADORA
    val (colorScheme, typography, shapes) = when (mode) {
        "SENIOR" -> Triple(SeniorColorScheme, SeniorTypography, SeniorShapes)
        "APOYO" -> Triple(ApoyoColorScheme, StandardTypography, ApoyoShapes)
        "CONTROL", "CUIDADOR" -> Triple(CaregiverColorScheme, StandardTypography, StandardShapes)
        else -> Triple(SeniorColorScheme, SeniorTypography, SeniorShapes)
    }

    // 4. CAMBIAMOS LA BARRA DE ESTADO DEL MÓVIL
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Iconos blancos si el fondo es oscuro, oscuros si es claro
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // 5. APLICAMOS EL TEMA A TODA LA APP
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}