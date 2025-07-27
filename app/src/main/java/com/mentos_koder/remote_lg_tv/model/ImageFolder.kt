package com.mentos_koder.remote_lg_tv.model

import java.io.File
import java.io.Serializable

data class ImageFolder(val folderName: String, var imageList: List<File>, val quantity: Int, val folderPath: String) :
    Serializable
