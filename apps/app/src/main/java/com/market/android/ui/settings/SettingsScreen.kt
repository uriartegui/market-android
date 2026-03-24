package com.market.android.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.market.android.data.network.RetrofitClient
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenPrefs = TokenPreferences(application)
    private val api = RetrofitClient.apiService

    private val _kioskEmail = MutableStateFlow<String?>(null)
    val kioskEmail: StateFlow<String?> = _kioskEmail

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    init { loadKioskUser() }

    private fun loadKioskUser() {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.getKioskUser(token)
                if (response.isSuccessful) {
                    _kioskEmail.value = response.body()?.email
                }
            } catch (_: Exception) {}
        }
    }

    fun createKioskUser(name: String) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                val response = api.createKioskUser(
                    token,
                    com.market.android.data.model.KioskUserRequest(name)
                )
                if (response.isSuccessful) {
                    _kioskEmail.value = response.body()?.email
                    _actionMessage.value = "Usuário kiosk criado com sucesso!"
                } else {
                    _actionMessage.value = "Erro ao criar usuário kiosk"
                }
            } catch (e: Exception) {
                _actionMessage.value = "Erro de conexão"
            }
        }
    }

    fun resetKioskUser() {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenPrefs.accessToken.first()}"
                api.resetKioskUser(token)
                _kioskEmail.value = null
                _actionMessage.value = "Conta kiosk removida. Crie uma nova."
            } catch (e: Exception) {
                _actionMessage.value = "Erro ao resetar conta kiosk"
            }
        }
    }

    fun logout(tokenPreferences: TokenPreferences) {
        viewModelScope.launch {
            tokenPreferences.clearTokens()
        }
    }

    fun clearMessage() { _actionMessage.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onKioskMode: () -> Unit,
    onLogout: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val kioskEmail by vm.kioskEmail.collectAsState()
    val actionMessage by vm.actionMessage.collectAsState()
    val context = LocalContext.current
    val tokenPrefs = remember { TokenPreferences(context) }

    var showKioskDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    val currentPin by tokenPrefs.managerPin.collectAsState(initial = null)

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            kotlinx.coroutines.delay(2000)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configurações") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Modo Kiosk
            Text("Modo Venda", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ativar Modo Kiosk", fontWeight = FontWeight.Bold)
                            Text("Trava o app na tela de venda",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = onKioskMode) { Text("Ativar") }
                    }
                }
            }

            // PIN do gerente
            Text("Segurança", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PIN do Gerente", fontWeight = FontWeight.Bold)
                        Text(
                            if (currentPin != null) "PIN configurado — toque 5x no status do scanner para desbloquear"
                            else "Configure um PIN para desbloquear o modo kiosk",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { showPinDialog = true }) {
                        Text(if (currentPin != null) "Alterar" else "Definir")
                    }
                }
            }

            // Usuário Kiosk
            Text("Conta Kiosk", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (kioskEmail != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Conta kiosk configurada", fontWeight = FontWeight.Bold)
                                Text(kioskEmail!!, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { vm.resetKioskUser() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Resetar") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sem conta kiosk", fontWeight = FontWeight.Bold)
                                Text("Crie uma conta para o tablet de vendas",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { showKioskDialog = true }) { Text("Criar") }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Logout
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text("Sair da conta")
            }
        }
    }

    actionMessage?.let {
        Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
    }

    if (showKioskDialog) {
        var name by remember { mutableStateOf("Tablet Kiosk") }
        AlertDialog(
            onDismissRequest = { showKioskDialog = false },
            title = { Text("Criar Conta Kiosk") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do tablet") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.createKioskUser(name)
                    showKioskDialog = false
                }) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { showKioskDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Definir PIN do Gerente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O PIN de 4 dígitos será exigido para sair do modo kiosk.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4) newPin = it },
                        label = { Text("Novo PIN (4 dígitos)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            tokenPrefs.saveManagerPin(newPin)
                            showPinDialog = false
                        }
                    },
                    enabled = newPin.length == 4
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você precisará fazer login novamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.logout(tokenPrefs)
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
