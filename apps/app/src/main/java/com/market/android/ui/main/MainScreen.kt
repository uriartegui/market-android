package com.market.android.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.market.android.data.preferences.TokenPreferences
import com.market.android.ui.BottomTab
import com.market.android.ui.orders.OrdersScreen
import com.market.android.ui.products.ProductsScreen
import com.market.android.ui.scanner.ScannerScreen
import com.market.android.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NavItem(
    val tab: BottomTab,
    val icon: ImageVector,
    val label: String
)

val navItems = listOf(
    NavItem(BottomTab.Vendas, Icons.Default.ShoppingCart, "Vendas"),
    NavItem(BottomTab.Estoque, Icons.Default.List, "Estoque"),
    NavItem(BottomTab.Pedidos, Icons.Default.ShoppingBag, "Pedidos"),
    NavItem(BottomTab.Configuracoes, Icons.Default.Settings, "Config"),
)

@Composable
fun MainScreen(
    onKioskMode: () -> Unit,
    onLogout: () -> Unit,
    onCheckout: (String) -> Unit
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val context = LocalContext.current
    val tokenPrefs = remember { TokenPreferences(context) }
    val scope = rememberCoroutineScope()

    // Dialog de PIN — pedido toda vez que sai de Vendas
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun handleTabClick(tab: BottomTab) {
        if (tab == BottomTab.Vendas) {
            navigateTo(tab.route)
        } else {
            // Sempre pede PIN ao acessar qualquer aba protegida
            pendingRoute = tab.route
            pinInput = ""
            pinError = false
            showPinDialog = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.tab.route,
                        onClick = { handleTabClick(item.tab) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Vendas.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomTab.Vendas.route) {
                ScannerScreen(onCheckout = onCheckout)
            }
            composable(BottomTab.Estoque.route) {
                ProductsScreen()
            }
            composable(BottomTab.Pedidos.route) {
                OrdersScreen()
            }
            composable(BottomTab.Configuracoes.route) {
                SettingsScreen(
                    onKioskMode = onKioskMode,
                    onLogout = onLogout
                )
            }
        }
    }

    // ── Dialog de PIN ──────────────────────────────────────
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pendingRoute = null
                pinInput = ""
                pinError = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Acesso Restrito", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Digite o PIN do gerente para acessar esta área.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        label = { Text("PIN de 4 dígitos") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text("PIN incorreto. Tente novamente.", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val savedPin = tokenPrefs.managerPin.first()
                            if (savedPin == null || pinInput == savedPin) {
                                showPinDialog = false
                                pinInput = ""
                                pinError = false
                                pendingRoute?.let { navigateTo(it) }
                                pendingRoute = null
                            } else {
                                pinError = true
                                pinInput = ""
                            }
                        }
                    },
                    enabled = pinInput.length == 4
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pendingRoute = null
                    pinInput = ""
                    pinError = false
                }) { Text("Cancelar") }
            }
        )
    }
}
