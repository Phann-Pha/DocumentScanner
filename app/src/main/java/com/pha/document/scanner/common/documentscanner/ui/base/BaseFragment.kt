package com.pha.document.scanner.common.documentscanner.ui.base

import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.pha.document.scanner.R
import com.pha.document.scanner.common.documentscanner.common.extensions.hide
import com.pha.document.scanner.common.documentscanner.common.extensions.show

internal abstract class BaseFragment : Fragment()
{
    fun showProgressBar()
    {
        view?.findViewById<RelativeLayout>(R.id.progressLayout)?.show()
    }
    
    fun hideProgressBar()
    {
        view?.findViewById<RelativeLayout>(R.id.progressLayout)?.hide()
    }
}