package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.content.SharedPreferences
import android.os.Vibrator
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
import com.mentos_koder.remote_lg_tv.model.Favorite
import com.mentos_koder.remote_lg_tv.util.Constants.KEY_FAVORITE
import com.mentos_koder.remote_lg_tv.util.Constants.PREFERENCE_NAME
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val device = listApp[position]
        val appHolder = holder as? AppViewHolder
        appHolder?.bind(device)
    }

    override fun getItemCount(): Int {
        return listApp.size
    }

    private fun insertDevice(
        favorite: Favorite, device: AppInfo, ip: String, deviceDao: DeviceDao
    ) {
        favorite.id = device.id
        favorite.name = device.name
        favorite.iconLink = ip
        favorite.ipAddress = ipAddress
        favorite.recentDate = currentTime
        favorite.favourite = true
        deviceDao.insertFavourite(favorite)
    }

    private val currentTime: String
        get() {
            val currentTimeMillis = System.currentTimeMillis()
            val currentTime = Date(currentTimeMillis)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return dateFormat.format(currentTime)
        }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var device: AppInfo? = null
        private var txtNameChannel: TextView = itemView.findViewById(R.id.txt_NameChannel)
        private var imgChannel: ImageView = itemView.findViewById(R.id.img_Channel)
        private var imgFavourite: ImageView = itemView.findViewById(R.id.img_favourite)
        private val preferences: SharedPreferences =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        init {
            imgFavourite.setOnClickListener {
                val device = this.device ?: return@setOnClickListener
                performVibrateAction()
                val isFavouriteCheck = preferences.getBoolean(KEY_FAVORITE + device.id, false)
                val editor = preferences.edit()
                val deviceDao = AppDatabase.getDatabase(context).deviceDao()
                val favorite = Favorite()
                val count = deviceDao.countFavouriteWithId(device.id)

                if (isFavouriteCheck) {
                    imgFavourite.isSelected = false
                    editor.putBoolean(KEY_FAVORITE + device.id, false).apply()
                    deviceDao.deleteFavourite(device.id)
                } else {
                    imgFavourite.isSelected = true
                    editor.putBoolean(KEY_FAVORITE + device.id, true).apply()
                    if (count > 0) {
                        deviceDao.updateFavourite(true, device.id)
                    } else {
                        insertDevice(favorite, device, getIp(device), deviceDao)
                    }
                }
                itemClickListener.onItemClicked()
            }
            imgChannel.setOnClickListener {
                val device = this.device ?: return@setOnClickListener
                performVibrateAction()
                Singleton.getInstance().openAppOnTV(device.id)
            }
        }

        fun bind(device: AppInfo) {
            this.device = device
            txtNameChannel.text = device.name
            Glide.with(context).load(getIp(device)).into(imgChannel)
            val isFavourite = preferences.getBoolean(KEY_FAVORITE + device.id, false)
            if (isFavourite) {
                imgFavourite.setImageResource(R.drawable.ic_heart_fill)
            } else {
                imgFavourite.setImageResource(R.drawable.ic_heart)
            }
        }

        private fun getIp(device: AppInfo) = "http://" + ipAddress + ":8060/query/icon/" + device.id
    }

    private fun checkRing(): Boolean {
        val boolean = restoreSwitchState(context)
        return boolean
    }

    private fun performVibrateAction() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (checkRing()) {
            vibrator.vibrate(100)
        }
    }
}