package com.pha.document.scanner

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.pha.document.scanner.common.document_scanner.BaseDocumentReader
import com.pha.document.scanner.common.document_scanner.listener.DocumentReaderListener
import com.pha.document.scanner.common.document_scanner.model.ErrorScannerModel
import com.pha.document.scanner.common.document_scanner.model.ResultScannedModel
import com.pha.document.scanner.common.documentscanner.common.utils.PermissionUtil
import com.pha.document.scanner.common.documentscanner.manager.DocumentSessionManager
import com.pha.document.scanner.common.documentscanner.ui.BaseDocumentScannerActivity.Companion.ORIGINAL_IMAGE_NAME
import com.pha.document.scanner.databinding.ActivityDocmentScannerBinding
import id.zelory.compressor.extension
import java.io.File

class ScanDocumentActivity : AppCompatActivity()
{
    private lateinit var activity: Activity
    private lateinit var binding: ActivityDocmentScannerBinding
    private lateinit var cameraManager: BaseDocumentReader
    
    private lateinit var originalImageFile: File
    private lateinit var imageType: Bitmap.CompressFormat
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        activity = this
        binding = DataBindingUtil.setContentView(activity, R.layout.activity_docment_scanner)
        
        val sessionDocumentManager = DocumentSessionManager(activity)
        imageType = sessionDocumentManager.onGetImageType()
        
        reInitOriginalImageFile()
        onInitCameraManager()
    }
    
    private fun reInitOriginalImageFile()
    {
        originalImageFile = File(filesDir, "$ORIGINAL_IMAGE_NAME.${imageType.extension()}") // re-init image file
        originalImageFile.delete() // delete file if exists
    }
    
    private fun onInitCameraManager()
    {
        cameraManager = BaseDocumentReader(activity, this@ScanDocumentActivity, originalImageFile, binding.viewFinder, listener)
        PermissionUtil.checkCameraSelfPermission(activity) { cameraManager.startCamera() }
    }
    
    private val listener = object : DocumentReaderListener
    {
        override fun onResultScanned(result: ResultScannedModel)
        {
        
        }
        
        override fun onFailed(error: ErrorScannerModel)
        {
        
        }
        
        override fun scanSurfacePictureTaken()
        {
        
        }
        
        override fun onShowFlash()
        {
        
        }
        
        override fun onHideFlash()
        {
        
        }
        
        override fun showFlashModeOn()
        {
        
        }
        
        override fun showFlashModeOff()
        {
        
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        PermissionUtil.checkCameraGranted(activity, requestCode) {
            cameraManager.startCamera()
        }
    }
}