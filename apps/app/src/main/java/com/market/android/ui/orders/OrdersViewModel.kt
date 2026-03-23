package com.market.android.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.market.android.data.model.OrderDetail
import com.market.android.data.model.OrderStats
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class OrdersState {
    object Loading : OrdersState()
    data class Success(
        val orders: List<OrderDetail>,
        val totalPages: Int,
        val currentPage: Int
    ) : OrdersState()
    data class Error(val message: String) : OrdersState()
}

sealed class StatsState {
    object Loading : StatsState()
    data class Success(val stats: OrderStats) : StatsState()
    object Error : StatsState()
}

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val ordersState: StateFlow<OrdersState> = _ordersState

    private val _statsState = MutableStateFlow<StatsState>(StatsState.Loading)
    val statsState: StateFlow<StatsState> = _statsState

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus

    private var currentPage = 1

    init {
        loadStats()
        loadOrders()
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.getOrderStats(token)
                if (response.isSuccessful) {
                    _statsState.value = StatsState.Success(response.body()!!)
                } else {
                    _statsState.value = StatsState.Error
                }
            } catch (e: Exception) {
                _statsState.value = StatsState.Error
            }
        }
    }

    fun loadOrders(page: Int = 1) {
        viewModelScope.launch {
            _ordersState.value = OrdersState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.getOrders(
                    token,
                    status = _selectedStatus.value,
                    page = page,
                    limit = 20
                )
                if (response.isSuccessful) {
                    val body = response.body()!!
                    currentPage = page
                    _ordersState.value = OrdersState.Success(
                        orders = body.orders,
                        totalPages = body.totalPages,
                        currentPage = page
                    )
                } else {
                    _ordersState.value = OrdersState.Error("Erro ao carregar pedidos")
                }
            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error("Erro de conexão")
            }
        }
    }

    fun filterByStatus(status: String?) {
        _selectedStatus.value = status
        loadOrders()
    }
}
