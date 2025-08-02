package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.connectsdk.device.ConnectableDevice
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.OnBack
import com.mentos_koder.remote_lg_tv.util.Singleton
import java.util.Locale

class DeviceAdapter(
    private val context: Context,
    private val listDevice: MutableList<ConnectableDevice>,
    private val onBack: OnBack?
) : BaseAdapter() {
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
        var txtName: TextView? = null
        var txtType: TextView? = null
        var logoTv: ImageView? = null
        var linearItem: LinearLayout? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        sort()

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
            holder = ViewHolder()
            holder.txtIpAddress = view.findViewById(R.id.tv_ip_value)
            holder.txtName = view.findViewById(R.id.tv_name_value)
            holder.txtType = view.findViewById(R.id.tv_type_value)
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
        holder.txtName!!.text = device.friendlyName

        if (name.contains("lg") || serviceName.contains("lg") || manufacturer.contains("lg")) {
            device.logo = R.drawable.ic_tv
            holder.txtIpAddress!!.text = device.ipAddress
            holder.txtType?.text = "LG"
        }

        holder.logoTv!!.setImageResource(device.logo)
        holder.linearItem!!.setOnClickListener {
            if (name.contains("lg") || serviceName.contains("lg") || manufacturer.contains("lg")) {
                singleton.handelTypeTV(device, context, onBack)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.tv_is_not_lg),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        return view!!
    }

    private fun sort() {
        listDevice.sortWith(compareBy({ device ->
            val name = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
            val serviceName = device.serviceName?.lowercase(Locale.ROOT) ?: ""
            val manufacturer = device.manufacturer?.lowercase(Locale.ROOT) ?: ""
            !(name.contains("lg") || serviceName.contains("lg") || manufacturer.contains("lg"))
        }, { device ->
            val name = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
            name
        }))
    }
}
