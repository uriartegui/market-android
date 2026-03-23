package com.market.android.ui.stockselection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.market.android.data.model.Condominio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSelectionScreen(
    onStockSelected: () -> Unit,
    vm: StockSelectionViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val actionState by vm.actionState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        if (actionState is StockActionState.Success) {
            onStockSelected()
            vm.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Estoques") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Novo estoque")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val s = state) {
                is StockSelectionState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is StockSelectionState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { vm.loadCondominios() }) { Text("Tentar novamente") }
                        }
                    }
                }

                is StockSelectionState.Success -> {
                    if (s.condominios.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏪", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text("Nenhum estoque encontrado", fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Crie um novo ou entre com um código",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { showCreateDialog = true }) {
                                    Text("Criar estoque")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { showJoinDialog = true }) {
                                    Text("Entrar com código")
                                }
                            }
                        }
                    } else {
                        Text(
                            "Selecione um estoque",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(s.condominios) { condominio ->
                                StockCard(
                                    condominio = condominio,
                                    onClick = { vm.selectCondominio(condominio) }
                                )
                            }
                            item {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showJoinDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Entrar em outro estoque com código")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStockDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, address, code ->
                vm.createCondominio(name, address, code)
                showCreateDialog = false
            }
        )
    }

    if (showJoinDialog) {
        JoinStockDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                vm.joinCondominio(code)
                showJoinDialog = false
            }
        )
    }

    if (actionState is StockActionState.Error) {
        LaunchedEffect(actionState) {
            vm.resetActionState()
        }
    }
}

@Composable
fun StockCard(condominio: Condominio, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(condominio.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(condominio.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "Código: ${condominio.code}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CreateStockDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Estoque") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Código de acesso") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, address, code) },
                enabled = name.isNotBlank() && address.isNotBlank() && code.isNotBlank()
            ) { Text("Criar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun JoinStockDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrar em Estoque") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Código de acesso") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = code.isNotBlank()
            ) { Text("Entrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
