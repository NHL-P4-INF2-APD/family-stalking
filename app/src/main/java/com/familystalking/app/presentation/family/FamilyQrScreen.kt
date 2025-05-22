package com.familystalking.app.presentation.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun FamilyQrScreen(
    navController: NavController,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentUser = state.currentUser
    val currentUserId = state.currentUserId
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Back button at the top
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp)) // Add space for the back button
            Text(
                text = "My QR Code",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = currentUser?.name ?: "...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Create QR code data with both user ID and name
            val qrData = if (currentUserId != null && currentUser != null) {
                "$currentUserId|${currentUser.name}"
            } else {
                "..."
            }
            
            QRCodeBox(data = qrData)
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Have friends scan this code to add you as a contact",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            com.familystalking.app.presentation.navigation.bottomNavBar(
                currentRoute = com.familystalking.app.presentation.navigation.Screen.Family.route,
                navController = navController
            )
        }
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