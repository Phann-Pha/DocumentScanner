package com.pha.document.scanner.common.documentscanner.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.hide
import com.pha.document.scanner.common.documentscanner.common.extensions.rotateBitmap
import com.pha.document.scanner.common.documentscanner.common.extensions.show
import com.pha.document.scanner.databinding.FragmentImageProcessingBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ImageProcessingManager : Fragment()
{
    private lateinit var binding: FragmentImageProcessingBinding
    
    companion object
    {
        private const val ANGLE_OF_ROTATION = 90
        fun newInstance(): ImageProcessingManager = ImageProcessingManager()
    }
    
    private var isInverted = false
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_image_processing, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        
        binding.imagePreview.setImageBitmap(getScanActivity().croppedImage)
        onInitEventClickListeners()
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    private fun onInitEventClickListeners()
    {
        binding.closeButton.setOnClickListener {
            getScanActivity().closeCurrentFragment()
        }
        binding.confirmButton.setOnClickListener {
            getScanActivity().finalScannerResult()
        }
        binding.magicButton.setOnClickListener {
            applyGrayScaleFilter()
        }
        binding.rotateButton.setOnClickListener {
            rotateImage()
        }
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun rotateImage()
    {
        binding.progressLayout.show()
        GlobalScope.launch(Dispatchers.IO) {
            if (isAdded)
            {
                getScanActivity().transformedImage = getScanActivity().transformedImage?.rotateBitmap(ANGLE_OF_ROTATION)
                getScanActivity().croppedImage = getScanActivity().croppedImage?.rotateBitmap(ANGLE_OF_ROTATION)
            }
            
            if (isAdded)
            {
                getScanActivity().runOnUiThread {
                    binding.progressLayout.hide()
                    if (isInverted)
                    {
                        binding.imagePreview.setImageBitmap(getScanActivity().transformedImage)
                    }
                    else
                    {
                        binding.imagePreview.setImageBitmap(getScanActivity().croppedImage)
                    }
                }
            }
        }
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun applyGrayScaleFilter()
    {
        binding.progressLayout.show()
        GlobalScope.launch(Dispatchers.IO) {
            if (isAdded)
            {
                if (!isInverted)
                {
                    val bmpMonochrome = Bitmap.createBitmap(getScanActivity().croppedImage!!.width, getScanActivity().croppedImage!!.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmpMonochrome)
                    val ma = ColorMatrix()
                    ma.setSaturation(0f)
                    val paint = Paint()
                    paint.colorFilter = ColorMatrixColorFilter(ma)
                    getScanActivity().croppedImage?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
                    getScanActivity().transformedImage = bmpMonochrome.copy(bmpMonochrome.config!!, true)
                    getScanActivity().runOnUiThread {
                        binding.progressLayout.hide()
                        binding.imagePreview.setImageBitmap(getScanActivity().transformedImage)
                    }
                }
                else
                {
                    getScanActivity().runOnUiThread {
                        binding.progressLayout.hide()
                        binding.imagePreview.setImageBitmap(getScanActivity().croppedImage)
                    }
                }
                isInverted = !isInverted
            }
        }
    }
}