package com.market.android.ui.payment

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PaymentScreen(
    orderId: String,
    onPaymentResult: (Boolean) -> Unit,
    vm: PaymentViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(orderId) {
        vm.generatePix(orderId)
    }

    LaunchedEffect(state) {
        when (state) {
            is PaymentState.Approved -> onPaymentResult(true)
            is PaymentState.Rejected -> onPaymentResult(false)
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val s = state) {
            is PaymentState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Gerando QR Code Pix...")
            }

            is PaymentState.WaitingPayment -> {
                Text(
                    "Pague com Pix",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "R$ ${"%.2f".format(s.payment.amount)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))

                s.payment.pixQrCodeBase64?.let { base64 ->
                    val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code Pix",
                            modifier = Modifier.size(280.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Aguardando pagamento...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
                s.payment.pixQrCode?.let { code ->
                    Text(
                        "Ou copie o código:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = code,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            is PaymentState.Error -> {
                Text("❌", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(s.message, fontSize = 16.sp)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { vm.generatePix(orderId) }) {
                    Text("Tentar novamente")
                }
            }

            else -> {}
        }
    }
}
