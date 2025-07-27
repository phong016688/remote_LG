package com.mentos_koder.remote_lg_tv.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat

object ImageLoader {

    fun loadImageFromMipmap(context: Context, imageName: String): Drawable? {
        val resourceId = context.resources.getIdentifier(imageName, "mipmap", context.packageName)
        return if (resourceId != 0) {
            ResourcesCompat.getDrawable(context.resources, resourceId, null)
        } else {
            null
        }
    }

}