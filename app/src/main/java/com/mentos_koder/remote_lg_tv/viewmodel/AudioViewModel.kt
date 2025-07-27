package com.mentos_koder.remote_lg_tv.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mentos_koder.remote_lg_tv.model.AudioFile

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val selectedAudio = MutableLiveData<AudioFile>()
    private val _audioList = MutableLiveData<List<AudioFile>>()
    val audioList: LiveData<List<AudioFile>> get() = _audioList
    fun setSelectedAudio(audioFile: AudioFile) {
        Log.d("audioA##", "setSelectedAudio: $audioFile")
        selectedAudio.value = audioFile
    }

    fun getSelectedAudio(): LiveData<AudioFile> {
        Log.d("audioA##", "getSelectedAudio: ${selectedAudio.value}")
        return selectedAudio
    }
    fun setAudioList(newAudioList: List<AudioFile>) {
        Log.d("AudioViewModel", "setAudioList: $newAudioList")
        _audioList.value = newAudioList
    }
}




