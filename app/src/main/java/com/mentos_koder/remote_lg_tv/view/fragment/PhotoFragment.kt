package com.mentos_koder.remote_lg_tv.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.FolderImageAdapter
import com.mentos_koder.remote_lg_tv.event.OnFolderClickListener
import com.mentos_koder.remote_lg_tv.model.ImageFolder
import com.mentos_koder.remote_lg_tv.util.MediaManager


class PhotoFragment : Fragment() {

    private lateinit var recyclerPhoto : RecyclerView
    private lateinit var folderImageAdapter: FolderImageAdapter
    private lateinit var backButton: ImageView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_photo, container, false)
        recyclerPhoto = rootView.findViewById(R.id.recycler_photo)
        backButton = rootView.findViewById(R.id.img_back)
        loadPhotoFolders()
        folderImageAdapter.setOnFolderClickListener(object : OnFolderClickListener{
            override fun onFolderClick(imageFolder: ImageFolder) {
                loadImagesForFolder(imageFolder)
            }
        })
        backButton.setOnClickListener{
            requireActivity().supportFragmentManager.popBackStack()
        }
        return rootView
    }
    private fun loadPhotoFolders() {
        val imageFolders= MediaManager.getAllPhotoFolders()
        recyclerPhoto.layoutManager = GridLayoutManager(requireContext(), 2)
        folderImageAdapter = FolderImageAdapter(requireContext(),imageFolders)
        recyclerPhoto.adapter = folderImageAdapter
        folderImageAdapter.notifyDataSetChanged()
    }

    private fun loadImagesForFolder(imageFolder: ImageFolder) {
        val imageList=imageFolder.imageList
        val imagePathList: List<String> = imageList.map { it.path }
        val fragment = PhotoFragmentDetail.newInstance(imagePathList)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}