package com.market.android.ui

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Scanner : Screen("scanner")
    object Payment : Screen("payment/{orderId}") {
        fun createRoute(orderId: String) = "payment/$orderId"
    }
    object Success : Screen("success/{success}") {
        fun createRoute(success: Boolean) = "success/$success"
    }
}
