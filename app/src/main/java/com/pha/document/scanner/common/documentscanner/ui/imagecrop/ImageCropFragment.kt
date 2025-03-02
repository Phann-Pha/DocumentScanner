package com.pha.document.scanner.common.documentscanner.ui.imagecrop

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
import android.widget.ImageView
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.scaledBitmap
import com.pha.document.scanner.common.documentscanner.common.utils.OpenCvNativeBridge
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.ui.base.BaseFragment
import com.pha.document.scanner.common.documentscanner.ui.components.polygon.PolygonView
import com.pha.document.scanner.common.documentscanner.ui.scan.BaseDocumentScannerActivity
import id.zelory.compressor.determineImageRotation

internal class ImageCropFragment : BaseFragment()
{
    companion object
    {
        fun newInstance(): ImageCropFragment
        {
            return ImageCropFragment()
        }
    }
    
    private val nativeClass = OpenCvNativeBridge()
    
    private var selectedImage: Bitmap? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_image_crop, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        
        val sourceBitmap = BitmapFactory.decodeFile(getScanActivity().originalImageFile.absolutePath)
        if (sourceBitmap != null)
        {
            selectedImage = determineImageRotation(getScanActivity().originalImageFile, sourceBitmap)
        } else
        {
            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.INVALID_IMAGE))
            Handler(Looper.getMainLooper()).post {
                closeFragment()
            }
        }
        view.findViewById<FrameLayout>(R.id.holderImageView).post {
            initializeCropping(view)
        }
        
        initListeners(view)
    }
    
    private fun initListeners(view: View)
    {
        view.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            closeFragment()
        }
        view.findViewById<ImageView>(R.id.confirmButton).setOnClickListener {
            onConfirmButtonClicked(view)
        }
    }
    
    private fun getScanActivity(): BaseDocumentScannerActivity
    {
        return (requireActivity() as BaseDocumentScannerActivity)
    }
    
    private fun initializeCropping(view: View)
    {
        if (selectedImage != null && selectedImage!!.width > 0 && selectedImage!!.height > 0)
        {
            val holderImageCrop = view.findViewById<FrameLayout>(R.id.holderImageCrop)
            val imagePreview = view.findViewById<ImageView>(R.id.imagePreview)
            val polygonView = view.findViewById<PolygonView>(R.id.polygonView)
            
            val scaledBitmap: Bitmap = selectedImage!!.scaledBitmap(holderImageCrop.width, holderImageCrop.height)
            imagePreview.setImageBitmap(scaledBitmap)
            val tempBitmap = (imagePreview.drawable as BitmapDrawable).bitmap
            val pointFs = getEdgePoints(polygonView, tempBitmap)
            
            polygonView.setPoints(pointFs)
            polygonView.visibility = View.VISIBLE
            val padding = resources.getDimension(R.dimen.polygon_dimens).toInt()
            val layoutParams = FrameLayout.LayoutParams(tempBitmap.width + padding, tempBitmap.height + padding)
            layoutParams.gravity = Gravity.CENTER
            polygonView.layoutParams = layoutParams
        }
    }
    
    private fun onError(error: ErrorScannerModel)
    {
        if (isAdded)
        {
            getScanActivity().onError(error)
        }
    }
    
    private fun onConfirmButtonClicked(view: View)
    {
        getCroppedImage(view)
        startImageProcessingFragment()
    }
    
    private fun getEdgePoints(polygonView: PolygonView, tempBitmap: Bitmap): Map<Int, PointF>
    {
        val pointFs: List<PointF> = nativeClass.getContourEdgePoints(tempBitmap)
        return polygonView.getOrderedValidEdgePoints(tempBitmap, pointFs)
    }
    
    private fun getCroppedImage(view: View)
    {
        if (selectedImage != null)
        {
            try
            {
                val imagePreview = view.findViewById<ImageView>(R.id.imagePreview)
                val polygonView = view.findViewById<PolygonView>(R.id.polygonView)
                
                val points: Map<Int, PointF> = polygonView.getPoints()
                val xRatio: Float = selectedImage!!.width.toFloat() / imagePreview.width
                val yRatio: Float = selectedImage!!.height.toFloat() / imagePreview.height
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
        } else
        {
            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.INVALID_IMAGE))
        }
    }
    
    private fun startImageProcessingFragment()
    {
        getScanActivity().showImageProcessingFragment()
    }
    
    private fun closeFragment()
    {
        getScanActivity().closeCurrentFragment()
    }
}