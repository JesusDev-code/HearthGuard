package com.example.tfg_apli.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.ui.viewmodels.CaregiverViewModel
import com.example.tfg_apli.util.SessionManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun SimpleQrScannerScreen(
    onBack: () -> Unit,
    viewModel: CaregiverViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // --- GESTIÓN DE PERMISOS ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }


    var isProcessing by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Escanea el QR del paciente") }
    var messageColor by remember { mutableStateOf(Color.White) }

    val cuidadorId = SessionManager.currentUser?.id

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (hasCameraPermission) {
            // 1. CÁMARA (CameraX)
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isProcessing) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                val scanner = BarcodeScanning.getClient()

                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrEmpty() && !isProcessing) {
                                                isProcessing = true

                                                val seniorId = rawValue.toLongOrNull()

                                                if (seniorId != null && cuidadorId != null) {
                                                    scanMessage = "Vinculando paciente..."
                                                    messageColor = Color.Yellow

                                                    viewModel.vincularPaciente(
                                                        cuidadorId = cuidadorId,
                                                        seniorId = seniorId,
                                                        onSuccess = {
                                                            val mainHandler = androidx.core.os.HandlerCompat.createAsync(android.os.Looper.getMainLooper())
                                                            mainHandler.post {
                                                                Toast.makeText(context, "Paciente vinculado", Toast.LENGTH_LONG).show()
                                                                onBack()
                                                            }
                                                        },
                                                        onError = { error ->
                                                            val mainHandler = androidx.core.os.HandlerCompat.createAsync(android.os.Looper.getMainLooper())
                                                            mainHandler.post {
                                                                scanMessage = "Error: $error"
                                                                messageColor = Color.Red
                                                            }
                                                            Thread.sleep(3000)
                                                            isProcessing = false
                                                            val resetHandler = androidx.core.os.HandlerCompat.createAsync(android.os.Looper.getMainLooper())
                                                            resetHandler.post {
                                                                scanMessage = "Escanea el QR del paciente"
                                                                messageColor = Color.White
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    isProcessing = false
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
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
                            Log.e("Camera", "Fallo al vincular cámara", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- 2. GUÍA VISUAL
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(250.dp)) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerLength = 40.dp.toPx()
                    val color = Color.White.copy(alpha = 0.8f)

                    // Esquina Superior Izquierda
                    drawLine(color, start = Offset(0f, 0f), end = Offset(cornerLength, 0f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    drawLine(color, start = Offset(0f, 0f), end = Offset(0f, cornerLength), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                    // Esquina Superior Derecha
                    drawLine(color, start = Offset(size.width, 0f), end = Offset(size.width - cornerLength, 0f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    drawLine(color, start = Offset(size.width, 0f), end = Offset(size.width, cornerLength), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                    // Esquina Inferior Izquierda
                    drawLine(color, start = Offset(0f, size.height), end = Offset(cornerLength, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    drawLine(color, start = Offset(0f, size.height), end = Offset(0f, size.height - cornerLength), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                    // Esquina Inferior Derecha
                    drawLine(color, start = Offset(size.width, size.height), end = Offset(size.width - cornerLength, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    drawLine(color, start = Offset(size.width, size.height), end = Offset(size.width, size.height - cornerLength), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                }
            }


        } else {
            // UI Sin Permiso
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Se requiere permiso de cámara", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Dar permiso")
                }
            }
        }

        // 3. INTERFAZ SUPERIOR (Botón volver)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp)
                .align(Alignment.TopStart)
        ) {
            FloatingActionButton(
                onClick = onBack,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }

        // 4. TEXTO DE ESTADO (ABAJO)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp) // Subimos un poco para que no choque
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = scanMessage,
                color = messageColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isProcessing && scanMessage.contains("Vinculando")) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Yellow
            )
        }
    }
}