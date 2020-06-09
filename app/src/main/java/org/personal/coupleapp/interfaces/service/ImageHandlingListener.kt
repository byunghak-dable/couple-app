package org.personal.coupleapp.interfaces.service

import android.graphics.Bitmap

interface ImageHandlingListener {
    fun onSingleImage(bitmap: Bitmap)
    fun onMultipleImage(bitmapList: ArrayList<Bitmap?>)
}