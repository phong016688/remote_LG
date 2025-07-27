package com.mentos_koder.remote_lg_tv.event


interface  OnMediaClickListener {
    fun onMediaClick(position: Int,path : String)
    fun onMediaClick(position: Int)
}