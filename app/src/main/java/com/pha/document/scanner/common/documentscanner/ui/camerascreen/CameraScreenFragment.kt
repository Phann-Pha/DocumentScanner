package com.pha.document.scanner.common.documentscanner.ui.camerascreen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.allShouldShowRationale
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.hide
import com.pha.document.scanner.common.documentscanner.common.extensions.show
import com.pha.document.scanner.common.documentscanner.common.utils.FileUriUtils
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.ui.base.BaseFragment
import com.pha.document.scanner.common.documentscanner.ui.components.scansurface.ScanSurfaceListener
import com.pha.document.scanner.common.documentscanner.ui.components.scansurface.ScanSurfaceView
import com.pha.document.scanner.common.documentscanner.ui.scan.BaseDocumentScannerActivity
import java.io.File
import java.io.FileNotFoundException

internal class CameraScreenFragment : BaseFragment(), ScanSurfaceListener
{
    companion object
    {
        fun newInstance(): CameraScreenFragment
        {
            return CameraScreenFragment()
        }
    }
    
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                        startCroppingProcess()
                    } else
                    {
                        onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR, null))
                    }
                } else
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
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_camera_screen, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        
        val scanSurfaceView = view.findViewById<ScanSurfaceView>(R.id.scanSurfaceView)
        scanSurfaceView.lifecycleOwner = this
        scanSurfaceView.listener = this
        scanSurfaceView?.originalImageFile = getScanActivity().originalImageFile
        
        checkForCameraPermissions(view)
        initListeners(view)
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
        view?.findViewById<ScanSurfaceView>(R.id.scanSurfaceView)?.originalImageFile = getScanActivity().originalImageFile
    }
    
    private fun initListeners(view: View)
    {
        view.findViewById<View>(R.id.cameraCaptureButton).setOnClickListener {
            takePhoto(view)
        }
        view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
            finishActivity()
        }
        view.findViewById<ImageView>(R.id.flashButton).setOnClickListener {
            switchFlashState(view)
        }
        view.findViewById<ImageView>(R.id.galleryButton).setOnClickListener {
            checkForStoragePermissions()
        }
        view.findViewById<TextView>(R.id.autoButton).setOnClickListener {
            toggleAutoManualButton(view)
        }
    }
    
    private fun toggleAutoManualButton(view: View)
    {
        val scanSurfaceView = view.findViewById<ScanSurfaceView>(R.id.scanSurfaceView)
        scanSurfaceView.isAutoCaptureOn = !scanSurfaceView.isAutoCaptureOn
        if (scanSurfaceView.isAutoCaptureOn)
        {
            view.findViewById<TextView>(R.id.autoButton).text = getString(R.string.auto)
        } else
        {
            view.findViewById<TextView>(R.id.autoButton).text = getString(R.string.manual)
        }
    }
    
    private fun checkForCameraPermissions(view: View)
    {
        permissionsBuilder(Manifest.permission.CAMERA)
            .build()
            .send { result ->
                if (result.allGranted())
                {
                    startCamera(view)
                } else if (result.allShouldShowRationale())
                {
                    onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.CAMERA_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN))
                } else
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
                } else if (result.allShouldShowRationale())
                {
                    onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.STORAGE_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN))
                } else
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
        } else
        {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    
    private fun startCamera(view: View)
    {
        view.findViewById<ScanSurfaceView>(R.id.scanSurfaceView).start(view)
    }
    
    private fun takePhoto(view: View)
    {
        view.findViewById<ScanSurfaceView>(R.id.scanSurfaceView).takePicture(view)
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    private fun finishActivity()
    {
        getScanActivity().finish()
    }
    
    private fun switchFlashState(view: View)
    {
        view.findViewById<ScanSurfaceView>(R.id.scanSurfaceView).switchFlashState()
    }
    
    override fun showFlash()
    {
        view?.findViewById<ImageView>(R.id.flashButton)?.show()
    }
    
    override fun hideFlash()
    {
        view?.findViewById<ImageView>(R.id.flashButton)?.hide()
    }
    
    private fun selectImageFromGallery()
    {
        val photoPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
        photoPickerIntent.type = "image/*"
        resultLauncher.launch(photoPickerIntent)
    }
    
    override fun scanSurfacePictureTaken()
    {
        startCroppingProcess()
    }
    
    private fun startCroppingProcess()
    {
        if (isAdded)
        {
            getScanActivity().showImageCropFragment()
        }
    }
    
    override fun scanSurfaceShowProgress()
    {
        showProgressBar()
    }
    
    override fun scanSurfaceHideProgress()
    {
        hideProgressBar()
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
        view?.findViewById<ImageView>(R.id.flashButton)?.setImageResource(R.drawable.flash_on)
    }
    
    override fun showFlashModeOff()
    {
        view?.findViewById<ImageView>(R.id.flashButton)?.setImageResource(R.drawable.flash_off)
    }
}