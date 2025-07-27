package com.mentos_koder.remote_lg_tv.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class PhotoViewModel:ViewModel() {
    private val _mutableLiveDataImageFolder=MutableLiveData<List<File>>()
    val livedataImageFolder:LiveData<List<File>> =_mutableLiveDataImageFolder
    fun setImageFolder(listPath: List<File>){
        _mutableLiveDataImageFolder.value=listPath
    }
}