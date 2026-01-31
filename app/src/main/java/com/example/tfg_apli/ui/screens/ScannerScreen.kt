package com.example.tfg_apli.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tfg_apli.network.dto.MedicamentoResponseDTO
import com.example.tfg_apli.ui.viewmodels.ScannerState
import com.example.tfg_apli.ui.viewmodels.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(
    isPickMode: Boolean = false,
    onCodePicked: (code: String, name: String) -> Unit = { _, _ -> },
    onNavigateToAddMedication: (MedicamentoResponseDTO, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: ScannerViewModel = viewModel()
    val scannerState by viewModel.scannerState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isNavigating by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNavigating = false
                viewModel.resetState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.setLanguage(Locale("es", "ES"))
            }
        }
        tts = ttsInstance
        onDispose { ttsInstance.stop(); ttsInstance.shutdown() }
    }

    LaunchedEffect(scannerState) {
        val state = scannerState

        if (!isPickMode && state is ScannerState.VerificationResult) {
            tts?.speak(state.message, TextToSpeech.QUEUE_FLUSH, null, "ID_MSG")
        }

        // --- MODO SELECCIÓN
        if (isPickMode && !isNavigating) {
            when (state) {
                is ScannerState.MedicationInfo -> {
                    isNavigating = true
                    // Devolvemos CÓDIGO y NOMBRE
                    onCodePicked(state.barcode, state.medication.nombre)
                    viewModel.resetState()
                }
                is ScannerState.VerificationResult -> {
                    isNavigating = true
                    // Si ya existe en local, usamos su nombre. Código ponemos uno dummy si no lo tenemos a mano.
                    onCodePicked("000000", state.medName)
                    viewModel.resetState()
                }
                else -> {}
            }
        }
        // --- MODO NORMAL ---
        else if (!isPickMode && state is ScannerState.MedicationInfo && !isNavigating) {
            isNavigating = true
            onNavigateToAddMedication(state.medication, state.barcode)
            viewModel.resetState()
        }
    }

    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }
    LaunchedEffect(Unit) { if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            val shouldScan = scannerState is ScannerState.Idle && !isNavigating

            CameraPreview(shouldScan = shouldScan) { code ->
                if (!isNavigating) viewModel.onBarcodeScanned(code)
            }
            ScannerOverlay()

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val state = scannerState) {
                    is ScannerState.Loading -> CircularProgressIndicator(color = Color.White)
                    is ScannerState.Error -> ResultDialog(false, "Error", state.message, false, {}, { viewModel.resetState() })

                    is ScannerState.VerificationResult -> {
                        if (!isPickMode) {
                            ResultDialog(
                                isCorrect = state.isCorrect,
                                title = if (state.isCorrect) "¡CORRECTO!" else "¡ALTO!",
                                message = state.message,
                                showActions = state.showActions,
                                onConfirm = {
                                    viewModel.confirmIntake(context)
                                    Toast.makeText(context, "💊 Toma registrada", Toast.LENGTH_SHORT).show()
                                },
                                onDismiss = {
                                    tts?.stop()
                                    viewModel.resetState()
                                }
                            )
                        }
                    }
                    else -> {
                        Text(
                            if(isPickMode) "Escanea el medicamento..." else "Apunta al código de barras",
                            color = Color.White, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(12.dp)
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Dar Permiso de Cámara") }
            }
        }
    }
}

@Composable
fun ResultDialog(
    isCorrect: Boolean,
    title: String,
    message: String,
    showActions: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFD32F2F)
    val icon = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
            }
        },
        text = {
            Text(
                message,
                color = Color.Black,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            if (showActions) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón CANCELAR
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57373)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Cancelar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Botón TOMAR
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Tomar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable fun ScannerOverlay() { Canvas(modifier = Modifier.fillMaxSize()) { val canvasWidth = size.width; val canvasHeight = size.height; val squareSize = 300.dp.toPx(); with(drawContext.canvas.nativeCanvas) { val checkPoint = saveLayer(null, null); drawRect(Color.Black.copy(alpha = 0.5f)); drawRoundRect(topLeft = Offset((canvasWidth - squareSize) / 2, (canvasHeight - squareSize) / 2), size = Size(squareSize, squareSize), cornerRadius = CornerRadius(30f, 30f), color = Color.Transparent, blendMode = BlendMode.Clear); restoreToCount(checkPoint) }; drawRoundRect(topLeft = Offset((canvasWidth - squareSize) / 2, (canvasHeight - squareSize) / 2), size = Size(squareSize, squareSize), cornerRadius = CornerRadius(30f, 30f), color = Color.White, style = Stroke(width = 4.dp.toPx())) } }

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable fun CameraPreview(shouldScan: Boolean, onBarcodeScanned: (String) -> Unit) { val context = LocalContext.current; val lifecycleOwner = LocalLifecycleOwner.current; val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }; val shouldScanRef = remember { AtomicBoolean(shouldScan) }; SideEffect { shouldScanRef.set(shouldScan) }; val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned); AndroidView(factory = { ctx -> val previewView = PreviewView(ctx).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); scaleType = PreviewView.ScaleType.FILL_CENTER }; cameraProviderFuture.addListener({ val provider = cameraProviderFuture.get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }; val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build(); imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy -> val mediaImage = imageProxy.image; if (mediaImage != null && shouldScanRef.get()) { val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees); BarcodeScanning.getClient().process(image).addOnSuccessListener { barcodes -> for (barcode in barcodes) { barcode.rawValue?.let { if(shouldScanRef.get()) currentOnBarcodeScanned(it) } } }.addOnCompleteListener { imageProxy.close() } } else { imageProxy.close() } }; try { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis) } catch (e: Exception) { Log.e("Camera", "Error", e) } }, ContextCompat.getMainExecutor(ctx)); previewView }, modifier = Modifier.fillMaxSize()) }