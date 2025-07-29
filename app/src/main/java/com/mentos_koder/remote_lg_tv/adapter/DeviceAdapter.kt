package com.mentos_koder.remote_lg_tv.adapter

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.connectsdk.device.ConnectableDevice
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.database.DeviceDao
import com.mentos_koder.remote_lg_tv.util.OnBack
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.MainActivity
import java.util.Locale

class DeviceAdapter(private val context: Context, private val listDevice: MutableList<ConnectableDevice>,private val onBack: OnBack?) :
    BaseAdapter() {
    var singleton: Singleton = Singleton.getInstance()

    fun updateData(newDevices: List<ConnectableDevice?>?) {
        listDevice.clear()
        newDevices?.let {
            listDevice.addAll(it.filterNotNull())
        }

        sort()
        notifyDataSetChanged()
    }
    override fun getCount(): Int {
        return listDevice.size
    }

    override fun getItem(position: Int): Any {
        return listDevice[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private class ViewHolder {
        var txtIpAddress: TextView? = null
        var txtBrand: TextView? = null
        var logoTv: ImageView? = null
        var linearItem: LinearLayout? = null
    }

    private fun checkSS(device: ConnectableDevice) {
        val ss = "Samsung"
        val manufacturer = device.manufacturer
        if (manufacturer != null && manufacturer.lowercase(Locale.getDefault()).contains(
                ss.lowercase(
                    Locale.getDefault()
                )
            )
        ) {
            device.logo = R.drawable.ic_tv
        } else {
            device.logo = R.drawable.ic_tv
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        sort()

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
            holder = ViewHolder()
            holder.txtIpAddress = view.findViewById(R.id.tv_ip_address)
            holder.txtBrand = view.findViewById(R.id.tv_brand)
            holder.logoTv = view.findViewById(R.id.img_logo)
            holder.linearItem = view.findViewById(R.id.linear_item)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val device = listDevice[position]
        val name: String = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
        val serviceName: String = device.serviceName?.lowercase(Locale.ROOT) ?: ""
        val manufacturer: String = device.manufacturer?.lowercase(Locale.ROOT) ?: ""

        holder.txtIpAddress!!.text = view?.context?.getString(R.string.universal_remote)
        holder.txtBrand!!.text = device.friendlyName
        checkSS(device)

        if ( name.contains("lg") || serviceName.contains("lg") || manufacturer.contains("lg")){
            device.logo = R.drawable.ic_tv
            holder.txtIpAddress!!.text = device.ipAddress
        }

        holder.logoTv!!.setImageResource(device.logo)
        holder.linearItem!!.setOnClickListener {
            if ( name.contains("lg") || serviceName.contains("lg") || manufacturer.contains("lg")){
                singleton.handelTypeTV(
                    device,
                    deviceDao,
                    getCountDevice(device),
                    { showCustomDialog() },
                    context, onBack
                )
            }else{
                openUniversalApp("com.mentos_koder.universalremote",".view.MainActivity")
                Log.d("0972334", "Name: $name - ServiceName: $serviceName - Manufacturer: $manufacturer")
            }
        }

        return view!!
    }

    private fun openUniversalApp(packageName: String, activityName: String){
        try {
            val intent = Intent().apply {
                setClassName(packageName, "$packageName$activityName")
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openPlayStore(packageName)
        }
    }

    private fun openPlayStore(packageName: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun showCustomDialog() {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.item_dialog_connect)
        val countdownText = dialog.findViewById<TextView>(R.id.time_connect)
        val title = dialog.findViewById<TextView>(R.id.Title)
        val innerLayout = dialog.findViewById<LinearLayout>(R.id.innerLayout)
        object : CountDownTimer(35000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownText.text = "(" + millisUntilFinished / 1000 + ")"
                if (millisUntilFinished <= 10000) {
                    blinkText(title)
                }
            }

            override fun onFinish() {
                countdownText.visibility = View.GONE
                blinkText(title)
            }
        }.start()
        innerLayout.setOnClickListener { _: View? ->
            dialog.dismiss()
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
            countdownText.visibility = View.GONE
        }
        dialog.show()
    }


    private fun blinkText(textView: TextView) {
        val blinkAnimation = AlphaAnimation(0.0f, 1.0f)
        blinkAnimation.duration = 500
        blinkAnimation.repeatMode = Animation.REVERSE
        blinkAnimation.repeatCount = Animation.INFINITE
        textView.setTextColor(Color.RED)
        textView.startAnimation(blinkAnimation)
    }

    private fun getCountDevice(device: ConnectableDevice): Int {
        val deviceDao = deviceDao
        return deviceDao.countDevicesWithAddress(device.ipAddress)
    }

    private val deviceDao: DeviceDao
        get() = AppDatabase.getDatabase(context).deviceDao()

    private fun sort(){
        listDevice.sortWith(compareBy(
            { device ->
                val name = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
                val serviceName = device.serviceName?.lowercase(Locale.ROOT) ?: ""
                val manufacturer = device.manufacturer?.lowercase(Locale.ROOT) ?: ""
                !(name.contains("lg") || serviceName.contains("lg") || manufacturer.contains("lg"))
            },
            { device ->
                val name = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
                name
            }
        ))
    }
}
