package com.familystalking.app.presentation.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.familystalking.app.ui.theme.PrimaryGreen
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun FamilyQrScreen(
    navController: NavController,
    userName: String = "Bert",
    userId: String = "Bert"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My QR Code",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = userName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))
        QRCodeBox(data = userId)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Have friends scan this code to add you as a contact",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QRCodeBox(data: String) {
    val size = 200
    val bitmap = generateQrCodeBitmap(data, size, size)
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.size(size.dp)
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "QR Code",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("QR Error", color = Color.Red)
            }
        }
    }
}

fun generateQrCodeBitmap(data: String, width: Int, height: Int): ImageBitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, width, height)
        val imageBitmap = ImageBitmap(width, height)
        val canvas = Canvas(imageBitmap)
        val paint = Paint().apply { color = ComposeColor.Black }
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (bitMatrix[x, y]) {
                    canvas.drawRect(
                        left = x.toFloat(),
                        top = y.toFloat(),
                        right = (x + 1).toFloat(),
                        bottom = (y + 1).toFloat(),
                        paint = paint
                    )
                }
            }
        }
        imageBitmap
    } catch (e: Exception) {
        null
    }
} 