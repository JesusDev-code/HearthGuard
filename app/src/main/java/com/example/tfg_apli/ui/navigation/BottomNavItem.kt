package com.example.tfg_apli.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem/**
 * Clase sellada (sealed class) que define los destinos de la barra de navegación inferior.
 * Las sealed classes son ideales para la navegación ya que restringen la jerarquía,
 * permitiendo que solo los objetos definidos aquí sean opciones válidas de menú.
 */
sealed class BottomNavItem(
    val route: String,      // Identificador único de la ruta (usado por NavHost)
    val title: String,      // Texto que se mostrará debajo del icono
    val icon: ImageVector   // El icono visual de Material Design
) {
    // Opción: Pantalla de Inicio / Resumen principal
    object Home : BottomNavItem(
        route = "home",
        title = "Inicio",
        icon = Icons.Filled.Home
    )

    // Opción: Pantalla de Mi Salud (patologías, medicamentos, etc.)
    object MyHealth : BottomNavItem(
        route = "my_health",
        title = "Mi Salud",
        icon = Icons.Filled.Favorite
    )

    // Opción: Escáner de medicamentos por cámara
    object Scanner : BottomNavItem(
        route = "scanner",
        title = "Escáner",
        icon = Icons.Filled.CameraAlt
    )

    // Opción: Configuración del Perfil de usuario
    object Profile : BottomNavItem(
        route = "profile",
        title = "Perfil",
        icon = Icons.Filled.Person
    )
}(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Inicio",
        icon = Icons.Filled.Home
    )

    object MyHealth : BottomNavItem(
        route = "my_health",
        title = "Mi Salud",
        icon = Icons.Filled.Favorite
    )

    object Scanner : BottomNavItem(
        route = "scanner",
        title = "Escáner",
        icon = Icons.Filled.CameraAlt
    )

    object Profile : BottomNavItem(
        route = "profile",
        title = "Perfil",
        icon = Icons.Filled.Person
    )
}
