package com.domain.document.scanner.documentscanner.core.manager

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.View
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.domain.document.scanner.documentscanner.core.config.BitmapUtils
import com.domain.document.scanner.documentscanner.core.config.DocumentScannerConfig
import com.domain.document.scanner.documentscanner.core.config.FileUtils
import com.domain.document.scanner.documentscanner.core.config.ScanResultHandler
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import java.io.File

class DocumentScannerSession internal constructor(
    private val activity: Activity,
    private val lifecycle: LifecycleOwner,
    private val rectangle: View? = null,
    private val viewFinder: PreviewView,
    private val config: DocumentScannerConfig
) {

    // Camera components
    private lateinit var processCameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture

    fun start() {
        processCameraProviderFuture = ProcessCameraProvider.getInstance(context = activity)
        processCameraProviderFuture.addListener({
            processCameraProvider = processCameraProviderFuture.get()
            viewFinder.post { setupCamera() }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun stop() {
        processCameraProvider.unbindAll()
    }

    private fun setupCamera() {
        processCameraProvider.unbindAll()
        processCameraProvider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, buildPreviewUseCase(), buildImageCaptureUseCase())
    }

    private fun buildPreviewUseCase(): Preview {
        val display = viewFinder.display
        val displaySize = getDisplaySize()

        val preview = Preview.Builder()
            .setTargetRotation(display.rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(displaySize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .build()
            )
            .build()

        preview.surfaceProvider = viewFinder.surfaceProvider
        return preview
    }

    private fun getDisplaySize(): Size {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.getSystemService(WindowManager::class.java).currentWindowMetrics
            val bounds = windowMetrics.bounds
            Size(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val display = activity.windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getMetrics(metrics)
            Size(metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun buildImageCaptureUseCase(): ImageCapture {
        val display = viewFinder.display
        val displaySize = getDisplaySize()

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(display.rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(displaySize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .build()
            )
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        return imageCapture
    }

    fun capture(page: Int, result: ScanResultHandler) {
        val format = config.format.name.lowercase()
        val child = "doc-image$page.$format"

        val file = config.directory ?: File(activity.externalCacheDirs[0], child)
        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(output, ContextCompat.getMainExecutor(activity), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val uri = output.savedUri
                    val dir = File(output.savedUri?.path.orEmpty())
                    val bitmap = BitmapUtils.getBitmapFromContentUri(activity.contentResolver, uri)

                    val cropped = bitmap?.let { cropImage(bitmap = it, frame = viewFinder, reference = rectangle) }
                    cropped?.let { bmp ->
                        val file = FileUtils.toFile(activity = activity, bitmap = bmp, child = child, quality = config.resolution)
                        if (file != null) {
                            result.onPageCaptured(file = file)
                        } else {
                            result.onPageCaptured(file = dir)
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(e: ImageCaptureException) {
                e.printStackTrace()
            }
        })
    }

    private fun cropImage(bitmap: Bitmap, frame: View, reference: View?, horizontalPaddingPercent: Float = 0.15f): Bitmap {
        try {

            if (reference == null) return bitmap

            // Original frame dimensions
            val originalHeight = frame.height
            val originalWidth = frame.width

            // Reference rectangle dimensions and position
            val referenceHeight = reference.height
            val referenceWidth = reference.width
            val referenceLeft = reference.left
            val referenceTop = reference.top

            // Actual bitmap dimensions
            val bitmapHeight = bitmap.height
            val bitmapWidth = bitmap.width

            // Validate inputs
            if (originalWidth <= 0 || originalHeight <= 0 ||
                bitmapWidth <= 0 || bitmapHeight <= 0 ||
                referenceWidth <= 0 || referenceHeight <= 0 ||
                horizontalPaddingPercent < 0 || horizontalPaddingPercent > 0.4f
            ) {
                return bitmap // Return original if invalid
            }

            // Calculate scaling factors with proper precision
            val widthScale = bitmapWidth.toFloat() / originalWidth.toFloat()
            val heightScale = bitmapHeight.toFloat() / originalHeight.toFloat()

            // Calculate base crop dimensions and position with float precision
            val baseLeft = referenceLeft * widthScale
            val baseTop = referenceTop * heightScale
            val baseWidth = referenceWidth * widthScale
            val baseHeight = referenceHeight * heightScale

            // Calculate horizontal padding in bitmap pixels (shrinks the crop area)
            val horizontalPaddingPixels = baseWidth * horizontalPaddingPercent

            // Apply horizontal padding (shrink crop area inward)
            val finalLeft = (baseLeft + horizontalPaddingPixels).toInt()
            val finalTop = baseTop.toInt()
            val finalWidth = (baseWidth - (2 * horizontalPaddingPixels)).toInt()
            val finalHeight = baseHeight.toInt()

            // Bounds checking to prevent crashes and ensure we stay within bitmap boundaries
            val safeLeft = maxOf(0, finalLeft)
            val safeTop = maxOf(0, finalTop)
            val safeRight = minOf(bitmapWidth, finalLeft + finalWidth)
            val safeBottom = minOf(bitmapHeight, finalTop + finalHeight)

            val safeWidth = safeRight - safeLeft
            val safeHeight = safeBottom - safeTop

            // Validate safe dimensions
            if (safeWidth <= 0 || safeHeight <= 0) {
                return bitmap
            }

            // Create cropped bitmap with safe bounds and padding
            val croppedBitmap = Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)

            // Compress to JPEG format with maximum quality
            val stream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            return croppedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return bitmap // Return original bitmap on error
        }
    }
}