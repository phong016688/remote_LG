package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connectsdk.core.AppInfo
import com.mentos_koder.remote_lg_tv.event.OnItemClickListener
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.database.DeviceDao
import com.mentos_koder.remote_lg_tv.model.Favourite
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppAdapter(
    private val context: Context,
    private val listApp: List<AppInfo>,
    private val ipAddress: String,
    private val itemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val VIEW_TYPE_APP = 1
    val VIEW_TYPE_TEMPLATE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_APP -> {
                val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
                AppViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_APP -> {
                val device = listApp[position]

                val appHolder = holder as AppViewHolder
                appHolder.bind(device)
            }
        }
    }

    override fun getItemCount(): Int {
        return listApp.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 3) {
            VIEW_TYPE_TEMPLATE
        } else {
            VIEW_TYPE_APP
        }
    }

    private fun insertDevice(
        favourite: Favourite,
        device: AppInfo,
        ip: String,
        deviceDao: DeviceDao
    ) {
        favourite.id = device.id
        favourite.name = device.name
        favourite.iconLink = ip
        favourite.ipAddress = ipAddress
        favourite.recentDate = currentTime
        favourite.favourite = true
        deviceDao.insertFavourite(favourite)
    }

    private val currentTime: String
        get() {
            val currentTimeMillis = System.currentTimeMillis()
            val currentTime = Date(currentTimeMillis)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return dateFormat.format(currentTime)
        }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtNameChannel: TextView
        var imgChannel: ImageView
        var imgFavourite: ImageView

        init {
            txtNameChannel = itemView.findViewById<TextView>(R.id.txt_NameChannel)
            imgChannel = itemView.findViewById<ImageView>(R.id.img_Channel)
            imgFavourite = itemView.findViewById<ImageView>(R.id.img_favourite)
        }

        fun bind(device: AppInfo) {
            txtNameChannel.text = device.name
            val ip = "http://" + ipAddress + ":8060/query/icon/" + device.id

            Glide.with(context).load(ip).into(imgChannel)

            val preferences = context.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            val isFavourite = preferences.getBoolean("isFavourite_" + device.id, false)

            imgFavourite.isSelected = isFavourite
            imgFavourite.setOnClickListener {
                performVibrateAction()
                val isFavouriteCheck =
                    preferences.getBoolean("isFavourite_" + device.id, false)
                val editor = preferences.edit()
                val deviceDao = AppDatabase.getDatabase(context).deviceDao()
                val favourite = Favourite()
                val count = deviceDao.countFavouriteWithId(device.id)

                if (isFavouriteCheck) {
                    imgFavourite.isSelected = false
                    editor.putBoolean("isFavourite_" + device.id, false).apply()
                    deviceDao.deleteFavourite(device.id)
                } else {
                    imgFavourite.isSelected = true
                    editor.putBoolean("isFavourite_" + device.id, true).apply()
                    if (count > 0) {
                        deviceDao.updateFavourite(true, device.id)
                        Log.d("####", "update")
                    } else {
                        insertDevice(favourite, device, ip, deviceDao)
                        Log.d("####", "insert")
                    }
                }
                itemClickListener.onItemClicked()
            }
            imgChannel.setOnClickListener {
                performVibrateAction()
                Singleton.getInstance().openAppOnTV(device.id)
            }
        }
    }
    private fun checkRing() : Boolean {
        val boolean =  context.let { restoreSwitchState(it) }
        return boolean
    }
    private fun performVibrateAction() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (checkRing() == true) {
            vibrator.vibrate(100)
        }
    }
    inner class TemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }
}