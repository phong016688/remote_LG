package com.mentos_koder.remote_lg_tv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.remote_lg_tv.databinding.ItemDeviceCastBinding
import com.mentos_koder.remote_lg_tv.model.Cast
import com.mentos_koder.remote_lg_tv.util.clicks

abstract class BaseListAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback) {

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    override fun onBindViewHolder(holder: VH, position: Int) {
        bind(holder, getItem(position), position)
    }

    abstract fun bind(holder: VH, item: T, position: Int)

    fun inflateLayout(parent: ViewGroup, layoutRes: Int): View {
        return LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
    }
}

class CastAdapter :
    BaseListAdapter<Cast, CastAdapter.ViewHolder>(TaskDiffCallbackCast()) {
    var clickItem: ((Cast) -> Unit)? = null

    inner class ViewHolder(itemView: ItemDeviceCastBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        var item: Cast? = null
        private val name = itemView.tvCast
        private val imgCast = itemView.imgCast

        init {
            itemView.root.clicks {
                item?.let { it1 -> clickItem?.invoke(it1) }
            }
        }

        fun bind(item: Cast) {
            this.item = item
            name.text = item.name
            imgCast.setImageResource(item.img)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = ItemDeviceCastBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun bind(holder: ViewHolder, item: Cast, position: Int) {
        holder.bind(item)
    }
}

class TaskDiffCallbackCast : DiffUtil.ItemCallback<Cast>() {
    override fun areItemsTheSame(oldItem: Cast, newItem: Cast): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Cast, newItem: Cast): Boolean {
        return oldItem == newItem
    }
}
