package com.pha.document.scanner.common.document_scanner

import android.app.Activity
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View.MeasureSpec
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
import androidx.lifecycle.LifecycleOwner
import com.pha.document.scanner.common.document_scanner.listener.DocumentReaderListener
import com.pha.document.scanner.common.document_scanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.common.extensions.yuvToRgba
import com.pha.document.scanner.common.documentscanner.common.utils.ImageDetectionProperties
import com.pha.document.scanner.common.documentscanner.common.utils.OpenCvNativeBridge
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class BaseDocumentReader(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val originalImageFile: File,
    private val viewFinder: PreviewView,
    private val listener: DocumentReaderListener
)
{
    companion object
    {
        private const val TIME_POST_PICTURE = 1500L
        private const val DEFAULT_TIME_POST_PICTURE = 1500L
        private const val IMAGE_ANALYSIS_SCALE_WIDTH = 400
    }
    
    private lateinit var _mPreviewSize: android.util.Size
    
    private var _mPreview: Preview? = null
    private var _mCamera: Camera? = null
    private var _mCameraProvider: ProcessCameraProvider? = null
    
    private var _mImageAnalysis: ImageAnalysis? = null
    private var _mImageCapture: ImageCapture? = null
    
    private val nativeClass = OpenCvNativeBridge()
    private var isCapturing = false
    private var isAutoCaptureOn: Boolean = true
    private var isAutoCaptureScheduled = false
    
    private var autoCaptureTimer: CountDownTimer? = null
    private var millisLeft = 0L
    
    private var isFlashEnabled: Boolean = false
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    
    fun startCamera()
    {
        viewFinder.post {
            viewFinder.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            _mPreviewSize = android.util.Size(viewFinder.width, viewFinder.height)
            openCamera()
        }
    }
    
    private fun openCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            _mCameraProvider = cameraProviderFuture.get()
            try
            {
                bindCamera()
                onCheckIfFlashIsPresent()
            }
            catch (e: Exception)
            {
                listener.onFailed(ErrorScannerModel(ErrorScannerModel.ErrorMessage.CAMERA_USE_CASE_BINDING_FAILED, e))
            }
        }, ContextCompat.getMainExecutor(activity))
    }
    
    private fun bindCamera()
    {
        _mCameraProvider?.unbindAll()
        _mCamera = null
        setUseCases()
    }
    
    private fun setUseCases()
    {
        _mPreview = Preview.Builder()
                .setTargetResolution(_mPreviewSize)
                .build()
                .also { it.surfaceProvider = viewFinder.surfaceProvider }
        
        onSetImageCapture()
        
        val aspectRatio: Float = _mPreviewSize.width / _mPreviewSize.height.toFloat()
        val width = IMAGE_ANALYSIS_SCALE_WIDTH
        val height = (width / aspectRatio).roundToInt()
        
        _mImageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(width, height))
                .build()
        
        _mImageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(activity)) { image ->
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
                        drawLargestRect(largestQuad.contour, largestQuad.points, originalPreviewSize)
                    }
                }
                catch (e: Exception)
                {
                    listener.onFailed(ErrorScannerModel(ErrorScannerModel.ErrorMessage.DETECT_LARGEST_QUADRILATERAL_FAILED, e))
                }
            }
            image.close()
        }
        
        _mCamera = _mCameraProvider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, _mPreview, _mImageAnalysis, _mImageCapture)
    }
    
    private fun onSetImageCapture()
    {
        try
        {
            if (_mImageCapture != null && _mCameraProvider?.isBound(_mImageCapture!!) == true)
            {
                _mCameraProvider?.unbind(_mImageCapture)
            }
            
            _mImageCapture = null
            _mImageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()
        }
        catch (e: Exception)
        {
            listener.onFailed(ErrorScannerModel(ErrorScannerModel.ErrorMessage.ENABLE_IMAGE_CAPTURE_FAILED, e))
        }
    }
    
    private fun onCheckIfFlashIsPresent()
    {
        if (_mCamera?.cameraInfo?.hasFlashUnit() == true) listener.onShowFlash() else listener.onHideFlash()
    }
    
    fun onSwitchFlashState()
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
        
        onSetImageCapture()
        _mCamera = _mCameraProvider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, _mImageCapture)
    }
    
    private fun drawLargestRect(approx: MatOfPoint2f, points: Array<Point>, stdSize: Size)
    {
        // Attention: axis are swapped
        val previewWidth = stdSize.height.toFloat()
        val previewHeight = stdSize.width.toFloat()
        
        val resultWidth = max(previewWidth - points[0].y.toFloat(), previewWidth - points[1].y.toFloat()) - min(previewWidth - points[2].y.toFloat(), previewWidth - points[3].y.toFloat())
        val resultHeight = max(points[1].x.toFloat(), points[2].x.toFloat()) - min(points[0].x.toFloat(), points[3].x.toFloat())
        
        val imgDetectionPropsObj = ImageDetectionProperties(previewWidth.toDouble(), previewHeight.toDouble(), points[0], points[1], points[2], points[3], resultWidth.toInt(), resultHeight.toInt())
        if (imgDetectionPropsObj.isNotValidImage(approx))
        {
            cancelAutoCapture()
        }
        else
        {
            if (!isAutoCaptureScheduled)
            {
                scheduleAutoCapture()
            }
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
    
    private fun scheduleAutoCapture()
    {
        isAutoCaptureScheduled = true
        millisLeft = 0L
        autoCaptureTimer = object : CountDownTimer(DEFAULT_TIME_POST_PICTURE, 90)
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
    
    private fun onTakePicture()
    {
        isCapturing = true
        val imageCapture = _mImageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(originalImageFile).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(activity), object : ImageCapture.OnImageSavedCallback
        {
            override fun onError(e: ImageCaptureException)
            {
                listener.onFailed(ErrorScannerModel(ErrorScannerModel.ErrorMessage.PHOTO_CAPTURE_FAILED, e))
            }
            
            override fun onImageSaved(output: ImageCapture.OutputFileResults)
            {
                unbindCamera()
                listener.scanSurfacePictureTaken()
                Handler(Looper.getMainLooper()).postDelayed({ isCapturing = false }, TIME_POST_PICTURE)
            }
        })
    }
    
    fun unbindCamera()
    {
        // _mCameraProvider?.unbind(_mImageAnalysis)
        _mCameraProvider?.unbindAll()
    }
}