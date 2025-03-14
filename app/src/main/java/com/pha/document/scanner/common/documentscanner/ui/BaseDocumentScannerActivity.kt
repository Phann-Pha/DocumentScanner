package com.pha.document.scanner.common.documentscanner.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.hide
import com.pha.document.scanner.common.documentscanner.common.extensions.scaledCipherTextBitmap
import com.pha.document.scanner.common.documentscanner.common.extensions.show
import com.pha.document.scanner.common.documentscanner.common.utils.getRawValue
import com.pha.document.scanner.common.documentscanner.common.utils.strippingWhiteSpace
import com.pha.document.scanner.common.documentscanner.manager.DocumentSessionManager
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.model.MrzHelper
import com.pha.document.scanner.common.documentscanner.model.ScannerResults
import com.pha.document.scanner.common.documentscanner.ui.components.ProgressView
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import id.zelory.compressor.extension
import id.zelory.compressor.saveBitmap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

abstract class BaseDocumentScannerActivity : AppCompatActivity()
{
    private lateinit var activity: Activity
    abstract fun onError(error: ErrorScannerModel)
    abstract fun onSuccess(scannerResults: ScannerResults)
    abstract fun onClose()
    
    companion object
    {
        internal const val CAMERA_SCREEN_FRAGMENT_TAG = "CameraScreenFragmentTag"
        internal const val IMAGE_CROP_FRAGMENT_TAG = "ImageCropFragmentTag"
        internal const val ORIGINAL_IMAGE_NAME = "original"
        internal const val CROPPED_IMAGE_NAME = "cropped"
        internal const val TRANSFORMED_IMAGE_NAME = "transformed"
        internal const val NOT_INITIALIZED = -1L
    }
    
    internal lateinit var originalImageFile: File
    internal var croppedImage: Bitmap? = null
    internal var transformedImage: Bitmap? = null
    private var imageQuality: Int = 100
    private var imageSize: Long = NOT_INITIALIZED
    private lateinit var imageType: Bitmap.CompressFormat
    internal var shouldCallOnClose = true
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        activity = this
        
        val sessionDocumentManager = DocumentSessionManager(activity)
        imageType = sessionDocumentManager.onGetImageType()
        imageSize = sessionDocumentManager.onGetImageSize()
        imageQuality = sessionDocumentManager.onGetImageQuality()
        reInitOriginalImageFile()
    }
    
    internal fun reInitOriginalImageFile()
    {
        originalImageFile = File(filesDir, "$ORIGINAL_IMAGE_NAME.${imageType.extension()}") // re-init image file
        originalImageFile.delete() // delete file if exists
    }
    
    private fun showCameraScreen()
    {
        addFragmentToBackStack(CameraPreviewManager.newInstance(), CAMERA_SCREEN_FRAGMENT_TAG)
    }
    
    internal fun showImageCropManagerScreen()
    {
        addFragmentToBackStack(CroppingImageManager.newInstance(), IMAGE_CROP_FRAGMENT_TAG)
    }
    
    internal fun closeCurrentFragment()
    {
        supportFragmentManager.popBackStackImmediate()
    }
    
    private fun addFragmentToBackStack(fragment: Fragment, fragmentTag: String)
    {
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.Content, fragment, fragmentTag)
        if (supportFragmentManager.findFragmentByTag(fragmentTag) == null)
        {
            fragmentTransaction.addToBackStack(fragmentTag)
        }
        fragmentTransaction.commit()
    }
    
    internal fun finalScannerResult()
    {
        findViewById<FrameLayout>(R.id.Content).hide()
        compressFiles()
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun compressFiles()
    {
        findViewById<ProgressView>(R.id.ProgressView).show()
        GlobalScope.launch(Dispatchers.IO) {
            var croppedImageFile: File? = null
            croppedImage?.let {
                croppedImageFile = File(filesDir, "$CROPPED_IMAGE_NAME.${imageType.extension()}")
                saveBitmap(it, croppedImageFile!!, imageType, imageQuality)
            }
            
            var transformedImageFile: File? = null
            transformedImage?.let {
                transformedImageFile = File(filesDir, "$TRANSFORMED_IMAGE_NAME.${imageType.extension()}")
                saveBitmap(it, transformedImageFile!!, imageType, imageQuality)
            }
            
            originalImageFile = Compressor.compress(activity, originalImageFile) {
                quality(imageQuality)
                if (imageSize != NOT_INITIALIZED) size(imageSize)
                format(imageType)
            }
            
            croppedImageFile = croppedImageFile?.let {
                Compressor.compress(activity, it) {
                    quality(imageQuality)
                    if (imageSize != NOT_INITIALIZED) size(imageSize)
                    format(imageType)
                }
            }
            
            transformedImageFile = transformedImageFile?.let {
                Compressor.compress(activity, it) {
                    quality(imageQuality)
                    if (imageSize != NOT_INITIALIZED) size(imageSize)
                    format(imageType)
                }
            }
            
            val scannerResults = ScannerResults(originalImageFile, croppedImageFile, transformedImageFile)
            runOnUiThread {
                findViewById<ProgressView>(R.id.ProgressView).hide()
                shouldCallOnClose = false
                supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onAnalyzeDocument(scannerResults)
                shouldCallOnClose = true
            }
        }
    }
    
    internal fun addFragmentContentLayout()
    {
        val frameLayout = FrameLayout(activity)
        frameLayout.id = R.id.Content
        addContentView(frameLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        
        val progressView = ProgressView(activity)
        progressView.id = R.id.ProgressView
        addContentView(progressView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        
        progressView.hide()
        showCameraScreen()
    }
    
    private fun onAnalyzeDocument(scannerResults: ScannerResults)
    {
        val sourceBitmap = BitmapFactory.decodeFile(scannerResults.croppedImageFile?.absolutePath)
        if (sourceBitmap != null)
        {
            val bitmap = sourceBitmap.scaledCipherTextBitmap(sourceBitmap.width, sourceBitmap.height)
            val image = InputImage.fromBitmap(bitmap, 0)
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            textRecognizer.process(image)
                    .addOnSuccessListener {
                        val rawValue = it.textBlocks
                        val cipher = strippingWhiteSpace(getRawValue(rawValue))
                        val helper = MrzHelper()
                        val mrz = helper.process(cipher)
                        
                        if (mrz?.getMRZType() == "TD1" || mrz?.getMRZType() == "TD3")
                        {
                            onSuccess(scannerResults)
                        }
                        else if (cipher.startsWith("IDKHM", true))
                        {
                            onSuccess(scannerResults)
                            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.NATIONALITY_CARD))
                        }
                        else
                        {
                            onSuccess(scannerResults)
                            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.INVALID_DOCUMENT))
                        }
                    }
                    .addOnFailureListener {
                        onSuccess(scannerResults)
                        onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.ERROR_RECOGNIZE))
                    }
        }
        else
        {
            onError(ErrorScannerModel(ErrorScannerModel.ErrorMessage.INVALID_IMAGE))
        }
    }
}