package com.market.android.ui.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.market.android.data.model.CartItem
import com.market.android.data.model.KioskOrderRequest
import com.market.android.data.model.OrderItemRequest
import com.market.android.data.model.Product
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ScannerState {
    object Idle : ScannerState()
    object Scanning : ScannerState()
    object Loading : ScannerState()
    data class ProductFound(val product: Product) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Loading : CheckoutState()
    data class Success(val orderId: String) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Scanning)
    val scannerState: StateFlow<ScannerState> = _scannerState

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    fun onBarcodeDetected(barcode: String) {
        if (_scannerState.value is ScannerState.Loading) return
        viewModelScope.launch {
            _scannerState.value = ScannerState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.getProductByBarcode(token, barcode)
                if (response.isSuccessful) {
                    _scannerState.value = ScannerState.ProductFound(response.body()!!)
                } else {
                    _scannerState.value = ScannerState.Error("Produto não encontrado")
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error("Erro de conexão")
            }
        }
    }

    fun addToCart(product: Product) {
        val current = _cart.value.toMutableList()
        val existing = current.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity++
        } else {
            current.add(CartItem(product, 1))
        }
        _cart.value = current
        _scannerState.value = ScannerState.Scanning
    }

    fun removeFromCart(productId: String) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun resumeScanning() {
        _scannerState.value = ScannerState.Scanning
    }

    fun checkout() {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val items = _cart.value.map {
                    OrderItemRequest(it.product.id, it.quantity)
                }
                val response = api.createKioskOrder(token, KioskOrderRequest(items))
                if (response.isSuccessful) {
                    _checkoutState.value = CheckoutState.Success(response.body()!!.id)
                    _cart.value = emptyList()
                } else {
                    _checkoutState.value = CheckoutState.Error("Erro ao criar pedido")
                }
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error("Erro de conexão")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenPrefs.clearTokens()
        }
    }

    val cartTotal: Double
        get() = _cart.value.sumOf { it.product.price * it.quantity }

    val cartCount: Int
        get() = _cart.value.sumOf { it.quantity }
}
