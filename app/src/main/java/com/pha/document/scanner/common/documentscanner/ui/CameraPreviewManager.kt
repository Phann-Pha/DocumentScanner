package com.pha.document.scanner.common.documentscanner.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.allShouldShowRationale
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.hide
import com.pha.document.scanner.common.documentscanner.common.extensions.show
import com.pha.document.scanner.common.documentscanner.common.utils.FileUriUtils
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.ui.components.scansurface.ScanSurfaceListener
import com.pha.document.scanner.databinding.FragmentCameraScreenBinding
import java.io.File
import java.io.FileNotFoundException

internal class CameraPreviewManager : Fragment(), ScanSurfaceListener
{
    private lateinit var binding: FragmentCameraScreenBinding
    
    companion object
    {
        fun newInstance(): CameraPreviewManager = CameraPreviewManager()
    }
    
    private var resultGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            try
            {
                val imageUri = result.data?.data
                if (imageUri != null)
                {
                    val realPath = FileUriUtils.getRealPath(getScanActivity(), imageUri)
                    if (realPath != null)
                    {
                        getScanActivity().reInitOriginalImageFile()
                        getScanActivity().originalImageFile = File(realPath)
                        startCroppingProcess() // start cropping image, from launcher gallery result
                    }
                    else
                    {
                        onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR, null))
                    }
                }
                else
                {
                    onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR, null))
                }
            }
            catch (e: FileNotFoundException)
            {
                onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR, e))
            }
        }
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera_screen, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        onConfigSurfaceView()
        onCheckCameraPermissions(view)
        onEventClickListener()
    }
    
    private fun onConfigSurfaceView()
    {
        binding.scanSurfaceView.apply {
            lifecycleOwner = this@CameraPreviewManager
            listener = this@CameraPreviewManager
            originalImageFile = getScanActivity().originalImageFile
        }
    }
    
    private fun onEventClickListener()
    {
        binding.cameraCaptureButton.setOnClickListener {
            binding.scanSurfaceView.onTakePicture()
        }
        binding.cancelButton.setOnClickListener {
            getScanActivity().finish()
        }
        binding.flashButton.setOnClickListener {
            binding.scanSurfaceView.switchFlashState()
        }
        binding.galleryButton.setOnClickListener {
            checkForStoragePermissions()
        }
        binding.autoButton.setOnClickListener {
            binding.scanSurfaceView.isAutoCaptureOn = !binding.scanSurfaceView.isAutoCaptureOn
            if (binding.scanSurfaceView.isAutoCaptureOn)
            {
                binding.autoButton.text = getString(R.string.auto)
            }
            else
            {
                binding.autoButton.text = getString(R.string.manual)
            }
        }
    }
    
    private fun onCheckCameraPermissions(view: View)
    {
        permissionsBuilder(Manifest.permission.CAMERA).build().send { result ->
            if (result.allGranted())
            {
                binding.scanSurfaceView.start(view)
            }
            else if (result.allShouldShowRationale())
            {
                onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.CAMERA_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN))
            }
            else
            {
                onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.CAMERA_PERMISSION_REFUSED_GO_TO_SETTINGS))
            }
        }
    }
    
    private fun checkForStoragePermissions()
    {
        permissionsBuilder(getStoragePermission())
                .build()
                .send { result ->
                    if (result.allGranted())
                    {
                        selectImageFromGallery()
                    }
                    else if (result.allShouldShowRationale())
                    {
                        onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.STORAGE_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN))
                    }
                    else
                    {
                        onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.STORAGE_PERMISSION_REFUSED_GO_TO_SETTINGS))
                    }
                }
    }
    
    private fun getStoragePermission(): String
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            Manifest.permission.READ_MEDIA_IMAGES
        }
        else
        {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    override fun showFlash()
    {
        binding.flashButton.show()
    }
    
    override fun hideFlash()
    {
        binding.flashButton.hide()
    }
    
    private fun selectImageFromGallery()
    {
        val photoPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
        photoPickerIntent.type = "image/*"
        resultGalleryLauncher.launch(photoPickerIntent)
    }
    
    override fun scanSurfacePictureTaken()
    {
        startCroppingProcess()
    }
    
    private fun startCroppingProcess()
    {
        if (isAdded)
        {
            getScanActivity().showImageCropManagerScreen()
        }
    }
    
    override fun scanSurfaceShowProgress()
    {
        binding.progressLayout.show()
    }
    
    override fun scanSurfaceHideProgress()
    {
        binding.progressLayout.hide()
    }
    
    override fun onError(error: ErrorScannerModel)
    {
        if (isAdded)
        {
            getScanActivity().onError(error)
        }
    }
    
    override fun showFlashModeOn()
    {
        binding.flashButton.setImageResource(R.drawable.flash_on)
    }
    
    override fun showFlashModeOff()
    {
        binding.flashButton.setImageResource(R.drawable.flash_off)
    }
    
    override fun onDestroy()
    {
        super.onDestroy()
        if (getScanActivity().shouldCallOnClose)
        {
            getScanActivity().onClose()
        }
    }
    
    override fun onResume()
    {
        super.onResume()
        getScanActivity().reInitOriginalImageFile()
        binding.scanSurfaceView.originalImageFile = getScanActivity().originalImageFile
    }
}