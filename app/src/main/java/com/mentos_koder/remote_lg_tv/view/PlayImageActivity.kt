package com.mentos_koder.remote_lg_tv.view

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.connectsdk.core.Util
import com.connectsdk.service.upnp.DLNAHttpServer
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.UtilsHttp


class PlayImageActivity : AppCompatActivity() {
    private lateinit var imgViewPhoto: ImageView
    private lateinit var seekbarTimePlay: SeekBar
    private lateinit var backward: LinearLayout
    private lateinit var tvQuantityImg: TextView
    private lateinit var forward: LinearLayout
    private lateinit var play: LinearLayout
    private lateinit var imgShare: ImageView
    private lateinit var imgBack: ImageView
    private lateinit var toolbar: Toolbar
    private val Duration = 8000
    private val UPDATE_INTERVAL = 1000
    private lateinit var countDownTimer: CountDownTimer
    private var currentIndex: Int = 0
    var position = 0
    var path = ""
    private lateinit var pathList: List<String>
    private var isCheckVisibleToolbar = false
    private var isCheckPlay = false
    private var server = DLNAHttpServer()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_image)
        setUI()
        observeBitmap()
        setEvent()
    }

    private fun setUI() {
        imgViewPhoto = findViewById(R.id.img_viewPhoto)
        seekbarTimePlay = findViewById(R.id.seekbar_time_play)
        seekbarTimePlay.visibility = View.GONE
        backward = findViewById(R.id.liner_backward)
        forward = findViewById(R.id.liner_forward)
        play = findViewById(R.id.liner_resume)
        toolbar = findViewById(R.id.toolbar)
        imgBack = findViewById(R.id.img_back)
        tvQuantityImg = findViewById(R.id.tv_quantity_img)
        imgShare = findViewById(R.id.img_share)
    }

    private fun observeBitmap() {
        server = DLNAHttpServer()
        position = intent.getIntExtra("position", 0)
        Log.d("nextSocket", "observeBitmap: " + position)
        path = intent.getStringExtra("path").toString()
        pathList = Singleton.getInstance().getPathList()
        Log.d("observeBitmap", "nextSocket: $position")
        Log.d("pathList", "nextSocket: " + pathList.size)
        Log.d("pathList", "nextSocket: " +  pathList.getOrNull(position))
       // setImage(imgViewPhoto,pathList[position])
        val path = "file://$path"
        Log.d("nextSocket", "setImage: " + path)
        Glide.with(this).load(path).into(imgViewPhoto)
        if (!server.isRunning) {
            Util.runInBackground {
                server.start(path,this)
                Log.d("#DLNA", "run: addSubscription ")
            }
        }
        val ipAddress = getLocalIpAddress()
        UtilsHttp.setIpAddress(ipAddress)
        Singleton.getInstance().showMediaImage(UtilsHttp.mediaURL,path,UtilsHttp.mimeType,UtilsHttp.title,UtilsHttp.iconURL,UtilsHttp.description)

    }

    fun setImage(img : ImageView,url : String){
        val path = "file://$url"
        Log.d("nextSocket", "setImage: " + path)
        Glide.with(this).load(path).into(img)
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
    override fun onDestroy() {
        super.onDestroy()
        if (server.isRunning){
            server.stop()
        }
    }
    private fun nextSocket(pathLocation :String){
        Log.d("nextSocket", "nextSocket: " + pathLocation)
        setImage(imgViewPhoto,pathLocation)
        if (!server.isRunning) {
            Util.runInBackground {
                val ipAddress = getLocalIpAddress()
                UtilsHttp.setIpAddress(ipAddress)
                server.start(pathLocation,this)
                Singleton.getInstance().showMediaImage(UtilsHttp.mediaURL,pathLocation,UtilsHttp.mimeType,UtilsHttp.title,UtilsHttp.iconURL,UtilsHttp.description)
                Log.d("#DLNA", "run: addSubscription ")
            }
        }else{
            val ipAddress = getLocalIpAddress()
            UtilsHttp.setIpAddress(ipAddress)
            server.stop()
            server.start(pathLocation,this)
            Singleton.getInstance().showMediaImage(UtilsHttp.mediaURL,pathLocation,UtilsHttp.mimeType,UtilsHttp.title,UtilsHttp.iconURL,UtilsHttp.description)
            Log.d("#DLNA", "run: nextSocket $pathLocation")

        }
    }
    private fun setEvent() {
        imgBack.setOnClickListener {onBackPressed() }
        currentIndex = position
        Log.d("nextSocket", "setEvent: " + position)
        play.setOnClickListener {
            if (!isCheckPlay){
                seekbarTimePlay.visibility = View.VISIBLE
                val ipAddress = getLocalIpAddress()
                UtilsHttp.setIpAddress(ipAddress)
                setTimeSeekbar()
                isCheckPlay = true
            }else{
                seekbarTimePlay.visibility = View.GONE
                countDownTimer.cancel()
                isCheckPlay = false
            }

        }
        backward.setOnClickListener {
            Log.d("nextSocket", "setEvent back: " + currentIndex)
            if (currentIndex > 0) {
                setQuantityImage(currentIndex)
                currentIndex--
                nextSocket(pathList[currentIndex])
            }
        }
        forward.setOnClickListener {
            Log.d("nextSocket", "setEvent forward: " + currentIndex)
            if (currentIndex < pathList.size - 1) {
                setQuantityImage(currentIndex)
                currentIndex++
                nextSocket(pathList[currentIndex])
            }
        }

        imgViewPhoto.setOnClickListener {
            if (!isCheckVisibleToolbar) {
                toolbar.visibility = View.GONE
                isCheckVisibleToolbar = true
            } else {
                toolbar.visibility = View.VISIBLE
                isCheckVisibleToolbar = false
            }
        }
    }
    private fun setQuantityImage(positionCurrent : Int){
        val currentPosition = positionCurrent + 1
        val positionMax = pathList.size
        tvQuantityImg.text =  "$currentPosition of $positionMax"
    }
    private fun setTimeSeekbar() {
        seekbarTimePlay.max = Duration
        countDownTimer =
            object : CountDownTimer(Duration.toLong(), UPDATE_INTERVAL.toLong()) {
                override fun onTick(millisUntilFinished: Long) {
                    seekbarTimePlay.progress = (Duration - millisUntilFinished).toInt()
                }

                override fun onFinish() {
                    if (currentIndex < pathList.size - 1) {
                        currentIndex++
                        setImage(imgViewPhoto,pathList[currentIndex])
                        val path = pathList[currentIndex]
                        nextSocket(path)
                        setQuantityImage(currentIndex)
                        setTimeSeekbar()
                    } else {
                        seekbarTimePlay.visibility = View.GONE
                    }
                }
            }
        countDownTimer.start()
    }
}
