package com.market.android.data.network

import android.content.Context
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject

class TokenAuthenticator(private val context: Context) : Authenticator {

    private val BASE_URL = "https://backend-production-f38c2.up.railway.app/api/"

    // Cliente HTTP separado (sem authenticator) para evitar loop infinito
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Se já tentou renovar nessa requisição, desiste (evita loop)
        if (response.request.header("X-Retry-Auth") != null) return null

        val tokenPrefs = TokenPreferences(context)

        // Pega o refresh token salvo (de forma síncrona)
        val refreshToken = runBlocking {
            tokenPrefs.refreshToken.first()
        } ?: return null // Sem refresh token → vai para o login

        // Chama o endpoint de refresh diretamente via OkHttp (síncrono)
        val body = JSONObject().put("refreshToken", refreshToken)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url("${BASE_URL}auth/refresh")
            .post(body)
            .build()

        val refreshResponse = try {
            refreshClient.newCall(refreshRequest).execute()
        } catch (e: Exception) {
            return null
        }

        if (!refreshResponse.isSuccessful) {
            // Notifica o backend para mandar alerta (Telegram/WhatsApp)
            notifySessionExpired(tokenPrefs)
            // Limpa tokens → AppNavigation vai redirecionar para Login
            runBlocking { tokenPrefs.clearTokens() }
            return null
        }

        val bodyStr = refreshResponse.body?.string() ?: return null
        val json = JSONObject(bodyStr)
        val newAccessToken = json.optString("accessToken").takeIf { it.isNotEmpty() } ?: return null
        val newRefreshToken = json.optString("refreshToken").takeIf { it.isNotEmpty() } ?: return null

        // Salva os novos tokens
        runBlocking {
            tokenPrefs.updateAccessToken(newAccessToken, newRefreshToken)
        }

        // Reexecuta a requisição original com o novo access token
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .header("X-Retry-Auth", "true")
            .build()
    }

    private fun notifySessionExpired(tokenPrefs: TokenPreferences) {
        try {
            val condominioId = runBlocking { tokenPrefs.condominioId.first() }
            val bodyStr = if (condominioId != null) {
                "{\"condominioId\":\"$condominioId\"}"
            } else {
                "{}"
            }
            val body = bodyStr.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${BASE_URL}auth/session-expired")
                .post(body)
                .build()
            refreshClient.newCall(request).execute()
        } catch (e: Exception) {
            // Ignora silenciosamente — notificação é best-effort
        }
    }
}
