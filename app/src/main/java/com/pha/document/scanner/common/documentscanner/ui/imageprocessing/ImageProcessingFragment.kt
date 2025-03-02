package com.pha.document.scanner.common.documentscanner.ui.imageprocessing

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.rotateBitmap
import com.pha.document.scanner.common.documentscanner.ui.base.BaseFragment
import com.pha.document.scanner.common.documentscanner.ui.scan.BaseDocumentScannerActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ImageProcessingFragment : BaseFragment()
{
    companion object
    {
        private const val ANGLE_OF_ROTATION = 90
        
        fun newInstance(): ImageProcessingFragment
        {
            return ImageProcessingFragment()
        }
    }
    
    private var isInverted = false
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_image_processing, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<ImageView>(R.id.imagePreview).setImageBitmap(getScanActivity().croppedImage)
        
        initListeners(view)
    }
    
    private fun initListeners(view: View)
    {
        view.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            closeFragment()
        }
        view.findViewById<ImageView>(R.id.confirmButton).setOnClickListener {
            selectFinalScannerResults()
        }
        view.findViewById<ImageView>(R.id.magicButton).setOnClickListener {
            applyGrayScaleFilter(view)
        }
        view.findViewById<ImageView>(R.id.rotateButton).setOnClickListener {
            rotateImage(view)
        }
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun rotateImage(view: View)
    {
        showProgressBar()
        GlobalScope.launch(Dispatchers.IO) {
            if (isAdded)
            {
                getScanActivity().transformedImage = getScanActivity().transformedImage?.rotateBitmap(ANGLE_OF_ROTATION)
                getScanActivity().croppedImage = getScanActivity().croppedImage?.rotateBitmap(ANGLE_OF_ROTATION)
            }
            
            if (isAdded)
            {
                getScanActivity().runOnUiThread {
                    hideProgressBar()
                    if (isInverted)
                    {
                        view.findViewById<ImageView>(R.id.imagePreview).setImageBitmap(getScanActivity().transformedImage)
                    } else
                    {
                        view.findViewById<ImageView>(R.id.imagePreview).setImageBitmap(getScanActivity().croppedImage)
                    }
                }
            }
        }
    }
    
    private fun closeFragment()
    {
        getScanActivity().closeCurrentFragment()
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun applyGrayScaleFilter(view: View)
    {
        showProgressBar()
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
                        hideProgressBar()
                        view.findViewById<ImageView>(R.id.imagePreview).setImageBitmap(getScanActivity().transformedImage)
                    }
                } else
                {
                    getScanActivity().runOnUiThread {
                        hideProgressBar()
                        view.findViewById<ImageView>(R.id.imagePreview).setImageBitmap(getScanActivity().croppedImage)
                    }
                }
                isInverted = !isInverted
            }
        }
    }
    
    private fun selectFinalScannerResults()
    {
        getScanActivity().finalScannerResult()
    }
}