package com.pha.document.scanner

import android.app.Activity
import android.os.Bundle
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.pha.document.scanner.databinding.ActivityMainBinding
import com.pha.document.scanner.common.documentscanner.BaseScannerActivity
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.model.ScannerResults

class MainActivity : BaseScannerActivity()
{
    private lateinit var activity: Activity
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        activity = this
        binding = DataBindingUtil.setContentView(activity, R.layout.activity_main)
        addFragmentContentLayout()
    }
    
    override fun onError(error: ErrorScannerModel)
    {
    
    }
    
    override fun onSuccess(scannerResults: ScannerResults)
    {
        binding.imagePreview.setImageURI(scannerResults.croppedImageFile?.toUri())
    }
    
    override fun onClose()
    {
        finish()
    }
}