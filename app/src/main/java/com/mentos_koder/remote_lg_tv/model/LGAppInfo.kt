package com.mentos_koder.remote_lg_tv.model

data class LGAppInfo(
    val id: String,
    val name: String,
    val running: Boolean,
    val version: String,
    val visible: Boolean
)