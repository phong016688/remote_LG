package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.event.OnFolderClickListenerVideo
import com.mentos_koder.remote_lg_tv.model.VideoFolder

class FolderVideoAdapter(private val context: Context, private val folderList: List<VideoFolder>) : RecyclerView.Adapter<FolderVideoAdapter.VideoFolderViewHolder>() {
    private var folderClickListener: OnFolderClickListenerVideo? = null

    fun setOnFolderClickListener(listener: OnFolderClickListenerVideo) {
        folderClickListener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoFolderViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_image, parent, false)
        return VideoFolderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VideoFolderViewHolder, position: Int) {
        val currentFolder = folderList[position]
        holder.bind(currentFolder)
    }

    override fun getItemCount() = folderList.size

    inner class VideoFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderNameTextView: TextView = itemView.findViewById(R.id.tv_nameFolder)
        private val folderQuantityTextView: TextView = itemView.findViewById(R.id.tv_quantity_img)
        private val imgPhoto: ImageView = itemView.findViewById(R.id.img_photo)
        private val imgPhoto2: ImageView = itemView.findViewById(R.id.img_photo_2)
        private val imgPhoto3: ImageView = itemView.findViewById(R.id.img_photo_3)

        fun bind(videoFolder: VideoFolder) {
            folderNameTextView.text = videoFolder.folderName
            val videoList = videoFolder.videoList
            folderQuantityTextView.text = videoFolder.quantity.toString()

            if (videoList.isNotEmpty()) {
                if (videoList.size >= 3) {
                    val firstVideoPath = videoList[videoList.size - 1].absolutePath
                    val secondVideoPath = videoList[videoList.size - 2].absolutePath
                    val thirdVideoPath = videoList[videoList.size - 3].absolutePath

                    Glide.with(context).load(firstVideoPath).into(imgPhoto)
                    Glide.with(context).load(secondVideoPath).into(imgPhoto2)
                    Glide.with(context).load(thirdVideoPath).into(imgPhoto3)
                } else {
                    for (i in videoList.indices) {
                        val videoPath = videoList[i].absolutePath
                        when (i) {
                            0 -> Glide.with(context).load(videoPath).into(imgPhoto)
                            1 -> Glide.with(context).load(videoPath).into(imgPhoto2)
                            2 -> Glide.with(context).load(videoPath).into(imgPhoto3)
                        }
                    }
                }
            } else {
                imgPhoto.setImageResource(R.color.white)
                imgPhoto2.visibility = View.INVISIBLE
                imgPhoto3.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener {
                folderClickListener?.onFolderClick(videoFolder.folderPath)
            }
        }
    }

}
