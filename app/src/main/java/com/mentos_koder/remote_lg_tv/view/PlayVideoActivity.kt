package com.mentos_koder.remote_lg_tv.view

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toolbar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.connectsdk.core.Util
import com.connectsdk.service.upnp.DLNAHttpServer
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.MediaManager
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.UtilsHttp
import java.io.File

class PlayVideoActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var seekbar_time_play: SeekBar
    private lateinit var img_backward: ImageView
    private lateinit var tv_quantity_img: TextView
    private lateinit var img_forward: ImageView
    private lateinit var img_play: ImageView
    private lateinit var img_share: ImageView
    private lateinit var img_back: ImageView
    private lateinit var toolbar: Toolbar
    private val TOTAL_DURATION = 8000
    private val UPDATE_INTERVAL = 1000
    private lateinit var countDownTimer: CountDownTimer
    private var currentIndex: Int = 0
    var position = 0
    var path = ""
    var isCheckVisibleToolbar = false
    var isCheckPlay = true
    var videoFilesList = ArrayList<File>()
    private var server = DLNAHttpServer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        setUI()
        setEvent()
        handle()
    }

    private fun setUI() {
        videoView = findViewById(R.id.video_viewVideo)
        seekbar_time_play = findViewById(R.id.seekbar_time_play)
        seekbar_time_play.visibility = View.GONE
        img_backward = findViewById(R.id.img_backward)
        img_forward = findViewById(R.id.img_forward)
        img_play = findViewById(R.id.img_play)
        toolbar = findViewById(R.id.toolbar)
        img_back = findViewById(R.id.img_back)
        tv_quantity_img = findViewById(R.id.tv_quantity_img)
        img_share = findViewById(R.id.img_share)
    }

    fun handle() {
        server = DLNAHttpServer()
        position = intent.getIntExtra("position", 0)
        path = intent.getStringExtra("path").toString()
        playVideoInVideoView(videoView, position, path)
        setQuantityVideo(position)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (server.isRunning) {
            server.stop()
        }
    }
    fun playVideoInVideoView(videoView: VideoView, position: Int, linkPath: String) {
        videoFilesList = MediaManager.getVideoFromAppStorage(linkPath)
        if (position >= 0 && position < videoFilesList.size) {
            val videoFile = videoFilesList[position]
            val videoUri = Uri.fromFile(videoFile)
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, videoUri)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotation = rotationStr?.toInt() ?: 0
            retriever.release()
            Log.d("VideoInfo", "Path: $videoUri")
            Log.d("VideoInfo", "Rotation: $rotation")
            videoView.setVideoURI(videoUri)
            if (!server.isRunning()) {
                Util.runInBackground {
                    server.start(videoFile.toString(), this)
                    //videoView.start()
                    Singleton.getInstance().showMediaVideoAndAudio(
                        UtilsHttp.mediaURL,
                        videoFilesList[position].absolutePath,
                        UtilsHttp.mimeTypeVideo,
                        UtilsHttp.titleVideo,
                        UtilsHttp.iconURL,
                        UtilsHttp.description
                    )
                }

            }else{
                Singleton.getInstance().showMediaVideoAndAudio(
                    UtilsHttp.mediaURL,
                    videoFilesList[position].absolutePath,
                    UtilsHttp.mimeTypeVideo,
                    UtilsHttp.titleVideo,
                    UtilsHttp.iconURL,
                    UtilsHttp.description
                )
            }

        } else {
            Log.e("VideoManager", "Invalid position")
        }
    }

    private fun nextVideoSocket(pathLocation: String) {
        val ipAddress = getLocalIpAddress()
        UtilsHttp.setIpAddress(ipAddress)
        if (!server.isRunning()) {
            Util.runInBackground {
                server.start(pathLocation, this)
                Singleton.getInstance().showMediaVideoAndAudio(
                    UtilsHttp.mediaURL,
                    pathLocation,
                    UtilsHttp.mimeTypeVideo,
                    UtilsHttp.titleVideo,
                    UtilsHttp.iconURL,
                    UtilsHttp.description
                )
                Log.d("#DLNA", "run: addSubscription ")
            }
        } else {
            Util.runInBackground {
                Singleton.getInstance().showMediaVideoAndAudio(
                    UtilsHttp.mediaURL,
                    pathLocation,
                    UtilsHttp.mimeTypeVideo,
                    UtilsHttp.titleVideo,
                    UtilsHttp.iconURL,
                    UtilsHttp.description)
            }
            Log.d("#DLNA", "run: nextSocket " + pathLocation)

        }
    }
    fun getLocalIpAddress(): String {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.getIpAddress()
        return String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff, ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff
        )
    }
    private fun setEvent() {
        val ipAddress = getLocalIpAddress()
        UtilsHttp.setIpAddress(ipAddress)
        img_back.setOnClickListener {
            onBackPressed()
        }
        currentIndex = position
        img_play.setOnClickListener {
            if(isCheckPlay) {
                videoView.pause();
                seekTime()
                isCheckPlay = false
            }else {
                countDownTimer.cancel()
                videoView.start()
                seekTime()
                isCheckPlay = true
            }

        }
        img_backward.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                setQuantityVideo(currentIndex)
                skipVideo(currentIndex)
                nextVideoSocket(videoFilesList[currentIndex].absolutePath)
            }
        }
        img_forward.setOnClickListener {
            if (currentIndex < videoFilesList.size - 1) {
                currentIndex++
                setQuantityVideo(currentIndex)
                skipVideo(currentIndex)
                nextVideoSocket(videoFilesList[currentIndex].absolutePath)
            }
        }

        videoView.setOnClickListener {
            if (!isCheckVisibleToolbar) {
                toolbar.visibility = View.GONE
                isCheckVisibleToolbar = true
            } else {
                toolbar.visibility = View.VISIBLE
                isCheckVisibleToolbar = false
            }
        }
    }

    private fun setQuantityVideo(positionCurrent: Int) {
        val currentPosition = positionCurrent + 1
        val positionMax = videoFilesList.size
        tv_quantity_img.text = "$currentPosition of $positionMax"
    }

    private fun seekTime() {
        seekbar_time_play.max = TOTAL_DURATION

        countDownTimer =
            object : CountDownTimer(TOTAL_DURATION.toLong(), UPDATE_INTERVAL.toLong()) {
                override fun onTick(millisUntilFinished: Long) {
                    val currentPosition = videoView.currentPosition
                    seekbar_time_play.progress = currentPosition
                }

                override fun onFinish() {
                    if (currentIndex < videoFilesList.size - 1) {
                        currentIndex++
                       // skipVideo(currentIndex)
                        setQuantityVideo(currentIndex)
                        seekTime()
                    } else {
                        seekbar_time_play.visibility = View.GONE
                    }
                }
            }

        countDownTimer.start()
    }

    private fun skipVideo(index: Int) {
        val videoFile = videoFilesList[index]
        val videoUri = Uri.fromFile(videoFile)
        videoView.setVideoURI(videoUri)
        videoView.start()
        Log.d("skipVideo", "skipVideo: " + videoFile.absolutePath)
    }
}
