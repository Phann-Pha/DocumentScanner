package com.pha.document.scanner.common.documentscanner.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.pha.document.scanner.R

internal class ProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr)
{
    init
    {
        LayoutInflater.from(context).inflate(R.layout.progress_layout, this, true)
    }
}
