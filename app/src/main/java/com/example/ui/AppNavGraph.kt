package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screen.*
import com.example.ui.viewmodel.JimpitanViewModel
import com.example.ui.viewmodel.LoginState

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    viewModel: JimpitanViewModel
) {
    val navController = rememberNavController()
    val loginState by viewModel.loginState.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    var startRoute by remember { mutableStateOf("splash") }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier
    ) {
        // ── Splash ────────────────────────────────────────────────────────────
        composable("splash") {
            SplashScreen(
                onAnimationFinished = {
                    // Try to restore session
                    val hasSession = viewModel.checkSavedSession()
                    if (hasSession) {
                        val role = viewModel.userRole.value
                        if (role == "ADMIN") {
                            navController.navigate("admin_dashboard") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("dashboard") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    val role = viewModel.userRole.value
                    if (role == "ADMIN") {
                        navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Petugas Dashboard ─────────────────────────────────────────────────
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateScan = { navController.navigate("scanner") },
                onNavigateHistory = { navController.navigate("history") },
                onNavigateProfile = { navController.navigate("profile") }
            )
        }

        // ── History Screen ────────────────────────────────────────────────────
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Profile Screen ────────────────────────────────────────────────────
        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Scanner Screen ────────────────────────────────────────────────────
        composable("scanner") {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onQrScanned = { qrText ->
                    viewModel.scanQr(qrText)
                    navController.navigate("payment")
                }
            )
        }


        // ── Payment Screen ────────────────────────────────────────────────────
        composable("payment") {
            PaymentScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate("scanner") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        }

        // ── Admin Dashboard ───────────────────────────────────────────────────
        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateWargaDetail = { wargaId ->
                    navController.navigate("warga_detail/$wargaId")
                }
            )
        }

        // ── Warga Detail ──────────────────────────────────────────────────────
        composable(
            route = "warga_detail/{wargaId}",
            arguments = listOf(navArgument("wargaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val wargaId = backStackEntry.arguments?.getString("wargaId") ?: return@composable
            WargaDetailScreen(
                wargaId = wargaId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
