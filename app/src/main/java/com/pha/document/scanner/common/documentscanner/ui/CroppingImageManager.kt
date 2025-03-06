package com.pha.document.scanner.common.documentscanner.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.scaledBitmap
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.databinding.FragmentImageCropBinding
import id.zelory.compressor.determineImageRotation

internal class CroppingImageManager : Fragment()
{
    private lateinit var binding: FragmentImageCropBinding
    
    companion object
    {
        fun newInstance(): CroppingImageManager = CroppingImageManager()
    }
    
    private var selectedImage: Bitmap? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_image_crop, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        
        onSyneImageCaptured()
        binding.holderImageView.post {
            initializeCropping()
        }
        onInitEventClickListener()
    }
    
    private fun onSyneImageCaptured()
    {
        val sourceBitmap = BitmapFactory.decodeFile(getScanActivity().originalImageFile.absolutePath)
        if (sourceBitmap != null)
        {
            selectedImage = determineImageRotation(getScanActivity().originalImageFile, sourceBitmap)
        }
        else
        {
            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.INVALID_IMAGE))
            Handler(Looper.getMainLooper()).post { closeFragment() }
        }
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    private fun initializeCropping()
    {
        try
        {
            if (selectedImage != null && selectedImage!!.width > 0 && selectedImage!!.height > 0)
            {
                val scaledBitmap: Bitmap = selectedImage!!.scaledBitmap(binding.holderImageCrop.width, binding.holderImageCrop.height)
                binding.CropImagePreview.setImageBitmap(scaledBitmap)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }
    
    private fun onInitEventClickListener()
    {
        binding.closeButton.setOnClickListener {
            closeFragment()
        }
        binding.confirmButton.setOnClickListener {
            onGetCroppedImage()
            getScanActivity().finalScannerResult()
        }
    }
    
    private fun onError(error: ErrorScannerModel)
    {
        if (isAdded)
        {
            getScanActivity().onError(error)
        }
    }
    
    private fun onGetCroppedImage()
    {
        if (selectedImage != null)
        {
            try
            {
                getScanActivity().croppedImage = binding.CropImagePreview.getCroppedImage()
            }
            catch (e: Exception)
            {
                onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.CROPPING_FAILED, e))
            }
        }
        else
        {
            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.INVALID_IMAGE))
        }
    }
    
    private fun closeFragment()
    {
        getScanActivity().closeCurrentFragment()
    }
}