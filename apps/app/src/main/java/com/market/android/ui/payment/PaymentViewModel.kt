package com.market.android.ui.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.market.android.data.model.PaymentResponse
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class PaymentState {
    object Loading : PaymentState()
    data class WaitingPayment(val payment: PaymentResponse) : PaymentState()
    object Approved : PaymentState()
    object Rejected : PaymentState()
    data class Error(val message: String) : PaymentState()
}

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _state = MutableStateFlow<PaymentState>(PaymentState.Loading)
    val state: StateFlow<PaymentState> = _state

    private var pollingJob: Job? = null

    fun generatePix(orderId: String) {
        viewModelScope.launch {
            _state.value = PaymentState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.generatePix(token, orderId)
                if (response.isSuccessful) {
                    _state.value = PaymentState.WaitingPayment(response.body()!!)
                    startPolling(orderId)
                } else {
                    _state.value = PaymentState.Error("Erro ao gerar Pix")
                }
            } catch (e: Exception) {
                _state.value = PaymentState.Error("Erro de conexão")
            }
        }
    }

    private fun startPolling(orderId: String) {
        pollingJob = viewModelScope.launch {
            val token = "Bearer ${tokenPrefs.accessToken.first()}"
            repeat(40) {
                delay(3000)
                try {
                    val response = api.getPaymentStatus(token, orderId)
                    if (response.isSuccessful) {
                        when (response.body()?.status) {
                            "APPROVED" -> {
                                _state.value = PaymentState.Approved
                                return@launch
                            }
                            "REJECTED", "EXPIRED" -> {
                                _state.value = PaymentState.Rejected
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    // continua tentando
                }
            }
            _state.value = PaymentState.Rejected
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
