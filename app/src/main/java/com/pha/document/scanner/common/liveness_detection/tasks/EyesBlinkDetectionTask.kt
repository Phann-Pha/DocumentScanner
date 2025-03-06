package com.pha.document.scanner.common.liveness_detection.tasks

import com.google.mlkit.vision.face.Face
import com.pha.document.scanner.common.liveness_detection.utils.DetectionUtils
import com.pha.document.scanner.common.liveness_detection.utils.LiveDetectionTask
import kotlin.math.absoluteValue

class EyesBlinkDetectionTask : DetectionTask
{
    private var timestamp: Long = 0L
    private var trackingId: Int = -1
    private var eyesOpen: Float = 0.6f
    private var eyesClose: Float = 0.3f
    private var leftEyeOpenProbabilityList: MutableList<Float> = arrayListOf()
    private var rightEyeOpenProbabilityList: MutableList<Float> = arrayListOf()
    
    private var sec = 0.1 * 1000000000
    override var isTaskCompleted: Boolean = false
    
    override fun taskName(): String
    {
        return LiveDetectionTask.BLINK_EYES_DETECTION
    }
    
    override fun taskDescription(): String
    {
        return "Please blink your eyes"
    }
    
    override fun process(face: Face, timestamp: Long): Boolean
    {
        var leftEyeBlinked = false
        var rightEyeBlinked = false
        
        if ((this.timestamp - timestamp).absoluteValue > sec)
        {
            this.timestamp = timestamp
            if (trackingId != face.trackingId)
            {
                trackingId = face.trackingId ?: -1
            }
            else
            {
                leftEyeOpenProbabilityList.add(face.leftEyeOpenProbability ?: 0f)
                if (leftEyeOpenProbabilityList.size > 3)
                {
                    if (leftEyeOpenProbabilityList[leftEyeOpenProbabilityList.size - 3] > eyesOpen && leftEyeOpenProbabilityList[leftEyeOpenProbabilityList.size - 2] < eyesClose && leftEyeOpenProbabilityList[leftEyeOpenProbabilityList.size - 1] > eyesOpen)
                    {
                        leftEyeBlinked = true
                    }
                    else
                    {
                        leftEyeBlinked = false
                    }
                }
                rightEyeOpenProbabilityList.add(face.rightEyeOpenProbability ?: 0f)
                if (rightEyeOpenProbabilityList.size > 3)
                {
                    if (rightEyeOpenProbabilityList[rightEyeOpenProbabilityList.size - 3] > eyesOpen && rightEyeOpenProbabilityList[rightEyeOpenProbabilityList.size - 2] < eyesClose && rightEyeOpenProbabilityList[rightEyeOpenProbabilityList.size - 1] > eyesOpen)
                    {
                        rightEyeBlinked = true
                    }
                    else
                    {
                        rightEyeBlinked = false
                    }
                }
            }
        }
        return leftEyeBlinked && rightEyeBlinked && DetectionUtils.isFacing(face)
    }
}
