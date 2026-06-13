package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screen.AdminDashboardScreen
import com.example.ui.screen.DashboardScreen
import com.example.ui.screen.LoginScreen
import com.example.ui.screen.PaymentScreen
import com.example.ui.screen.ScanScreen
import com.example.ui.viewmodel.JimpitanViewModel

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: JimpitanViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
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
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateScan = {
                    viewModel.clearScanned()
                    navController.navigate("scan")
                }
            )
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("scan") {
            ScanScreen(
                onBack = {
                    navController.popBackStack()
                },
                onQrScanned = { qrText ->
                    viewModel.scanQr(qrText)
                    navController.navigate("payment") {
                        popUpTo("scan") { inclusive = true }
                    }
                }
            )
        }
        composable("payment") {
            PaymentScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
