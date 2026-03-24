package com.market.android.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.market.android.data.model.LoginRequest
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenPrefs.saveTokens(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        condominioId = body.user.condominioId,
                        userRole = body.user.role
                    )
                    _state.value = LoginState.Success
                } else {
                    _state.value = LoginState.Error("Email ou senha incorretos")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Erro de conexão")
            }
        }
    }
}
