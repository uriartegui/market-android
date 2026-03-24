package com.market.android.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.market.android.data.model.Product

private val BgColor = Color(0xFFF4F5F7)
private val NavyColor = Color(0xFF1A1F36)
private val PurpleColor = Color(0xFF6C63FF)
private val GreenColor = Color(0xFF00D97E)
private val RedColor = Color(0xFFE53935)
private val TextSecondary = Color(0xFF8B8FA8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(vm: ProductsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val search by vm.search.collectAsState()
    val actionState by vm.actionState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showStockDialog by remember { mutableStateOf<Product?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is ProductActionState.Success) vm.resetActionState()
    }

    val products = (state as? ProductsState.Success)?.products ?: emptyList()
    val lowStockProducts = products.filter { it.quantity <= 10 }
    val filteredProducts = if (selectedCategory != null)
        products.filter { it.category?.equals(selectedCategory, true) == true }
    else products

    // Stats por categoria
    val categoryStats = products
        .groupBy { it.category?.uppercase() ?: "OUTROS" }
        .map { (cat, items) -> Triple(cat, items.sumOf { it.quantity }, items.size) }
        .sortedByDescending { it.second }
        .take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Header ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Título + busca
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "INVENTÁRIO GERAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Estoque",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyColor
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Busca
                    OutlinedTextField(
                        value = search,
                        onValueChange = { vm.onSearchChange(it) },
                        placeholder = { Text("Buscar por nome ou SKU...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.width(260.dp).height(48.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE8EAF0)
                        )
                    )

                    // Categorias
                    OutlinedButton(
                        onClick = { selectedCategory = null },
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Categorias", fontSize = 13.sp)
                    }

                    // Novo produto
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Novo Produto", fontSize = 13.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Alerta estoque baixo ──────────────────────
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Estoque baixo detectado",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF856404),
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Existem ${lowStockProducts.size} itens operando abaixo do nível mínimo de segurança.",
                                    color = Color(0xFF856404),
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { vm.toggleLowStock() }) {
                                Text(
                                    "Revisar Agora",
                                    color = Color(0xFF856404),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Cards de categorias ───────────────────────
            if (products.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val catColors = listOf(PurpleColor, RedColor, GreenColor)

                        categoryStats.forEachIndexed { index, (cat, qty, _) ->
                            val isSelected = selectedCategory == cat
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedCategory = if (isSelected) null else cat
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, PurpleColor, RoundedCornerShape(12.dp))
                                        else Modifier
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        cat,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = catColors[index],
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "$qty",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyColor
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "un",
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(catColors[index].copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(
                                                    (qty.toFloat() / (categoryStats.maxOfOrNull { it.second }?.toFloat() ?: 1f)).coerceIn(0.05f, 1f)
                                                )
                                                .background(catColors[index])
                                        )
                                    }
                                }
                            }
                        }

                        // Total itens
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "TOTAL ITENS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${products.size}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyColor
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "SKUs",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFDDE1EA))
                                )
                            }
                        }
                    }
                }
            }

            // ── Lista de produtos ─────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Lista de Produtos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.GridView, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        Icon(Icons.Default.List, null, tint = PurpleColor, modifier = Modifier.size(20.dp))
                    }
                }
            }

            when (val s = state) {
                is ProductsState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PurpleColor)
                        }
                    }
                }

                is ProductsState.Error -> {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(s.message, color = RedColor)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { vm.loadProducts() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleColor)
                                ) { Text("Tentar novamente") }
                            }
                        }
                    }
                }

                is ProductsState.Success -> {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📦", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Nenhum produto encontrado", color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        itemsIndexed(filteredProducts) { _, product ->
                            ProductRow(
                                product = product,
                                onEdit = { selectedProduct = product },
                                onDelete = { vm.deleteProduct(product.id) },
                                onAdjustStock = { showStockDialog = product }
                            )
                        }

                        item {
                            Text(
                                "Mostrando ${filteredProducts.size} produto${if (filteredProducts.size != 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Dialogs
    if (showAddDialog) {
        ProductDialog(
            product = null,
            onDismiss = { showAddDialog = false },
            onSave = { vm.createProduct(it); showAddDialog = false }
        )
    }
    selectedProduct?.let { product ->
        ProductDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onSave = { vm.updateProduct(product.id, it); selectedProduct = null }
        )
    }
    showStockDialog?.let { product ->
        AdjustStockDialog(
            product = product,
            onDismiss = { showStockDialog = null },
            onAdjust = { qty, type -> vm.adjustStock(product.id, qty, type); showStockDialog = null }
        )
    }
}

// ── Item da lista de produtos ──────────────────────────────

@Composable
fun ProductRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: () -> Unit
) {
    val isLowStock = product.quantity <= 10
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagem placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    null,
                    tint = Color(0xFFB0B7C3),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Nome + categoria
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyColor
                    )
                    if (isLowStock) {
                        Surface(
                            color = RedColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "BAIXO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedColor
                            )
                        }
                    }
                }
                product.category?.let { cat ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = GreenColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            cat.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenColor
                        )
                    }
                }
            }

            // Preço
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "PREÇO UNITÁRIO",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text(
                    "R$ ${"%.2f".format(product.price)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyColor
                )
            }

            // Estoque
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "ESTOQUE",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text(
                    "${product.quantity} un",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLowStock) RedColor else NavyColor
                )
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ajustar Estoque") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onAdjustStock() }
                    )
                    DropdownMenuItem(
                        text = { Text("Editar Produto") },
                        leadingIcon = { Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Excluir", color = RedColor) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedColor, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

// ── Dialogs (mantidos do original) ────────────────────────

@Composable
fun ProductDialog(product: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Preço*") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qtd*") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Código de barras") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(Product(
                        id = product?.id ?: "",
                        name = name,
                        description = description.ifBlank { null },
                        price = price.toDoubleOrNull() ?: 0.0,
                        quantity = quantity.toIntOrNull() ?: 0,
                        category = category.ifBlank { null },
                        barcode = barcode.ifBlank { null },
                        imageUrl = product?.imageUrl,
                        condominioId = product?.condominioId ?: ""
                    ))
                },
                enabled = name.isNotBlank() && price.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleColor)
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockDialog(product: Product, onDismiss: () -> Unit, onAdjust: (Int, String) -> Unit) {
    var quantity by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("ADD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Estoque") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${product.name}",
                    fontWeight = FontWeight.SemiBold,
                    color = NavyColor
                )
                Text("Estoque atual: ${product.quantity} unidades", fontSize = 13.sp, color = TextSecondary)
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
                enabled = quantity.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleColor)
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
