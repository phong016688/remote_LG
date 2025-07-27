package com.mentos_koder.remote_lg_tv.model

import java.io.Serializable

data class AudioFile(
    val id: Long,
    val name: String,
    val duration: Long,
    val data: String,
    val albumArt: String?,
    val artist: String?
) : Serializable
