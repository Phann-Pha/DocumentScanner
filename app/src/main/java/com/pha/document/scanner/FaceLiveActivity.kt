package com.pha.document.scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Outline
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.pha.document.scanner.common.documentscanner.common.utils.PermissionUtil
import com.pha.document.scanner.common.liveness_detection.CameraManager
import com.pha.document.scanner.common.liveness_detection.analyzer.LiveDetector
import com.pha.document.scanner.common.liveness_detection.tasks.EyesBlinkDetectionTask
import com.pha.document.scanner.common.liveness_detection.tasks.FacingDetectionTask
import com.pha.document.scanner.common.liveness_detection.utils.LiveDetectionTask
import com.pha.document.scanner.databinding.ActivityFaceLiveBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FaceLiveActivity : AppCompatActivity()
{
    private lateinit var activity: Activity
    private lateinit var binding: ActivityFaceLiveBinding
    private lateinit var cameraManager: CameraManager
    
    private val liveDetector = LiveDetector(FacingDetectionTask(), EyesBlinkDetectionTask())
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        activity = this
        binding = DataBindingUtil.setContentView(activity, R.layout.activity_face_live)
        
        lifecycleScope.launch(Dispatchers.Default) {
            onInitCameraManager()
            PermissionUtil.checkCameraSelfPermission(activity) {
                cameraManager.start()
            }
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun onInitCameraManager()
    {
        binding.viewFinder.clipToOutline = true
        binding.viewFinder.outlineProvider = object : ViewOutlineProvider()
        {
            override fun getOutline(view: View, outline: Outline)
            {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
            }
        }
        cameraManager = CameraManager(activity, this@FaceLiveActivity, liveDetector, binding.viewFinder, binding.textInstruction) { status ->
            when (status)
            {
                LiveDetectionTask.FACING_DETECTION ->
                {
                    binding.textInstruction.text = "Please blink your eyes"
                }
                
                LiveDetectionTask.BLINK_EYES_DETECTION ->
                {
                    binding.animationView.playAnimation()
                    binding.textInstruction.text = "Completed"
                    cameraManager.pause()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        PermissionUtil.checkCameraGranted(activity, requestCode) {
            cameraManager.start()
        }
    }
}