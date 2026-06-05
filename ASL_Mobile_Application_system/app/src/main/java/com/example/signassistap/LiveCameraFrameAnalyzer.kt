package com.example.signassistap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@Composable
fun LiveCameraFrameAnalyzer(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    throttleMs: Long = 220L,
    onFrame: (ByteArray) -> Unit,
    onAnalyzerError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Camera permission is required for live detection.", color = Color.Gray)
        }
        return
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val lastSentAt = remember { AtomicLong(0L) }
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnFrame by rememberUpdatedState(onFrame)
    val latestOnAnalyzerError by rememberUpdatedState(onAnalyzerError)

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(480, 360))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                try {
                                    if (!latestEnabled) return@setAnalyzer
                                    val now = System.currentTimeMillis()
                                    if (now - lastSentAt.get() < throttleMs) return@setAnalyzer
                                    val jpeg = imageProxy.toJpegBytes(quality = 45)
                                    lastSentAt.set(now)
                                    latestOnFrame(jpeg)
                                } catch (e: Exception) {
                                    latestOnAnalyzerError(e.message ?: "Camera frame analysis failed")
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                },
                ContextCompat.getMainExecutor(ctx)
            )
            previewView
        }
    )
}

private fun ImageProxy.toJpegBytes(quality: Int): ByteArray {
    val nv21 = yuv420888ToNv21()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val output = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, output)
    return output.toByteArray()
}

private fun ImageProxy.yuv420888ToNv21(): ByteArray {
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]
    val ySize = width * height
    val output = ByteArray(ySize + (width * height / 2))
    copyPlane(yPlane, width, height, output, 0, 1)

    var outputOffset = ySize
    val chromaHeight = height / 2
    val chromaWidth = width / 2
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val vuIndex = row * vPlane.rowStride + col * vPlane.pixelStride
            val uuIndex = row * uPlane.rowStride + col * uPlane.pixelStride
            output[outputOffset++] = vBuffer.get(vuIndex)
            output[outputOffset++] = uBuffer.get(uuIndex)
        }
    }
    return output
}

private fun copyPlane(
    plane: ImageProxy.PlaneProxy,
    width: Int,
    height: Int,
    output: ByteArray,
    offset: Int,
    pixelStride: Int
) {
    val buffer = plane.buffer
    var outputOffset = offset
    for (row in 0 until height) {
        val rowOffset = row * plane.rowStride
        for (col in 0 until width) {
            output[outputOffset] = buffer.get(rowOffset + col * plane.pixelStride)
            outputOffset += pixelStride
        }
    }
}
