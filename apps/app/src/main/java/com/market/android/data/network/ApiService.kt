package com.market.android.data.network

import com.market.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── AUTH ─────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ─── USERS ────────────────────────────────────────────
    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserResponse>

    @POST("users/kiosk")
    suspend fun createKioskUser(
        @Header("Authorization") token: String,
        @Body request: KioskUserRequest
    ): Response<KioskUserResponse>

    @GET("users/kiosk")
    suspend fun getKioskUser(
        @Header("Authorization") token: String
    ): Response<KioskUserResponse>

    // ─── CONDOMINIOS ──────────────────────────────────────
    @GET("condominios")
    suspend fun getCondominios(
        @Header("Authorization") token: String
    ): Response<List<Condominio>>

    @POST("condominios")
    suspend fun createCondominio(
        @Header("Authorization") token: String,
        @Body request: CreateCondominioRequest
    ): Response<Condominio>

    @POST("condominios/join")
    suspend fun joinCondominio(
        @Header("Authorization") token: String,
        @Body request: JoinCondominioRequest
    ): Response<Condominio>

    // ─── PRODUCTS ─────────────────────────────────────────
    @GET("products")
    suspend fun getProducts(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("lowStock") lowStock: Boolean? = null
    ): Response<List<Product>>

    @GET("products/barcode/{barcode}")
    suspend fun getProductByBarcode(
        @Header("Authorization") token: String,
        @Path("barcode") barcode: String
    ): Response<Product>

    @GET("products/{id}")
    suspend fun getProduct(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Product>

    @POST("products")
    suspend fun createProduct(
        @Header("Authorization") token: String,
        @Body product: Product
    ): Response<Product>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body product: Product
    ): Response<Product>

    @PATCH("products/{id}/stock")
    suspend fun adjustStock(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: AdjustStockRequest
    ): Response<Product>

    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Product>

    // ─── ORDERS ───────────────────────────────────────────
    @POST("orders/kiosk")
    suspend fun createKioskOrder(
        @Header("Authorization") token: String,
        @Body request: KioskOrderRequest
    ): Response<OrderResponse>

    @GET("orders")
    suspend fun getOrders(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<OrdersPage>

    @GET("orders/stats")
    suspend fun getOrderStats(
        @Header("Authorization") token: String
    ): Response<OrderStats>

    @GET("orders/{id}")
    suspend fun getOrder(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<OrderDetail>

    // ─── PAYMENTS ─────────────────────────────────────────
    @POST("payments/pix/{orderId}")
    suspend fun generatePix(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: String
    ): Response<PaymentResponse>

    @GET("payments/status/{orderId}")
    suspend fun getPaymentStatus(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: String
    ): Response<PaymentResponse>
}
