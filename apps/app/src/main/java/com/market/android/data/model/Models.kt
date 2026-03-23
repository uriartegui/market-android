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
