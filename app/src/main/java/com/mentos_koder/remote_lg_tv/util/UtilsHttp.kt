package com.mentos_koder.remote_lg_tv.util

class UtilsHttp {

    companion object {
        private var ipAddress: String? = null
        private  var _titleAudio: String? = null
        private  var _descriptionAudio: String? = null
        fun setIpAddress(ip: String) {
            ipAddress = ip
        }

        val mediaURL: String
            get() = "http://$ipAddress:5555"
        fun setTitleNameAudio(title : String){
            _titleAudio = title
        }
        fun setArtist(description : String){
            _descriptionAudio = description
        }
        val iconURL: String
            get() = "http://$ipAddress:5555"
        var title = "Display Image To Device"
        var titleVideo = "Display Video To Device"
        var titleAudio = _titleAudio
        var description = "Show Image"
        var descriptionAudio = _descriptionAudio
        var mimeType = "image/jpeg"
        var mimeTypeVideo = "video/mp4"
        var minTypeAudio = " audio/mp3"
    }

}
