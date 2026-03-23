package com.market.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.market.android.ui.BottomTab
import com.market.android.ui.orders.OrdersScreen
import com.market.android.ui.products.ProductsScreen
import com.market.android.ui.scanner.ScannerScreen
import com.market.android.ui.settings.SettingsScreen

data class BottomNavItem(
    val tab: BottomTab,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(BottomTab.Vendas, Icons.Default.ShoppingCart, Icons.Default.ShoppingCart),
    BottomNavItem(BottomTab.Estoque, Icons.Default.List, Icons.Default.List),
    BottomNavItem(BottomTab.Pedidos, Icons.Default.ShoppingBag, Icons.Default.ShoppingBag),
    BottomNavItem(BottomTab.Configuracoes, Icons.Default.Settings, Icons.Default.Settings),
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.tab.route,
                        onClick = {
                            navController.navigate(item.tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.tab.label) },
                        label = { Text(item.tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Vendas.route,
            modifier = Modifier.padding(padding)
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
}
