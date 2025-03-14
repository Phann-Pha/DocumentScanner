package com.pha.document.scanner.common.liveness_detection.tasks

import com.google.mlkit.vision.face.Face
import com.pha.document.scanner.common.liveness_detection.utils.DetectionUtils
import com.pha.document.scanner.common.liveness_detection.utils.LiveDetectionTask

class FacingDetectionTask : DetectionTask
{
    companion object
    {
        private const val FACING_CAMERA_KEEP_TIME = 1500L
    }
    
    override var isTaskCompleted: Boolean = false
    
    override fun taskName(): String
    {
        return LiveDetectionTask.FACING_DETECTION
    }
    
    override fun taskDescription(): String
    {
        return "Please squarely facing the camera"
    }
    
    private var startTime = 0L
    
    override fun start()
    {
        startTime = System.currentTimeMillis()
    }
    
    override fun process(face: Face, timestamp: Long): Boolean
    {
        if (!DetectionUtils.isFacing(face))
        {
            startTime = System.currentTimeMillis()
            return false
        }
        return System.currentTimeMillis() - startTime >= FACING_CAMERA_KEEP_TIME
    }
}