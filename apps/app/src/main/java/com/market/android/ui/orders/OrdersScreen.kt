package com.market.android.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.market.android.data.model.OrderDetail
import com.market.android.data.model.OrderStats

private val Blue700   = Color(0xFF1565C0)
private val GreenBadge  = Color(0xFF00897B)
private val OrangeBadge = Color(0xFFF57C00)
private val RedBadge    = Color(0xFFD32F2F)
private val GreenBg     = Color(0xFFE0F2F1)
private val OrangeBg    = Color(0xFFFFF3E0)
private val RedBg       = Color(0xFFFFEBEE)
private val StatBlue    = Color(0xFF1976D2)
private val StatGreen   = Color(0xFF2E7D32)
private val StatOrange  = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(vm: OrdersViewModel = viewModel()) {
    val ordersState   by vm.ordersState.collectAsState()
    val statsState    by vm.statsState.collectAsState()
    val selectedStatus by vm.selectedStatus.collectAsState()

    val tabs = listOf(null to "Todos", "PAGO" to "Pagos",
        "PENDENTE" to "Pendentes", "CANCELADO" to "Cancelados")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumo de Vendas", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificações")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda")
                    }
                    OutlinedButton(
                        onClick = { vm.loadStats(); vm.loadOrders() },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Sync, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync Data", fontSize = 13.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Stats Cards ─────────────────────────────────────
            item {
                when (val s = statsState) {
                    is StatsState.Success -> StatsRow(s.stats)
                    is StatsState.Loading -> LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    else -> {}
                }
            }

            // ── Tab Filters ─────────────────────────────────────
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { (value, label) ->
                            val selected = selectedStatus == value
                            Box(
                                modifier = Modifier
                                    .clickable { vm.filterByStatus(value) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        label,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Blue700
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (selected) {
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .height(2.dp)
                                                .width(40.dp)
                                                .background(Blue700, RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {},
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.FilterList, null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Filtrar por Data", fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider()
                }
            }

            // ── Order List ───────────────────────────────────────
            when (val s = ordersState) {
                is OrdersState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                is OrdersState.Error -> {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp)
                        ) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { vm.loadOrders() }) { Text("Tentar novamente") }
                        }
                    }
                }
                is OrdersState.Success -> {
                    if (s.orders.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(64.dp)
                            ) {
                                Text("🧾", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Nenhum pedido encontrado",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(s.orders) { order ->
                            OrderRow(order)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        if (s.totalPages > 1) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { vm.loadOrders(s.currentPage - 1) },
                                        enabled = s.currentPage > 1
                                    ) { Text("← Anterior") }
                                    Text(
                                        "${s.currentPage} / ${s.totalPages}",
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        fontSize = 13.sp
                                    )
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

// ── Stats Row ──────────────────────────────────────────────────────────────

@Composable
fun StatsRow(stats: OrderStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "HOJE",
            icon = Icons.Default.CalendarToday,
            iconTint = StatBlue,
            value = "R$ ${"%.2f".format(stats.today.total).replace('.', ',')}",
            subtitle = "${stats.today.count} Pedidos concluídos"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "SEMANA",
            icon = Icons.Default.BarChart,
            iconTint = StatGreen,
            value = "R$ ${"%.2f".format(stats.week.total).replace('.', ',')}",
            subtitle = "${stats.week.count} Pedidos este período"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "MÊS",
            icon = Icons.Default.DateRange,
            iconTint = StatOrange,
            value = "R$ ${"%.2f".format(stats.month.total).replace('.', ',')}",
            subtitle = "${stats.month.count} Pedidos registrados"
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 14.sp
            )
        }
    }
}

// ── Order Row ──────────────────────────────────────────────────────────────

@Composable
fun OrderRow(order: OrderDetail) {
    val isCancelled = order.status == "CANCELADO"
    val textAlpha = if (isCancelled) 0.45f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ORDER ID
        Column(modifier = Modifier.width(90.dp)) {
            Text(
                "ORDER ID",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "#${order.id.takeLast(8).uppercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isCancelled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else Blue700,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
        }

        // ITENS
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "ITENS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(3.dp))
            val itemsText = order.items.joinToString(", ") {
                "${it.quantity}x ${it.product?.name ?: "Produto"}"
            }
            Text(
                itemsText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
        }

        Spacer(Modifier.width(12.dp))

        // DATA E HORA
        Column(modifier = Modifier.width(110.dp)) {
            Text(
                "DATA E HORA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                formatDateTime(order.createdAt),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Status badge
        StatusBadge(order.status)

        Spacer(Modifier.width(8.dp))

        // TOTAL
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(68.dp)) {
            Text(
                "TOTAL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "R$ ${"%.2f".format(order.total).replace('.', ',')}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
        }

        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status) {
        "PAGO"      -> Triple(GreenBg, GreenBadge, "PAGO")
        "PENDENTE"  -> Triple(OrangeBg, OrangeBadge, "PENDENTE")
        "CANCELADO" -> Triple(RedBg, RedBadge, "CANCELADO")
        else        -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), status)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

fun formatDateTime(iso: String): String {
    return try {
        // "2026-03-23T14:30:00.000Z" → "23/03/2026 - 14:30"
        val date = iso.take(10).split("-")
        val time = if (iso.length >= 16) iso.substring(11, 16) else ""
        "${date[2]}/${date[1]}/${date[0]} - $time"
    } catch (e: Exception) {
        iso.take(16)
    }
}
