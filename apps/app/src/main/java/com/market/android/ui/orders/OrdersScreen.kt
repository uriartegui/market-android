package com.market.android.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.market.android.data.model.OrderDetail
import com.market.android.data.model.OrderStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(vm: OrdersViewModel = viewModel()) {
    val ordersState by vm.ordersState.collectAsState()
    val statsState by vm.statsState.collectAsState()
    val selectedStatus by vm.selectedStatus.collectAsState()

    val statusFilters = listOf(null to "Todos", "PAGO" to "Pagos", "PENDENTE" to "Pendentes", "CANCELADO" to "Cancelados")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pedidos") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stats
            item {
                Spacer(Modifier.height(8.dp))
                when (val s = statsState) {
                    is StatsState.Success -> StatsCard(s.stats)
                    is StatsState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else -> {}
                }
            }

            // Filtros
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusFilters) { (value, label) ->
                        FilterChip(
                            selected = selectedStatus == value,
                            onClick = { vm.filterByStatus(value) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Lista de pedidos
            when (val s = ordersState) {
                is OrdersState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                is OrdersState.Error -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.loadOrders() }) { Text("Tentar novamente") }
                        }
                    }
                }
                is OrdersState.Success -> {
                    if (s.orders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🧾", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Nenhum pedido encontrado")
                                }
                            }
                        }
                    } else {
                        items(s.orders) { order -> OrderCard(order) }

                        if (s.totalPages > 1) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { vm.loadOrders(s.currentPage - 1) },
                                        enabled = s.currentPage > 1
                                    ) { Text("← Anterior") }
                                    Text("${s.currentPage} / ${s.totalPages}")
                                    TextButton(
                                        onClick = { vm.loadOrders(s.currentPage + 1) },
                                        enabled = s.currentPage < s.totalPages
                                    ) { Text("Próximo →") }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun StatsCard(stats: OrderStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resumo de Vendas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem("Hoje", stats.today.total, stats.today.count)
                StatItem("Semana", stats.week.total, stats.week.count)
                StatItem("Mês", stats.month.total, stats.month.count)
            }
            if (stats.pendingOrders > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "⏳ ${stats.pendingOrders} pedido(s) pendente(s)",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, total: Double, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "R$ ${"%.2f".format(total)}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text("$count vendas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OrderCard(order: OrderDetail) {
    val statusColor = when (order.status) {
        "PAGO" -> MaterialTheme.colorScheme.primaryContainer
        "PENDENTE" -> MaterialTheme.colorScheme.tertiaryContainer
        "CANCELADO" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusText = when (order.status) {
        "PAGO" -> "✅ Pago"
        "PENDENTE" -> "⏳ Pendente"
        "CANCELADO" -> "❌ Cancelado"
        else -> order.status
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#${order.id.takeLast(8).uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Surface(color = statusColor, shape = MaterialTheme.shapes.small) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            order.items.forEach { item ->
                Text(
                    "• ${item.quantity}x ${item.product?.name ?: "Produto"} — R$ ${"%.2f".format(item.price * item.quantity)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    order.createdAt.take(10),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Total: R$ ${"%.2f".format(order.total)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
