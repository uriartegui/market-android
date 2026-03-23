package com.market.android.ui.stockselection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.market.android.data.model.Condominio
import com.market.android.data.model.CreateCondominioRequest
import com.market.android.data.model.JoinCondominioRequest
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class StockSelectionState {
    object Loading : StockSelectionState()
    data class Success(val condominios: List<Condominio>) : StockSelectionState()
    data class Error(val message: String) : StockSelectionState()
}

sealed class StockActionState {
    object Idle : StockActionState()
    object Loading : StockActionState()
    object Success : StockActionState()
    data class Error(val message: String) : StockActionState()
}

class StockSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _state = MutableStateFlow<StockSelectionState>(StockSelectionState.Loading)
    val state: StateFlow<StockSelectionState> = _state

    private val _actionState = MutableStateFlow<StockActionState>(StockActionState.Idle)
    val actionState: StateFlow<StockActionState> = _actionState

    init {
        loadCondominios()
    }

    fun loadCondominios() {
        viewModelScope.launch {
            _state.value = StockSelectionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.getCondominios(token)
                if (response.isSuccessful) {
                    _state.value = StockSelectionState.Success(response.body() ?: emptyList())
                } else {
                    _state.value = StockSelectionState.Error("Erro ao carregar estoques")
                }
            } catch (e: Exception) {
                _state.value = StockSelectionState.Error("Erro de conexão")
            }
        }
    }

    fun selectCondominio(condominioId: String) {
        viewModelScope.launch {
            val token = tokenPrefs.accessToken.first() ?: return@launch
            tokenPrefs.saveTokens(token, condominioId)
            _actionState.value = StockActionState.Success
        }
    }

    fun createCondominio(name: String, address: String, code: String) {
        viewModelScope.launch {
            _actionState.value = StockActionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.createCondominio(
                    token,
                    CreateCondominioRequest(name, address, code)
                )
                if (response.isSuccessful) {
                    loadCondominios()
                    _actionState.value = StockActionState.Success
                } else {
                    _actionState.value = StockActionState.Error("Erro ao criar estoque")
                }
            } catch (e: Exception) {
                _actionState.value = StockActionState.Error("Erro de conexão")
            }
        }
    }

    fun joinCondominio(code: String) {
        viewModelScope.launch {
            _actionState.value = StockActionState.Loading
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.joinCondominio(token, JoinCondominioRequest(code))
                if (response.isSuccessful) {
                    loadCondominios()
                    _actionState.value = StockActionState.Success
                } else {
                    _actionState.value = StockActionState.Error("Código inválido")
                }
            } catch (e: Exception) {
                _actionState.value = StockActionState.Error("Erro de conexão")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = StockActionState.Idle
    }
}
