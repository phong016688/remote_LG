package com.mentos_koder.remote_lg_tv.model

import java.io.File

data class VideoFolder(
    val folderName: String,
    val videoList: List<File>,
    val quantity: Int,
    val folderPath: String
)
