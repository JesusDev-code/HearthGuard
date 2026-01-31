package com.example.tfg_apli.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tfg_apli.ui.screens.*
import com.example.tfg_apli.util.SessionManager
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // 1. LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    val user = SessionManager.currentUser
                    if (user?.rol == "CUIDADOR") {
                        navController.navigate("caregiver_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },

                onNavigateToForgotPassword = {
                    navController.navigate("forgot_password")
                }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 2. MAIN SENIOR
        composable("main") {
            MainScreen(
                onLogout = { navController.navigate("login") { popUpTo("main") { inclusive = true } } },
                onNavigateToBloodPressure = { navController.navigate("blood_pressure_registration") },
                onNavigateToAddMedication = { med, code ->
                    val encodedName = java.net.URLEncoder.encode(med.nombre, "UTF-8")
                    navController.navigate("add_medication/-1/$encodedName/$code/Desconocido")
                }
            )
        }

        // 3. DASHBOARD CUIDADOR
        composable("caregiver_dashboard") {
            CaregiverHomeScreen(
                onNavigateToPatientTwin = { id, name -> navController.navigate("patient_twin/$id/$name") },
                onNavigateToScanQr = { navController.navigate("simple_qr_scanner") },
                onLogout = { navController.navigate("login") { popUpTo("caregiver_dashboard") { inclusive = true } } }
            )
        }

        // 4. VINCULACIÓN DE PACIENTE (QR SIMPLE)
        composable("simple_qr_scanner") {
            SimpleQrScannerScreen(
                onBack = { navController.popBackStack() },

            )
        }

        // 5. GEMELO DIGITAL
        composable(
            "patient_twin/{seniorId}/{name}",
            arguments = listOf(
                navArgument("seniorId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("seniorId") ?: 0L
            val name = backStackEntry.arguments?.getString("name") ?: ""

            PatientTwinScreen(
                seniorId = id,
                name = name,
                navController = navController
            )
        }

        // --- 6. RUTA DEL ESCÁNER PICKER
        composable("scanner_picker") {
            ScannerScreen(
                isPickMode = true, // Modo selección
                onCodePicked = { code, name ->

                    navController.previousBackStackEntry?.savedStateHandle?.apply {

                        set("scanned_name", name)


                        set("scanned_code", code)
                    }

                    navController.popBackStack()
                }
            )
        }


        // 7. AÑADIR MEDICACIÓN
        composable(
            "add_medication/{seniorId}/{name}/{code}/{lab}",
            arguments = listOf(
                navArgument("seniorId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType },
                navArgument("code") { type = NavType.StringType },
                navArgument("lab") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val seniorId = backStackEntry.arguments?.getLong("seniorId") ?: -1L
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val code = backStackEntry.arguments?.getString("code") ?: ""
            val lab = URLDecoder.decode(backStackEntry.arguments?.getString("lab") ?: "", StandardCharsets.UTF_8.toString())

            AddMedicationScreen(
                seniorId = seniorId,
                nombre = name,
                codigoNacional = code,
                laboratorio = lab,
                onMedicationAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // 8. EXTRAS
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