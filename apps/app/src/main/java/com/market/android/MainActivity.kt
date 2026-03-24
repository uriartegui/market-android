package com.market.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import com.market.android.ui.Screen
import com.market.android.ui.login.LoginScreen
import com.market.android.ui.main.MainScreen
import com.market.android.ui.payment.PaymentScreen
import com.market.android.ui.scanner.ScannerScreen
import com.market.android.ui.stockselection.StockSelectionScreen
import com.market.android.ui.success.SuccessScreen
import com.market.android.ui.theme.MarketAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        enableEdgeToEdge()
        setContent {
            MarketAndroidTheme {
                AppNavigation()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableKioskMode()
    }

    private fun enableKioskMode() {
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenPrefs = remember { TokenPreferences(context) }
    val token by tokenPrefs.accessToken.collectAsState(initial = null)
    val condominioId by tokenPrefs.condominioId.collectAsState(initial = null)
    val userRole by tokenPrefs.userRole.collectAsState(initial = null)

    // Detecta se caiu no login por expiração (já tinha token antes)
    var sessionExpiredOnce by remember { mutableStateOf(false) }
    val hasToken = token != null

    LaunchedEffect(hasToken) {
        // Se tinha token e agora não tem mais (e já inicializou) → sessão expirou
        if (!hasToken && sessionExpiredOnce) {
            navController.navigate(Screen.Login.route + "?expired=true") {
                popUpTo(0) { inclusive = true }
            }
        }
        if (hasToken) sessionExpiredOnce = true
    }

    val isKiosk = userRole == "KIOSK"

    val startDestination = when {
        token == null -> Screen.Login.route
        condominioId == null -> Screen.StockSelection.route
        isKiosk -> Screen.Scanner.route   // KIOSK → scanner direto, sem navegação
        else -> Screen.Main.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.StockSelection.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                sessionExpired = false
            )
        }

        composable(Screen.Login.route + "?expired=true") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.StockSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                sessionExpired = true
            )
        }

        composable(Screen.StockSelection.route) {
            StockSelectionScreen(
                onStockSelected = {
                    val dest = if (isKiosk) Screen.Scanner.route else Screen.Main.route
                    navController.navigate(dest) {
                        popUpTo(Screen.StockSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onKioskMode = {
                    navController.navigate(Screen.Scanner.route) {
                        popUpTo(Screen.Main.route) { inclusive = false }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCheckout = { orderId ->
                    navController.navigate(Screen.Payment.createRoute(orderId))
                }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                onCheckout = { orderId ->
                    navController.navigate(Screen.Payment.createRoute(orderId))
                },
                onManagerUnlock = {
                    // PIN correto → vai para o app completo de gerente
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Payment.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            PaymentScreen(
                orderId = orderId,
                onPaymentResult = { success ->
                    navController.navigate(Screen.Success.createRoute(success)) {
                        popUpTo(Screen.Scanner.route)
                    }
                }
            )
        }

        composable(Screen.Success.route) { backStackEntry ->
            val success = backStackEntry.arguments?.getString("success")?.toBoolean() ?: false
            SuccessScreen(
                success = success,
                onFinish = {
                    navController.navigate(Screen.Scanner.route) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
