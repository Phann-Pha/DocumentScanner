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
import com.pha.document.scanner.common.document_scanner.DocumentCameraManager
import com.pha.document.scanner.common.document_scanner.analyzer.LiveDocumentDetector
import com.pha.document.scanner.common.document_scanner.task.ObjectingDetectionTask
import com.pha.document.scanner.common.document_scanner.util.DocDetectionTask
import com.pha.document.scanner.common.documentscanner.common.utils.PermissionUtil
import com.pha.document.scanner.common.liveness_detection.utils.LiveDetectionTask
import com.pha.document.scanner.databinding.ActivityDocmentScannerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScanDocumentActivity : AppCompatActivity()
{
    private lateinit var activity: Activity
    private lateinit var binding: ActivityDocmentScannerBinding
    private lateinit var cameraManager: DocumentCameraManager
    
    private val liveDetector = LiveDocumentDetector(ObjectingDetectionTask())
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        activity = this
        binding = DataBindingUtil.setContentView(activity, R.layout.activity_docment_scanner)
        
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
                outline.setRoundRect(0, 0, view.width, view.height, 24f)
            }
        }
        cameraManager = DocumentCameraManager(activity, this@ScanDocumentActivity, liveDetector, binding.viewFinder, binding.textInstruction) { status ->
            when (status)
            {
                DocDetectionTask.DETECTION ->
                {
                    cameraManager.pause()
                    binding.textInstruction.text = "OK"
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