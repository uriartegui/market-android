package com.market.android.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.market.android.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(vm: ProductsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val search by vm.search.collectAsState()
    val showLowStock by vm.showLowStock.collectAsState()
    val actionState by vm.actionState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showStockDialog by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is ProductActionState.Success) vm.resetActionState()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Estoque") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Novo produto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Barra de busca
            OutlinedTextField(
                value = search,
                onValueChange = { vm.onSearchChange(it) },
                label = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Filtro estoque baixo
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = showLowStock,
                    onClick = { vm.toggleLowStock() },
                    label = { Text("⚠️ Estoque baixo") }
                )
            }

            Spacer(Modifier.height(8.dp))

            when (val s = state) {
                is ProductsState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProductsState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.loadProducts() }) { Text("Tentar novamente") }
                        }
                    }
                }
                is ProductsState.Success -> {
                    if (s.products.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📦", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Nenhum produto encontrado")
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(s.products) { product ->
                                ProductCard(
                                    product = product,
                                    onEdit = { selectedProduct = product },
                                    onDelete = { vm.deleteProduct(product.id) },
                                    onAdjustStock = { showStockDialog = product }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductDialog(
            product = null,
            onDismiss = { showAddDialog = false },
            onSave = { product ->
                vm.createProduct(product)
                showAddDialog = false
            }
        )
    }

    selectedProduct?.let { product ->
        ProductDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onSave = { updated ->
                vm.updateProduct(product.id, updated)
                selectedProduct = null
            }
        )
    }

    showStockDialog?.let { product ->
        AdjustStockDialog(
            product = product,
            onDismiss = { showStockDialog = null },
            onAdjust = { quantity, type ->
                vm.adjustStock(product.id, quantity, type)
                showStockDialog = null
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: () -> Unit
) {
    val isLowStock = product.quantity <= 5

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    product.category?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "R$ ${"%.2f".format(product.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = if (isLowStock)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${product.quantity} un",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (isLowStock)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isLowStock) {
                        Text(
                            "⚠️ Estoque baixo",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            product.barcode?.let {
                Text(
                    "Barcode: $it",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAdjustStock,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Estoque", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Excluir", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.quantity?.toString() ?: "0") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Novo Produto" else "Editar Produto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nome*") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        label = { Text("Preço*") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantity, onValueChange = { quantity = it },
                        label = { Text("Qtd*") }, modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = barcode, onValueChange = { barcode = it },
                    label = { Text("Código de barras") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Product(
                            id = product?.id ?: "",
                            name = name,
                            description = description.ifBlank { null },
                            price = price.toDoubleOrNull() ?: 0.0,
                            quantity = quantity.toIntOrNull() ?: 0,
                            category = category.ifBlank { null },
                            barcode = barcode.ifBlank { null },
                            imageUrl = product?.imageUrl,
                            condominioId = product?.condominioId ?: ""
                        )
                    )
                },
                enabled = name.isNotBlank() && price.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AdjustStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onAdjust: (Int, String) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("ADD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Estoque — ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estoque atual: ${product.quantity} unidades")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ADD" to "Adicionar", "REMOVE" to "Remover", "SET" to "Definir").forEach { (value, label) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantidade") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdjust(quantity.toIntOrNull() ?: 0, type) },
                enabled = quantity.isNotBlank()
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
