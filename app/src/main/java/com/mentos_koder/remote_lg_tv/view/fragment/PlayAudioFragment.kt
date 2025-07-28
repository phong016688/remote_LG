package com.mentos_koder.remote_lg_tv.view.fragment

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.connectsdk.core.Util
import com.connectsdk.service.upnp.DLNAHttpServer
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.model.AudioFile
import com.mentos_koder.remote_lg_tv.viewmodel.AudioViewModel
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.UtilsHttp
import java.io.File

class PlayAudioFragment : Fragment() {
    private lateinit var seekbar_time_play: SeekBar
    private lateinit var img_backward: ImageView
    private lateinit var tv_quantity_img: TextView
    private lateinit var img_forward: ImageView
    private lateinit var img_play: ImageView
    private lateinit var img_share: ImageView
    private lateinit var img_view_audio: ImageView
    private lateinit var img_back: ImageView
    private lateinit var toolbar: Toolbar
    private var server = DLNAHttpServer()
    private val viewModel: AudioViewModel by activityViewModels()
    private var audioList: List<AudioFile> = listOf()
    var position = 0
    private var currentIndex: Int = 0
    var isCheckVisibleToolbar = false
    var isCheckPlay = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_play_audio, container, false)
        setUI(view)
        viewModel.audioList.observe(viewLifecycleOwner, Observer { list ->
            Log.d("audioAList##", "onCreate: $list")
            audioList = list
        })
        server = DLNAHttpServer()
        showAudio()
        return view
    }

    private fun showAudio() {
        viewModel.getSelectedAudio().observe(viewLifecycleOwner) { audioFile ->
            if (audioFile != null) {
                val imgViewAudio = audioFile.albumArt
                tv_quantity_img.text = audioFile.name
                if (imgViewAudio != null) {
                    context?.let {
                        Glide.with(it).load(File(imgViewAudio)).apply(
                            RequestOptions().placeholder(R.drawable.ic_app)
                                .error(R.drawable.setting)
                        ).diskCacheStrategy(DiskCacheStrategy.ALL).into(img_view_audio)
                    }
                } else {
                    img_view_audio.setImageResource(R.drawable.ic_audio)
                }
                position = audioList.indexOfFirst { it.id == audioFile.id }
                playAudio(audioFile)
                setEvent()
            }
        }
    }

    private fun startConnect(audioFile: AudioFile, title: String, description: String) {
        if (!server.isRunning) {
            Util.runInBackground {
                server.start(audioFile.data, context)
                Singleton.getInstance().showMediaVideoAndAudio(
                    UtilsHttp.mediaURL,
                    audioFile.data,
                    UtilsHttp.minTypeAudio,
                    title,
                    UtilsHttp.iconURL,
                    description
                )
            }

        } else {
            server.stop()
            server.start(audioFile.data, context)
            Singleton.getInstance().showMediaVideoAndAudio(
                UtilsHttp.mediaURL,
                audioFile.data,
                UtilsHttp.minTypeAudio,
                title,
                UtilsHttp.iconURL,
                description
            )
        }
    }

    private fun playAudio(audioFile: AudioFile) {
        val ipAddress = getLocalIpAddress()
        val title = audioFile.name
        var description = audioFile.artist
        UtilsHttp.setIpAddress(ipAddress)
        if (description == null) {
            description = "<unknown>"
        }
        startConnect(audioFile, title, description)
    }

    private fun getListAudioForPosition(pos: Int): AudioFile {
        Log.d("audioA##", "getListAudioForPosition: " + audioList[pos])
        tv_quantity_img.text = audioList[pos].name
        return audioList[pos]
    }

    private fun setEvent() {
        val ipAddress = getLocalIpAddress()
        UtilsHttp.setIpAddress(ipAddress)
        currentIndex = position
        Log.d("audioA", "setEvent currentIndex: $currentIndex ")
        Log.d("audioA", "setEvent position: $position ")
        img_back.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        img_play.setOnClickListener {
            if (isCheckPlay) {
                Singleton.getInstance().playAudio()
                isCheckPlay = false
            } else {
                Singleton.getInstance().stopAudio()
                isCheckPlay = true
            }

        }
        img_backward.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                Log.d("audioA", "setEvent: " + currentIndex)
                val audio = getListAudioForPosition(currentIndex)
                playAudio(audio)
            }
        }
        img_forward.setOnClickListener {
            if (currentIndex < audioList.size - 1) {
                currentIndex++
                Log.d("audioA", "setEvent: " + currentIndex)
                val audio = getListAudioForPosition(currentIndex)
                playAudio(audio)
            }
        }
        img_view_audio.setOnClickListener {
            if (!isCheckVisibleToolbar) {
                toolbar.visibility = View.GONE
                isCheckVisibleToolbar = true
            } else {
                toolbar.visibility = View.VISIBLE
                isCheckVisibleToolbar = false
            }
        }
    }

    private fun setUI(view: View) {
        img_view_audio = view.findViewById(R.id.img_view_audio)
        seekbar_time_play = view.findViewById(R.id.seekbar_time_play)
        seekbar_time_play.visibility = View.GONE
        img_backward = view.findViewById(R.id.img_backward)
        img_forward = view.findViewById(R.id.img_forward)
        img_play = view.findViewById(R.id.img_play)
        toolbar = view.findViewById(R.id.toolbar)
        img_back = view.findViewById(R.id.img_back)
        tv_quantity_img = view.findViewById(R.id.tv_quantity_img)
        img_share = view.findViewById(R.id.img_share)
    }

    private fun getLocalIpAddress(): String {
        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.getIpAddress()
        return String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (server.isRunning) {
            server.stop()
        }
    }
}