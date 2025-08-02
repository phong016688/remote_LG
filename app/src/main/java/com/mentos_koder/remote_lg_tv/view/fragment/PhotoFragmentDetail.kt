package com.mentos_koder.remote_lg_tv.view.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.PhotoAdapter
import com.mentos_koder.remote_lg_tv.event.OnMediaClickListener
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.clicks
import com.mentos_koder.remote_lg_tv.view.MainActivity
import com.mentos_koder.remote_lg_tv.view.PlayImageActivity
import com.mentos_koder.remote_lg_tv.viewmodel.PhotoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PhotoFragmentDetail : Fragment() {
    private lateinit var backButton: ImageView
    private lateinit var recyclerPhoto: RecyclerView
    private lateinit var adapterPhoto: PhotoAdapter
    private lateinit var imageList: List<String>
    private lateinit var photoViewModel: PhotoViewModel
    private val photo = mutableListOf<String>()
    private var isDataLoaded = false
    private var isDataLoadedR = false

    companion object {
        private const val ARG_FOLDER_PATH = "folder_path"

        fun newInstance(imageList: List<String>): PhotoFragmentDetail {
            val fragment = PhotoFragmentDetail()
            val args = Bundle()
            val jsonImageList = Gson().toJson(imageList)
            args.putString(ARG_FOLDER_PATH, jsonImageList)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_photo_view, container, false)
        recyclerPhoto = rootView.findViewById(R.id.recycler_photo)
        photoViewModel = ViewModelProvider(requireActivity())[PhotoViewModel::class.java]
        backButton = rootView.findViewById(R.id.img_back)
        rootView.findViewById<ImageView>(R.id.img_back).clicks {
            (activity as? MainActivity)?.showFragmentDevice()
        }
        val imageListJson = arguments?.getString(ARG_FOLDER_PATH) ?: ""
        val type = object : TypeToken<List<String>>() {}.type
        imageList = Gson().fromJson(imageListJson, type)
        adapterPhoto = PhotoAdapter(photo, requireContext())
        adapterPhoto.setOnImageClickListener(object : OnMediaClickListener {
            override fun onMediaClick(position: Int, path: String) {
                val intent = Intent(requireContext(), PlayImageActivity::class.java).apply {
                    putExtra("position", position)
                    putExtra("path", path)
                }
                startActivity(intent)
            }

            override fun onMediaClick(position: Int) {
            }

        })
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        rootView.findViewById<ImageView>(R.id.img_cast).setOnClickListener {
            (activity as? MainActivity)?.showFragmentDevice()
        }
        recyclerPhoto.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerPhoto.adapter = adapterPhoto
//        val imageListFile: List<File> = imageList.map { File(it) }
//        imageListFile.forEach {image->
//            lifecycleScope.launch {
//                image.checkAndRotateIfNeeded(requireContext())
//            }
//        }
        if (!isDataLoaded) {
            imageList.forEach { path ->
                if (!isDataLoadedR) {
                    adapterPhoto.addPhoto(path)
                }
            }
            isDataLoaded = true
        }
        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Singleton.getInstance()._pathList.clear()
    }

    private suspend fun File.checkAndRotateIfNeeded(context: Context): String? = try {
        val exif = ExifInterface(absolutePath)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationNeeded = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationNeeded != 0f) rotateImageAndGetPath(rotationNeeded, context) else absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }

    private suspend fun File.rotateImageAndGetPath(degrees: Float, context: Context): String? {
        if (!exists()) return null
        val tempDir = context.cacheDir
        val rotatedFileName = if (tempDir.listFiles()?.any { it.name.contains(name) } == true) {
            tempDir.listFiles()?.firstOrNull { it.name.contains(name) }?.absolutePath ?: return null
        } else {
            File.createTempFile("temp_${name}", ".jpg", tempDir).absolutePath
        }
        withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(absolutePath) ?: return@withContext
                val rotatedBitmap = bitmap.rotateBitmap(degrees)
                val outputStream = FileOutputStream(rotatedFileName)
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        // Thêm ảnh vào adapterPhoto trên luồng giao diện người dùng
        withContext(Dispatchers.Main) {
            isDataLoadedR = true
            adapterPhoto.addPhoto(rotatedFileName)
        }
        return rotatedFileName
    }

    private fun Bitmap.rotateBitmap(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}