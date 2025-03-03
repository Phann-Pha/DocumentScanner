package com.pha.document.scanner.common.documentscanner.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.scaledBitmap
import com.pha.document.scanner.common.documentscanner.common.utils.OpenCvNativeBridge
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
    
    private val nativeClass = OpenCvNativeBridge()
    private var selectedImage: Bitmap? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_image_crop, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
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
        binding.holderImageView.post { initializeCropping() }
        onInitEventClickListener()
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    private fun initializeCropping()
    {
        fun getEdgePoints(tempBitmap: Bitmap): Map<Int, PointF>
        {
            val pointFs: List<PointF> = nativeClass.getContourEdgePoints(tempBitmap)
            return binding.polygonView.getOrderedValidEdgePoints(tempBitmap, pointFs)
        }
        
        if (selectedImage != null && selectedImage!!.width > 0 && selectedImage!!.height > 0)
        {
            val scaledBitmap: Bitmap = selectedImage!!.scaledBitmap(binding.holderImageCrop.width, binding.holderImageCrop.height)
            binding.imagePreview.setImageBitmap(scaledBitmap)
            val tempBitmap = (binding.imagePreview.drawable as BitmapDrawable).bitmap
            val pointFs = getEdgePoints(tempBitmap)
            
            binding.polygonView.setPoints(pointFs)
            binding.polygonView.visibility = View.VISIBLE
            val padding = resources.getDimension(R.dimen.polygon_dimens).toInt()
            val layoutParams = FrameLayout.LayoutParams(tempBitmap.width + padding, tempBitmap.height + padding)
            layoutParams.gravity = Gravity.CENTER
            binding.polygonView.layoutParams = layoutParams
        }
    }
    
    private fun onInitEventClickListener()
    {
        binding.closeButton.setOnClickListener {
            closeFragment()
        }
        binding.confirmButton.setOnClickListener {
            onGetCroppedImage()
            getScanActivity().showImageProcessingManagerScreen()
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
                val points: Map<Int, PointF> = binding.polygonView.getPoints()
                val xRatio: Float = selectedImage!!.width.toFloat() / binding.imagePreview.width
                val yRatio: Float = selectedImage!!.height.toFloat() / binding.imagePreview.height
                val pointPadding = requireContext().resources.getDimension(R.dimen.point_padding).toInt()
                val x1: Float = (points.getValue(0).x + pointPadding) * xRatio
                val x2: Float = (points.getValue(1).x + pointPadding) * xRatio
                val x3: Float = (points.getValue(2).x + pointPadding) * xRatio
                val x4: Float = (points.getValue(3).x + pointPadding) * xRatio
                val y1: Float = (points.getValue(0).y + pointPadding) * yRatio
                val y2: Float = (points.getValue(1).y + pointPadding) * yRatio
                val y3: Float = (points.getValue(2).y + pointPadding) * yRatio
                val y4: Float = (points.getValue(3).y + pointPadding) * yRatio
                getScanActivity().croppedImage = nativeClass.getScannedBitmap(selectedImage!!, x1, y1, x2, y2, x3, y3, x4, y4)
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