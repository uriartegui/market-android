package com.market.android.data.network

import com.market.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("products/barcode/{barcode}")
    suspend fun getProductByBarcode(
        @Header("Authorization") token: String,
        @Path("barcode") barcode: String
    ): Response<Product>

    @POST("orders/kiosk")
    suspend fun createKioskOrder(
        @Header("Authorization") token: String,
        @Body request: KioskOrderRequest
    ): Response<OrderResponse>

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
