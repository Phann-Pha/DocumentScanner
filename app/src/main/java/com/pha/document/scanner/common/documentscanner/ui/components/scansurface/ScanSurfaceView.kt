package com.pha.document.scanner.common.documentscanner.ui.components.scansurface

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.yuvToRgba
import com.pha.document.scanner.common.documentscanner.common.utils.ImageDetectionProperties
import com.pha.document.scanner.common.documentscanner.common.utils.OpenCvNativeBridge
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel.ErrorMessage
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class ScanSurfaceView : FrameLayout
{
    companion object
    {
        private const val TIME_POST_PICTURE = 1500L
        private const val DEFAULT_TIME_POST_PICTURE = 1500L
        private const val IMAGE_ANALYSIS_SCALE_WIDTH = 400
    }
    
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    
    lateinit var lifecycleOwner: LifecycleOwner
    lateinit var listener: ScanSurfaceListener
    lateinit var originalImageFile: File
    
    private val nativeClass = OpenCvNativeBridge()
    private var autoCaptureTimer: CountDownTimer? = null
    private var millisLeft = 0L
    private var isAutoCaptureScheduled = false
    private var isCapturing = false
    
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewSize: android.util.Size
    
    var isAutoCaptureOn: Boolean = true
    private var isFlashEnabled: Boolean = false
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    
    init
    {
        LayoutInflater.from(context).inflate(R.layout.scan_surface_view, this, true)
    }
    
    fun start(view: View)
    {
        val viewFinder = view.findViewById<PreviewView>(R.id.viewFinder)
        val textInstruction = view.findViewById<TextView>(R.id.textInstruction)
        
        viewFinder.post {
            viewFinder.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            previewSize = android.util.Size(viewFinder.width, viewFinder.height)
            openCamera(viewFinder, textInstruction)
        }
    }
    
    private fun openCamera(viewFinder: PreviewView, textInstruction: TextView)
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            try
            {
                bindCamera(viewFinder, textInstruction)
                checkIfFlashIsPresent()
            }
            catch (e: Exception)
            {
                listener.onError(ErrorScannerModel(ErrorMessage.CAMERA_USE_CASE_BINDING_FAILED, e))
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun bindCamera(viewFinder: PreviewView, textInstruction: TextView)
    {
        cameraProvider?.unbindAll()
        camera = null
        setUseCases(viewFinder, textInstruction)
    }
    
    private fun setImageCapture()
    {
        if (imageCapture != null && cameraProvider?.isBound(imageCapture!!) == true)
        {
            cameraProvider?.unbind(imageCapture)
        }
        
        imageCapture = null
        imageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()
    }
    
    fun unbindCamera()
    {
        cameraProvider?.unbind(imageAnalysis)
    }
    
    @SuppressLint("SetTextI18n")
    private fun setUseCases(viewFinder: PreviewView, textInstruction: TextView)
    {
        preview = Preview.Builder()
                .setTargetResolution(previewSize)
                .build()
                .also { it.surfaceProvider = viewFinder.surfaceProvider }
        
        setImageCapture()
        
        val aspectRatio: Float = previewSize.width / previewSize.height.toFloat()
        val width = IMAGE_ANALYSIS_SCALE_WIDTH
        val height = (width / aspectRatio).roundToInt()
        
        imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(width, height))
                .build()
        
        imageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
            if (isAutoCaptureOn)
            {
                try
                {
                    val mat = image.yuvToRgba()
                    val originalPreviewSize = mat.size()
                    val largestQuad = nativeClass.detectLargestQuadrilateral(mat)
                    mat.release()
                    if (null != largestQuad)
                    {
                        drawLargestRect(largestQuad.contour, largestQuad.points, originalPreviewSize, textInstruction)
                    }
                    else
                    {
                        textInstruction.isVisible = true
                        textInstruction.text = "Scan Document"
                    }
                }
                catch (e: Exception)
                {
                    textInstruction.isVisible = false
                    textInstruction.text = "Cannot Detect Document"
                    listener.onError(ErrorScannerModel(ErrorMessage.DETECT_LARGEST_QUADRILATERAL_FAILED, e))
                }
            }
            else
            {
                textInstruction.isVisible = true
                textInstruction.text = "Auto Capture Disabled"
            }
            image.close()
        }
        
        camera = cameraProvider!!.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis, imageCapture)
    }
    
    private fun drawLargestRect(approx: MatOfPoint2f, points: Array<Point>, stdSize: Size, textInstruction: TextView)
    {
        // Attention: axis are swapped
        val previewWidth = stdSize.height.toFloat()
        val previewHeight = stdSize.width.toFloat()
        
        val resultWidth = max(previewWidth - points[0].y.toFloat(), previewWidth - points[1].y.toFloat()) - min(previewWidth - points[2].y.toFloat(), previewWidth - points[3].y.toFloat())
        
        val resultHeight = max(points[1].x.toFloat(), points[2].x.toFloat()) - min(points[0].x.toFloat(), points[3].x.toFloat())
        
        val imgDetectionPropsObj = ImageDetectionProperties(previewWidth.toDouble(), previewHeight.toDouble(), points[0], points[1], points[2], points[3], resultWidth.toInt(), resultHeight.toInt())
        if (imgDetectionPropsObj.isNotValidImage(approx))
        {
            textInstruction.isVisible = false
            cancelAutoCapture()
        }
        else
        {
            if (!isAutoCaptureScheduled)
            {
                scheduleAutoCapture(textInstruction)
            }
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun scheduleAutoCapture(textInstruction: TextView)
    {
        textInstruction.isVisible = true
        textInstruction.text = "Processing..."
        isAutoCaptureScheduled = true
        millisLeft = 0L
        autoCaptureTimer = object : CountDownTimer(DEFAULT_TIME_POST_PICTURE, 100)
        {
            override fun onTick(millisUntilFinished: Long)
            {
                if (millisUntilFinished != millisLeft)
                {
                    millisLeft = millisUntilFinished
                }
            }
            
            override fun onFinish()
            {
                isAutoCaptureScheduled = false
                autoCapture()
            }
        }
        autoCaptureTimer?.start()
    }
    
    private fun autoCapture()
    {
        if (isCapturing) return
        cancelAutoCapture()
        onTakePicture()
    }
    
    fun onTakePicture()
    {
        listener.scanSurfaceShowProgress()
        isCapturing = true
        
        val imageCapture = imageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(originalImageFile).build()
        
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback
            {
                override fun onError(e: ImageCaptureException)
                {
                    listener.scanSurfaceHideProgress()
                    listener.onError(ErrorScannerModel(ErrorMessage.PHOTO_CAPTURE_FAILED, e))
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults)
                {
                    listener.scanSurfaceHideProgress()
                    
                    unbindCamera()
                    listener.scanSurfacePictureTaken()
                    postDelayed({ isCapturing = false }, TIME_POST_PICTURE)
                }
            })
    }
    
    private fun checkIfFlashIsPresent()
    {
        if (camera?.cameraInfo?.hasFlashUnit() == true)
        {
            listener.showFlash()
        }
        else
        {
            listener.hideFlash()
        }
    }
    
    private fun cancelAutoCapture()
    {
        if (isAutoCaptureScheduled)
        {
            isAutoCaptureScheduled = false
            autoCaptureTimer?.cancel()
        }
    }
    
    fun switchFlashState()
    {
        isFlashEnabled = !isFlashEnabled
        flashMode = if (isFlashEnabled)
        {
            listener.showFlashModeOn()
            ImageCapture.FLASH_MODE_ON
        }
        else
        {
            listener.showFlashModeOff()
            ImageCapture.FLASH_MODE_OFF
        }
        setImageCapture()
        camera = cameraProvider!!.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)
    }
}