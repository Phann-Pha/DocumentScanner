package com.pha.document.scanner.common.liveness_detection.tasks

import com.google.mlkit.vision.face.Face
import com.pha.document.scanner.common.liveness_detection.utils.DetectionUtils
import com.pha.document.scanner.common.liveness_detection.utils.LiveDetectionTask

class SmileDetectionTask : DetectionTask
{
    override var isTaskCompleted: Boolean = false
    
    override fun taskName(): String
    {
        return LiveDetectionTask.SMILE_DETECTION
    }
    
    override fun taskDescription(): String
    {
        return "Please smile"
    }
    
    override fun process(face: Face, timestamp: Long): Boolean
    {
        val isSmile = (face.smilingProbability ?: 0f) > 0.67f
        return isSmile && DetectionUtils.isFacing(face)
    }
}