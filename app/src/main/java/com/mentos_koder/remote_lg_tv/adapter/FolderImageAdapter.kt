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
import com.mentos_koder.remote_lg_tv.event.OnFolderClickListener
import com.mentos_koder.remote_lg_tv.model.ImageFolder
import java.io.File

class FolderImageAdapter(private val context: Context, private var folderList: List<ImageFolder>) : RecyclerView.Adapter<FolderImageAdapter.ImageFolderViewHolder>() {
    private var folderClickListener: OnFolderClickListener? = null
    fun setOnFolderClickListener(listener: OnFolderClickListener) {
        folderClickListener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageFolderViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_image, parent, false)
        return ImageFolderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageFolderViewHolder, position: Int) {
        val currentFolder = folderList[position]
        holder.bind(currentFolder)
    }

    override fun getItemCount() = folderList.size

    inner class ImageFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderNameTextView: TextView = itemView.findViewById(R.id.tv_nameFolder)
        private val folderQuantityTextView: TextView = itemView.findViewById(R.id.tv_quantity_img)
        private val imgPhoto: ImageView = itemView.findViewById(R.id.img_photo)
        private val imgPhoto2: ImageView = itemView.findViewById(R.id.img_photo_2)
        private val imgPhoto3: ImageView = itemView.findViewById(R.id.img_photo_3)
        fun bind(imageFolder: ImageFolder) {
            folderNameTextView.text = imageFolder.folderName
            val imageList = imageFolder.imageList
             folderQuantityTextView.text = imageFolder.quantity.toString()
            if (imageList.isNotEmpty()) {
                if (imageList.size >= 3) {
                    val firstImagePath = imageList[imageList.size - 1].absolutePath
                    val secondImagePath = imageList[imageList.size - 2].absolutePath
                    val thirdImagePath = imageList[imageList.size - 3].absolutePath

                    Glide.with(context)
                        .load(File(firstImagePath))
                        .into(imgPhoto)

                    Glide.with(context)
                        .load(File(secondImagePath))
                        .into(imgPhoto2)

                    Glide.with(context)
                        .load(File(thirdImagePath))
                        .into(imgPhoto3)
                } else {
                    for (i in imageList.indices) {
                        val imagePath = imageList[i].absolutePath
                        when (i) {
                            0 -> Glide.with(context).load(File(imagePath)).into(imgPhoto)
                            1 -> Glide.with(context).load(File(imagePath)).into(imgPhoto2)
                            2 -> Glide.with(context).load(File(imagePath)).into(imgPhoto3)
                        }
                    }
                }
            } else {
                imgPhoto.setImageResource(R.color.white)
                imgPhoto2.setImageResource(R.color.white)
                imgPhoto3.setImageResource(R.color.white)
            }

            itemView.setOnClickListener {
                folderClickListener?.onFolderClick(imageFolder)
            }
        }
    }
}