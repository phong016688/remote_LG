package com.mentos_koder.remote_lg_tv.util

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.mentos_koder.remote_lg_tv.model.AudioFile
import com.mentos_koder.remote_lg_tv.model.ImageFolder
import com.mentos_koder.remote_lg_tv.model.VideoFolder
import java.io.File
import java.util.Locale

object MediaManager {

    fun getAllPhotoFolders(): List<ImageFolder> {
        val photoFolders = mutableListOf<ImageFolder>()
        val dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val dcimFiles = dcimFolder.listFiles() ?: return emptyList()

        dcimFiles.forEach { folder ->
            if (folder.isDirectory) {
                val imageFiles = folder.listFiles { _, name ->
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                }
                val imageList = imageFiles?.toList() ?: emptyList()
                val imageFolder = ImageFolder(folder.name, imageList, imageList.size, folder.absolutePath)
                photoFolders.add(imageFolder)

                folder.listFiles()?.forEach { subFolder ->
                    if (subFolder.isDirectory) {
                        val subImageFiles = subFolder.listFiles { _, name ->
                            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                        }
                        val subImageList = subImageFiles?.toList() ?: emptyList()
                        val subImageFolder = ImageFolder(
                            subFolder.name,
                            subImageList,
                            subImageList.size,
                            subFolder.absolutePath
                        )
                        photoFolders.add(subImageFolder)
                    }
                }
            }
        }
        photoFolders.sortByDescending { it.quantity }
        return photoFolders
    }

    fun getVideoFromAppStorage(folderAbsolutePath: String): ArrayList<File> {
        val videos = ArrayList<File>()
        val folder = File(folderAbsolutePath)
        if (folder.exists() && folder.isDirectory) {
            val videoFiles = folder.listFiles()
            videoFiles?.forEach { file ->
                if (file.isFile && file.extension.lowercase(Locale.ROOT) in listOf("mp4", "avi", "mkv")) {
                    // Thêm tệp video vào danh sách
                    videos.add(file)
                }
            }
        }
        return videos
    }

    fun getAllVideoFolders(): ArrayList<VideoFolder> {
        val videoFolders = ArrayList<VideoFolder>()
        val dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val dcimPath = dcimFolder.absolutePath

        val dcimFile = File(dcimPath)
        if (dcimFile.exists() && dcimFile.isDirectory) {
            val subFolders = dcimFile.listFiles()
            subFolders?.forEach { folder ->
                if (folder.isDirectory) {
                    val videoFiles = folder.listFiles { _, name ->
                        name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv")
                    }
                    val videoList = videoFiles?.toList() ?: emptyList()
                    if (videoList.isNotEmpty()) {
                        val videoFolder = VideoFolder(folder.name, videoList, videoList.size, folder.absolutePath)
                        videoFolders.add(videoFolder)
                    }

                    val subSubFolders = folder.listFiles()
                    subSubFolders?.forEach { subFolder ->
                        if (subFolder.isDirectory) {
                            val subVideoFiles = subFolder.listFiles { _, name ->
                                name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv")
                            }
                            val subVideoList = subVideoFiles?.toList() ?: emptyList()
                            if (subVideoList.isNotEmpty()) {
                                val subVideoFolder = VideoFolder(
                                    subFolder.name,
                                    subVideoList,
                                    subVideoList.size,
                                    subFolder.absolutePath
                                )
                                videoFolders.add(subVideoFolder)
                            }
                        }
                    }
                }
            }
        }
        videoFolders.sortByDescending { it.quantity }
        return videoFolders
    }
    fun loadAudioFiles(context: Context): List<AudioFile> {
        val audioList = mutableListOf<AudioFile>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST
        )

        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val duration = it.getLong(durationColumn)
                val data = it.getString(dataColumn)
                val albumId = it.getLong(albumIdColumn)
                val artist = it.getString(artistColumn)
                val albumArt = getAlbumArt(context, albumId)
                audioList.add(AudioFile(id, name, duration, data, albumArt,artist))
            }
        }
        return audioList
    }
    private fun getAlbumArt(context: Context, albumId: Long): String? {
        val albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
        val selection = "${MediaStore.Audio.Albums._ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())

        val cursor = context.contentResolver.query(
            albumUri,
            projection,
            selection,
            selectionArgs,
            null
        )

        var albumArt: String? = null
        cursor?.use {
            if (it.moveToFirst()) {
                albumArt = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
                Log.d("MediaManager", "Album Art Path: $albumArt")
            }
        }
        return albumArt
    }
}
