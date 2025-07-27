package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.event.OnMediaClickListener
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.fragment.DeviceFragment
import java.io.File

class VideoAdapter(private val videoFiles: List<File>, val context: Context) :
    RecyclerView.Adapter<VideoAdapter.ImageViewHolder>() {
    private var onClick: OnMediaClickListener? = null
    fun setOnVideoClickListener(listener: OnMediaClickListener) {
        onClick = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val videoFile = videoFiles[position]
        val thumbnail = getVideoThumbnail(videoFile)
        thumbnail?.let {
            val rotatedThumbnail = if (it.width > it.height) {
                rotateBitmap(it, 90f) // Xoay ảnh 90 độ nếu nó đang nằm ngang
            } else {
                it // Giữ nguyên ảnh nếu nó đang nằm dọc
            }
            holder.imgVideo.setImageBitmap(rotatedThumbnail)
            holder.imgPlay.visibility = View.VISIBLE
            holder.itemView.setOnClickListener {
                if (Singleton.getInstance().isConnected()) {
                    onClick?.onMediaClick(position)
                } else {
                    val deviceFrag = DeviceFragment()
                    if (context is FragmentActivity) {
                        val transaction = context.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        transaction.replace(R.id.fragment_container, deviceFrag, "findThisFragment")
                        transaction.addToBackStack("findThisFragment")
                        transaction.commit()
                    }
            }
            }
        }
    }

    override fun getItemCount(): Int {
        return videoFiles.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgVideo: ImageView = itemView.findViewById(R.id.img_video)
        val imgPlay: ImageView = itemView.findViewById(R.id.img_play)
    }

    private fun getVideoThumbnail(videoFile: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoFile.absolutePath)
        val bitmap = retriever.frameAtTime
        retriever.release()
        return bitmap
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
