package com.example.tfg_apli.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.ui.navigation.BottomNavItem
import com.example.tfg_apli.util.SessionManager

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToBloodPressure: () -> Unit,
    onNavigateToAddMedication: (MedicamentoResponseDTO, String) -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(BottomNavItem.Home, BottomNavItem.MyHealth, BottomNavItem.Scanner, BottomNavItem.Profile)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            val user = SessionManager.currentUser
            val isSenior = user?.rol == "SENIOR" || user?.modoInterfaz == "SENIOR"

            val shouldShowBottomBar = if (isSenior && currentRoute == BottomNavItem.Home.route) false
            else currentRoute in items.map { it.route } || currentRoute == "blood_pressure_registration" || currentRoute == "appointments"

            if (shouldShowBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                if (item.route == BottomNavItem.Home.route) {
                                    navController.navigate(BottomNavItem.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = navController, startDestination = BottomNavItem.Home.route) {

                composable(BottomNavItem.Home.route) {
                    HomeScreen(navController = navController, onNavigateToBloodPressure = { navController.navigate("blood_pressure_registration") })
                }

                composable(BottomNavItem.Scanner.route) {
                    ScannerScreen(onNavigateToAddMedication = onNavigateToAddMedication)
                }


                composable("scanner_picker") {
                    ScannerScreen(
                        isPickMode = true,
                        onCodePicked = { code, name ->
                            navController.previousBackStackEntry?.savedStateHandle?.let {
                                it["scanned_code"] = code
                                it["scanned_name"] = name
                            }
                            navController.popBackStack()
                        },
                        onNavigateToAddMedication = { _, _ -> }
                    )
                }

                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(onLogout = onLogout)
                }

                composable(BottomNavItem.MyHealth.route) {
                    MyHealthScreen(
                        onNavigateToAddMedication = {
                            navController.navigate(BottomNavItem.Scanner.route) {
                                popUpTo(BottomNavItem.MyHealth.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToScanner = {
                            navController.navigate(BottomNavItem.Scanner.route) {
                                popUpTo(BottomNavItem.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        navController = navController
                    )
                }

                composable("blood_pressure_registration") {
                    BloodPressureScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("appointments") {
                    AppointmentsScreen(onBack = { navController.popBackStack() })
                }
                composable("crisis_mode") {
                    CrisisModeScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}