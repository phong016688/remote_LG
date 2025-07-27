package com.mentos_koder.remote_lg_tv.adapter


import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.model.Cast


class CastAdapter(
    private val mData: List<Cast>,
    val context: Context,
) :
    RecyclerView.Adapter<CastAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(typeCast : String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_cast_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, img) = mData[position]
        holder.tvCast.text = name
        holder.tvCast2.text = holder.itemView.context.getString(R.string.cast_text,name)
        holder.imgCast.setImageResource(img)
        holder.constraintCast.setOnClickListener {
            listener?.onItemClick(name)
//            if (Singleton.getInstance().isConnected()){
//                listener?.onItemClick(name)
//            }else{
//                listenerFragment.onCastItemClick()
//            }
        }

        when (position) {
            0 -> holder.itemView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF91AD"))
            1 -> holder.itemView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E45285"))
            2 -> holder.itemView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C8135C"))
        }
    }


    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgCast: ImageView = itemView.findViewById(R.id.img_cast)
        var tvCast: TextView = itemView.findViewById(R.id.tv_cast)
        var tvCast2: TextView = itemView.findViewById(R.id.tv_cast_2)
        var constraintCast: ConstraintLayout = itemView.findViewById(R.id.constraint_cast)
    }
}
