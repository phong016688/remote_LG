package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.event.OnMediaClickListener
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.MainActivity
import com.mentos_koder.remote_lg_tv.view.fragment.DeviceFragment

class PhotoAdapter(private var images: MutableList<String>?, val context: Context) :
    RecyclerView.Adapter<PhotoAdapter.ImageViewHolder>() {
    private var onClick: OnMediaClickListener? = null
    fun setOnImageClickListener(listener: OnMediaClickListener) {
        onClick = listener
    }

    fun addPhoto(photoPath: String) {
        images?.add(photoPath)
        notifyItemInserted(images!!.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return ImageViewHolder(view).apply {
            itemView.setOnClickListener {
                val imagePath = this.imagePath ?: return@setOnClickListener
                if (Singleton.getInstance().isConnected()) {
                    onClick?.onMediaClick(position, imagePath)
                } else if (context is MainActivity) {
                    context.showFragmentDevice()
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageBitmap = images?.get(position)
        Glide.with(holder.itemView.context).load(imageBitmap)
            .apply(RequestOptions().placeholder(R.drawable.ic_app).error(R.drawable.ic_setting))
            .diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imageView)

        if (!imageBitmap?.let { Singleton.getInstance().containsBitmap(it) }!!) {
            Singleton.getInstance().addBitmap(imageBitmap)
        }
        holder.imagePath = images?.get(position)
    }

    override fun getItemCount(): Int {
        return images?.size ?: 0
    }


    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imagePath: String? = null
        val imageView: ImageView = itemView.findViewById(R.id.img_photo)
    }
}