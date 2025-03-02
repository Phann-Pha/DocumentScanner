package com.pha.document.scanner.common.documentscanner.common.extensions

import android.view.View

internal fun View.hide()
{
    visibility = View.GONE
}

internal fun View.show()
{
    visibility = View.VISIBLE
}