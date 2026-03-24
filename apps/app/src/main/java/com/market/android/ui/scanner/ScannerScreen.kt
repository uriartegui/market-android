package com.market.android.ui.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.market.android.data.model.CartItem
import com.market.android.data.preferences.TokenPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onCheckout: (String) -> Unit,
    onManagerUnlock: (() -> Unit)? = null,   // null = modo gerente (tem nav), não-null = modo kiosk
    vm: ScannerViewModel = viewModel()
) {
    val scannerState by vm.scannerState.collectAsState()
    val cart by vm.cart.collectAsState()
    val checkoutState by vm.checkoutState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val context = LocalContext.current
    val tokenPrefs = remember { TokenPreferences(context) }

    var showManualInput by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var lastAddedName by remember { mutableStateOf<String?>(null) }

    // PIN de desbloqueio (apenas no modo kiosk)
    var statusTapCount by remember { mutableStateOf(0) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutState.Success) {
            onCheckout((checkoutState as CheckoutState.Success).orderId)
        }
    }

    // Auto-adiciona quando produto é encontrado
    LaunchedEffect(scannerState) {
        if (scannerState is ScannerState.ProductFound) {
            val product = (scannerState as ScannerState.ProductFound).product
            lastAddedName = product.name
            vm.addToCart(product)
            delay(2500)
            lastAddedName = null
        }
    }

    // Câmera invisível rodando em segundo plano
    if (cameraPermission.status.isGranted) {
        HiddenBarcodeScanner(
            isScanning = scannerState is ScannerState.Scanning,
            onBarcodeDetected = { vm.onBarcodeDetected(it) }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {

        // ── Área principal: Carrinho ───────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Cesta de Compras",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1F36)
                    )
                    Text(
                        "${vm.cartCount} ${if (vm.cartCount == 1) "item escaneado" else "itens escaneados"} até agora",
                        fontSize = 13.sp,
                        color = Color(0xFF8B8FA8)
                    )
                }
                OutlinedButton(
                    onClick = { showManualInput = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Inserir Manualmente", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Lista ou estado vazio
            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            modifier = Modifier.size(72.dp),
                            tint = Color(0xFFDDE1EA)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Pronto para escanear",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8B8FA8)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Passe o produto no leitor para adicionar",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B7C3)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cart) { item ->
                        CartItemCard(
                            item = item,
                            onIncrease = { vm.increaseQuantity(item.product.id) },
                            onDecrease = { vm.decreaseQuantity(item.product.id) },
                            onRemove = { vm.removeFromCart(item.product.id) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // Notificação de produto adicionado
            AnimatedVisibility(
                visible = lastAddedName != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00D97E))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${lastAddedName} adicionado ao carrinho",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Barra de erro
            if (scannerState is ScannerState.Error) {
                Spacer(Modifier.height(8.dp))
                ErrorBar(
                    message = (scannerState as ScannerState.Error).message,
                    onDismiss = { vm.resumeScanning() }
                )
            }
        }

        // ── Painel direito: Resumo ─────────────────────────
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(Color.White)
                .padding(20.dp)
        ) {
            // Status do scanner (toca 5x para abrir PIN no modo kiosk)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF4F5F7))
                    .then(
                        if (onManagerUnlock != null)
                            Modifier.clickable {
                                statusTapCount++
                                if (statusTapCount >= 5) {
                                    statusTapCount = 0
                                    showPinDialog = true
                                }
                            }
                        else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (scannerState is ScannerState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6C63FF)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D97E))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (scannerState is ScannerState.Loading) "Buscando produto..." else "Scanner Ativo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (scannerState is ScannerState.Loading) Color(0xFF6C63FF) else Color(0xFF1A1F36)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Resumo do pedido
            Text(
                "RESUMO DO PEDIDO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B8FA8),
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF4F5F7))
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Subtotal (${vm.cartCount} itens)",
                    fontSize = 13.sp,
                    color = Color(0xFF8B8FA8)
                )
                Text(
                    "R$ ${"%.2f".format(vm.cartTotal)}",
                    fontSize = 13.sp,
                    color = Color(0xFF1A1F36),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Descontos", fontSize = 13.sp, color = Color(0xFF00D97E))
                Text("R$ 0,00", fontSize = 13.sp, color = Color(0xFF00D97E))
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF4F5F7))
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TOTAL A PAGAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B8FA8),
                    letterSpacing = 0.5.sp
                )
                Text(
                    "R$ ${"%.2f".format(vm.cartTotal)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1F36)
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.checkout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                enabled = cart.isNotEmpty() && checkoutState !is CheckoutState.Loading
            ) {
                if (checkoutState is CheckoutState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "FINALIZAR CHECKOUT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Phone,
                    null,
                    tint = Color(0xFFB0B7C3),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Suporte", fontSize = 12.sp, color = Color(0xFFB0B7C3))
            }
        }
    }

    // Dialog de PIN (modo kiosk)
    if (showPinDialog && onManagerUnlock != null) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
                pinError = false
            },
            title = { Text("Acesso Gerente", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Digite o PIN de 4 dígitos para acessar o painel gerencial.",
                        fontSize = 13.sp,
                        color = Color(0xFF8B8FA8)
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        supportingText = if (pinError) {{ Text("PIN incorreto", color = Color(0xFFE53935)) }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scope = kotlinx.coroutines.MainScope()
                        scope.launch {
                            val savedPin = tokenPrefs.managerPin.first()
                            if (savedPin == null || pinInput == savedPin) {
                                showPinDialog = false
                                pinInput = ""
                                pinError = false
                                onManagerUnlock()
                            } else {
                                pinError = true
                            }
                        }
                    },
                    enabled = pinInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) { Text("Entrar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinInput = ""
                    pinError = false
                }) { Text("Cancelar") }
            }
        )
    }

    // Dialog de inserção manual
    if (showManualInput) {
        AlertDialog(
            onDismissRequest = { showManualInput = false; manualCode = "" },
            title = { Text("Inserir código manualmente") },
            text = {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Código de barras") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (manualCode.isNotBlank()) {
                        vm.onBarcodeDetected(manualCode)
                        manualCode = ""
                        showManualInput = false
                    }
                }) { Text("Buscar") }
            },
            dismissButton = {
                TextButton(onClick = { showManualInput = false; manualCode = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ── Câmera invisível (sem preview, só análise) ─────────────
@Composable
fun HiddenBarcodeScanner(
    isScanning: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isScanningRef = rememberUpdatedState(isScanning)
    val callbackRef = rememberUpdatedState(onBarcodeDetected)

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                if (isScanningRef.value) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        val scanner = BarcodeScanning.getClient()
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val value = barcode.rawValue ?: continue
                                    if (barcode.format == Barcode.FORMAT_EAN_13 ||
                                        barcode.format == Barcode.FORMAT_EAN_8 ||
                                        barcode.format == Barcode.FORMAT_CODE_128 ||
                                        barcode.format == Barcode.FORMAT_UPC_A
                                    ) {
                                        callbackRef.value(value)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else imageProxy.close()
                } else imageProxy.close()
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProvider?.unbindAll()
        }
    }
}

// ── Componentes ────────────────────────────────────────────

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagem placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEEF0F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    null,
                    tint = Color(0xFFB0B7C3),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                item.product.category?.let {
                    Text(
                        it.uppercase(),
                        fontSize = 10.sp,
                        color = Color(0xFF6C63FF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    item.product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1F36)
                )
                item.product.description?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = Color(0xFF8B8FA8),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Controles de quantidade
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F5F7))
                ) {
                    Icon(
                        Icons.Default.Remove,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF1A1F36)
                    )
                }
                Text(
                    "${item.quantity}",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1F36)
                )
                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C63FF))
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "R$ ${"%.2f".format(item.product.price * item.quantity)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1F36)
                )
                if (item.quantity > 1) {
                    Text(
                        "R$ ${"%.2f".format(item.product.price)} un.",
                        fontSize = 11.sp,
                        color = Color(0xFF8B8FA8)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = Color(0xFFB0B7C3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorBar(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEEE))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, color = Color(0xFFE53935), modifier = Modifier.weight(1f), fontSize = 14.sp)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
