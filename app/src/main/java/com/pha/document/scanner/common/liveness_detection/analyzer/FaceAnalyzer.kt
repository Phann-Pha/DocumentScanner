package com.pha.document.scanner.common.liveness_detection.analyzer

import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import androidx.camera.view.TransformExperimental
import androidx.core.os.ExecutorCompat
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.max

@TransformExperimental
class FaceAnalyzer(detector: LiveDetector) : Analyzer
{
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build()
    )
    
    private val delegate = MlKitAnalyzer(listOf(faceDetector), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, ExecutorCompat.create(Handler(Looper.getMainLooper()))) { result ->
        detector.process(result.getValue(faceDetector), detectionSize, result.timestamp)
    }
    
    private var detectionSize: Int = 640
    
    override fun analyze(image: ImageProxy)
    {
        detectionSize = max(image.width, image.height)
        delegate.analyze(image)
    }
    
    override fun getDefaultTargetResolution(): Size
    {
        return delegate.defaultTargetResolution
    }
    
    override fun getTargetCoordinateSystem(): Int
    {
        return delegate.targetCoordinateSystem
    }
    
    override fun updateTransform(matrix: Matrix?)
    {
        delegate.updateTransform(matrix)
    }
}