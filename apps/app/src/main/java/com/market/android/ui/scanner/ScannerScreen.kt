package com.market.android.ui.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.market.android.data.model.CartItem
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onCheckout: (String) -> Unit,
    vm: ScannerViewModel = viewModel()
) {
    val scannerState by vm.scannerState.collectAsState()
    val cart by vm.cart.collectAsState()
    val checkoutState by vm.checkoutState.collectAsState()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutState.Success) {
            onCheckout((checkoutState as CheckoutState.Success).orderId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            Text(
                text = "🛒 Market Kiosk",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            if (cart.isNotEmpty()) {
                Text(
                    text = "${vm.cartCount} itens • R$ ${"%.2f".format(vm.cartTotal)}",
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            TextButton(
                onClick = { vm.logout() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("Sair", color = Color.White.copy(alpha = 0.7f))
            }
        }

        // Camera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (cameraPermission.status.isGranted) {
                CameraPreview(
                    isScanning = scannerState is ScannerState.Scanning,
                    onBarcodeDetected = { vm.onBarcodeDetected(it) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Permissão de câmera necessária")
                }
            }

            // Overlay de estado
            when (val state = scannerState) {
                is ScannerState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is ScannerState.ProductFound -> {
                    ProductFoundOverlay(
                        product = state.product,
                        onAdd = { vm.addToCart(state.product) },
                        onCancel = { vm.resumeScanning() }
                    )
                }
                is ScannerState.Error -> {
                    ErrorOverlay(
                        message = state.message,
                        onDismiss = { vm.resumeScanning() }
                    )
                }
                else -> {}
            }
        }

        // Carrinho
        if (cart.isNotEmpty()) {
            CartSection(
                cart = cart,
                total = vm.cartTotal,
                isLoading = checkoutState is CheckoutState.Loading,
                onRemove = { vm.removeFromCart(it) },
                onCheckout = { vm.checkout() }
            )
        }
    }
}

@Composable
fun CameraPreview(isScanning: Boolean, onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastScanned by remember { mutableStateOf("") }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    if (isScanning) {
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
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
                                            if (value != lastScanned) {
                                                lastScanned = value
                                                onBarcodeDetected(value)
                                            }
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ProductFoundOverlay(
    product: com.market.android.data.model.Product,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(product.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                product.description?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "R$ ${"%.2f".format(product.price)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
                        Text("Adicionar")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorOverlay(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.padding(32.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❌", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(message, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss) { Text("Tentar novamente") }
            }
        }
    }
}

@Composable
fun CartSection(
    cart: List<CartItem>,
    total: Double,
    isLoading: Boolean,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Carrinho", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(cart) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${item.quantity}x ${item.product.name}", modifier = Modifier.weight(1f))
                    Text("R$ ${"%.2f".format(item.product.price * item.quantity)}")
                    IconButton(onClick = { onRemove(item.product.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover")
                    }
                }
            }
        }
        Divider()
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total: R$ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(
                onClick = onCheckout,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Pagar com Pix")
                }
            }
        }
    }
}
