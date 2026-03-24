package com.market.android.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.market.android.data.model.AdjustStockRequest
import com.market.android.data.model.Product
import com.market.android.data.model.ProductRequest
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ProductsState {
    object Loading : ProductsState()
    data class Success(val products: List<Product>) : ProductsState()
    data class Error(val message: String) : ProductsState()
}

sealed class ProductActionState {
    object Idle : ProductActionState()
    object Loading : ProductActionState()
    object Success : ProductActionState()
    data class Error(val message: String) : ProductActionState()
}

class ProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _state = MutableStateFlow<ProductsState>(ProductsState.Loading)
    val state: StateFlow<ProductsState> = _state

    private val _actionState = MutableStateFlow<ProductActionState>(ProductActionState.Idle)
    val actionState: StateFlow<ProductActionState> = _actionState

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    private val _showLowStock = MutableStateFlow(false)
    val showLowStock: StateFlow<Boolean> = _showLowStock

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _state.value = ProductsState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.getProducts(
                    token,
                    search = _search.value.ifBlank { null },
                    lowStock = if (_showLowStock.value) true else null
                )
                if (response.isSuccessful) {
                    _state.value = ProductsState.Success(response.body() ?: emptyList())
                } else {
                    _state.value = ProductsState.Error("Erro ao carregar produtos")
                }
            } catch (e: Exception) {
                _state.value = ProductsState.Error("Erro de conexão")
            }
        }
    }

    fun onSearchChange(query: String) {
        _search.value = query
        loadProducts()
    }

    fun toggleLowStock() {
        _showLowStock.value = !_showLowStock.value
        loadProducts()
    }

    fun createProduct(product: Product) {
        viewModelScope.launch {
            _actionState.value = ProductActionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val request = ProductRequest(
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    quantity = product.quantity,
                    category = product.category,
                    barcode = product.barcode,
                    imageUrl = product.imageUrl
                )
                val response = api.createProduct(token, request)
                if (response.isSuccessful) {
                    loadProducts()
                    _actionState.value = ProductActionState.Success
                } else {
                    _actionState.value = ProductActionState.Error("Erro ao criar produto (${response.code()})")
                }
            } catch (e: Exception) {
                _actionState.value = ProductActionState.Error("Erro de conexão")
            }
        }
    }

    fun updateProduct(id: String, product: Product) {
        viewModelScope.launch {
            _actionState.value = ProductActionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val request = ProductRequest(
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    quantity = product.quantity,
                    category = product.category,
                    barcode = product.barcode,
                    imageUrl = product.imageUrl
                )
                val response = api.updateProduct(token, id, request)
                if (response.isSuccessful) {
                    loadProducts()
                    _actionState.value = ProductActionState.Success
                } else {
                    _actionState.value = ProductActionState.Error("Erro ao atualizar produto")
                }
            } catch (e: Exception) {
                _actionState.value = ProductActionState.Error("Erro de conexão")
            }
        }
    }

    fun adjustStock(id: String, quantity: Int, type: String) {
        viewModelScope.launch {
            _actionState.value = ProductActionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.adjustStock(token, id, AdjustStockRequest(quantity, type))
                if (response.isSuccessful) {
                    loadProducts()
                    _actionState.value = ProductActionState.Success
                } else {
                    _actionState.value = ProductActionState.Error("Erro ao ajustar estoque")
                }
            } catch (e: Exception) {
                _actionState.value = ProductActionState.Error("Erro de conexão")
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            _actionState.value = ProductActionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.deleteProduct(token, id)
                if (response.isSuccessful) {
                    loadProducts()
                    _actionState.value = ProductActionState.Success
                } else {
                    _actionState.value = ProductActionState.Error("Erro ao excluir produto")
                }
            } catch (e: Exception) {
                _actionState.value = ProductActionState.Error("Erro de conexão")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ProductActionState.Idle
    }
}
