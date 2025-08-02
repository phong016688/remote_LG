package com.mentos_koder.remote_lg_tv.view.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.Main
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.FolderVideoAdapter
import com.mentos_koder.remote_lg_tv.adapter.VideoAdapter
import com.mentos_koder.remote_lg_tv.event.OnFolderClickListenerVideo
import com.mentos_koder.remote_lg_tv.event.OnMediaClickListener
import com.mentos_koder.remote_lg_tv.util.MediaManager
import com.mentos_koder.remote_lg_tv.util.clicks
import com.mentos_koder.remote_lg_tv.view.MainActivity
import com.mentos_koder.remote_lg_tv.view.PlayVideoActivity
import java.io.File

class VideoFragment : Fragment() {
    private lateinit var videoRecyclerView: RecyclerView
    private lateinit var folderVideoAdapter: FolderVideoAdapter
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var backButton: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_video, container, false)
        videoRecyclerView = rootView.findViewById(R.id.recycler_video)
        backButton = rootView.findViewById(R.id.img_back)
        rootView.findViewById<ImageView>(R.id.img_cast).clicks {
            (activity as? MainActivity)?.showFragmentDevice()
        }
        initializeVideoFolders()
        folderVideoAdapter.setOnFolderClickListener(object : OnFolderClickListenerVideo {
            override fun onFolderClick(videoFolderPath: String) {
                val videos = MediaManager.getVideoFromAppStorage(videoFolderPath)
                displayVideos(videos, videoFolderPath)
            }
        })
        backButton.setOnClickListener{
            requireActivity().supportFragmentManager.popBackStack()
        }
        return rootView
    }

    private fun initializeVideoFolders() {
        val videoFolders = MediaManager.getAllVideoFolders()
        videoRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        folderVideoAdapter = FolderVideoAdapter(requireContext(), videoFolders)
        videoRecyclerView.adapter = folderVideoAdapter
    }

    private fun displayVideos(videos: ArrayList<File>, folderPath: String) {
        videoRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        videoAdapter = VideoAdapter(videos, requireContext())
        videoRecyclerView.adapter = videoAdapter
        videoAdapter.notifyDataSetChanged()
        videoAdapter.setOnVideoClickListener(object : OnMediaClickListener {
            override fun onMediaClick(position: Int, path: String) {
            }

            override fun onMediaClick(position: Int) {
                val intent = Intent(requireContext(), PlayVideoActivity::class.java)
                intent.putExtra("position", position)
                intent.putExtra("path", folderPath)
                startActivity(intent)
            }

        })
    }

}