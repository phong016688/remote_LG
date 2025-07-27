package com.mentos_koder.remote_lg_tv.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.AudioAdapter
import com.mentos_koder.remote_lg_tv.model.AudioFile
import com.mentos_koder.remote_lg_tv.viewmodel.AudioViewModel
import com.mentos_koder.remote_lg_tv.util.MediaManager

class AudioListFragment : Fragment(){
    private lateinit var audioRecyclerView: RecyclerView
    private lateinit var audioAdapter: AudioAdapter
    private var audioList: List<AudioFile> = listOf()
    private val viewModel: AudioViewModel by activityViewModels()
    private lateinit var imgBack: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_audio_list, container, false)
        audioRecyclerView = view.findViewById(R.id.recycler_audio)
        audioRecyclerView.layoutManager = LinearLayoutManager(context)
        context?.let {
            audioList = MediaManager.loadAudioFiles(it)
        }
        audioAdapter = context?.let { AudioAdapter(audioList,viewModel, it) }!!
        viewModel.setAudioList(audioList)
        audioRecyclerView.adapter = audioAdapter

        imgBack = view.findViewById(R.id.img_back)
        imgBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        return view
    }
}