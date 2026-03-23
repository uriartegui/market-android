package com.market.android.ui

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")

    // Stock Selection
    object StockSelection : Screen("stock_selection")

    // Main (gestão)
    object Main : Screen("main")

    // Kiosk (venda travada)
    object Scanner : Screen("scanner")
    object Payment : Screen("payment/{orderId}") {
        fun createRoute(orderId: String) = "payment/$orderId"
    }
    object Success : Screen("success/{success}") {
        fun createRoute(success: Boolean) = "success/$success"
    }
}

// Bottom nav tabs
sealed class BottomTab(val route: String, val label: String, val icon: String) {
    object Vendas : BottomTab("tab_vendas", "Vendas", "cart")
    object Estoque : BottomTab("tab_estoque", "Estoque", "inventory")
    object Pedidos : BottomTab("tab_pedidos", "Pedidos", "orders")
    object Configuracoes : BottomTab("tab_config", "Config", "settings")
}
