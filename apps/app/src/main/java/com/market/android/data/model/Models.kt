package com.market.android.data.model

// ─── AUTH ───────────────────────────────────────────
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

data class RefreshRequest(
    val refreshToken: String
)

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val condominioId: String?
)

// ─── PRODUCT ─────────────────────────────────────────
data class Product(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val quantity: Int,
    val imageUrl: String?,
    val category: String?,
    val barcode: String?,
    val condominioId: String
)

// ─── ORDER ───────────────────────────────────────────
data class CartItem(
    val product: Product,
    var quantity: Int
)

data class OrderItemRequest(
    val productId: String,
    val quantity: Int
)

data class KioskOrderRequest(
    val items: List<OrderItemRequest>
)

data class OrderResponse(
    val id: String,
    val status: String,
    val total: Double,
    val items: List<OrderItemResponse>
)

data class OrderItemResponse(
    val id: String,
    val quantity: Int,
    val price: Double,
    val productId: String
)

// ─── PAYMENT ─────────────────────────────────────────
data class PaymentResponse(
    val id: String,
    val status: String,
    val amount: Double,
    val pixQrCode: String?,
    val pixQrCodeBase64: String?,
    val expiresAt: String?
)
// ─── CONDOMINIO ───────────────────────────────────────
data class Condominio(
    val id: String,
    val name: String,
    val address: String,
    val code: String
)

data class CreateCondominioRequest(
    val name: String,
    val address: String,
    val code: String
)

data class JoinCondominioRequest(
    val code: String
)

// ─── STOCK ───────────────────────────────────────────
data class AdjustStockRequest(
    val quantity: Int,
    val type: String, // ADD, REMOVE, SET
    val reason: String? = null
)

// ─── ORDER STATS ─────────────────────────────────────
data class StatPeriod(
    val total: Double,
    val count: Int
)

data class OrderStats(
    val today: StatPeriod,
    val week: StatPeriod,
    val month: StatPeriod,
    val allTime: StatPeriod,
    val pendingOrders: Int
)

// ─── ORDER DETAIL ─────────────────────────────────────
data class OrderItemDetail(
    val id: String,
    val quantity: Int,
    val price: Double,
    val productId: String,
    val product: ProductSummary?
)

data class ProductSummary(
    val name: String,
    val imageUrl: String?
)

data class OrderDetail(
    val id: String,
    val status: String,
    val total: Double,
    val createdAt: String,
    val items: List<OrderItemDetail>,
    val payment: PaymentResponse?
)

data class OrdersPage(
    val orders: List<OrderDetail>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

// ─── KIOSK USER ──────────────────────────────────────
data class KioskUserRequest(
    val name: String
)

data class KioskUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val condominioId: String
)
