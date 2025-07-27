package com.mentos_koder.remote_lg_tv.event

import com.mentos_koder.remote_lg_tv.model.ImageFolder

interface OnFolderClickListener {
    fun onFolderClick(imageFolder: ImageFolder)
}
interface OnFolderClickListenerVideo {
    fun onFolderClick(pathVideoFolder: String)
}
