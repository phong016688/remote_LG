package com.mentos_koder.remote_lg_tv.view.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.connectsdk.core.Util
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.service.command.ServiceCommandError
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.DeviceAdapter
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.OnBack
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale


class DeviceFragment : Fragment(), DiscoveryManagerListener {
    private var closeButton: ImageButton? = null
    private var deviceList: ListView? = null
    private var deviceAdapter: DeviceAdapter? = null
    private var singletonInstance: Singleton? = null
    private var mActivity: FragmentActivity? = null
    private var onBack: OnBack? = null
    private var deviceMap = HashMap<String, ConnectableDevice>()

    init {
        deviceMap = java.util.HashMap()
        DiscoveryManager.getInstance().addListener(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is OnBack){
            onBack = context
        }
        if(context is FragmentActivity){
            mActivity = context
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.fragment_device, container, false)
        onBack()
        initializeViews(rootView)
        setupEventListeners()
        singletonInstance = Singleton.getInstance()
        val sortedDevices = sortDevices(
            getDeviceMap().values.toList()
        )
        deviceAdapter = DeviceAdapter(requireContext(), sortedDevices, onBack)
        deviceList!!.adapter = deviceAdapter
        return rootView
    }

    private fun initializeViews(rootView: View) {
        closeButton = rootView.findViewById(R.id.btn_close_device)
        deviceList = rootView.findViewById(R.id.device_list)
    }
    private fun onBack(){
        mActivity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                onBack?.back(0)
            }
        })
    }

    private fun setupEventListeners() {
        closeButton!!.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun sortDevices(devices: Collection<ConnectableDevice>): MutableList<ConnectableDevice> {
        val sortedDevices = devices.toMutableList()
        sortedDevices.sortWith { device1, device2 ->
            val manufacturer1 = device1.manufacturer
            val manufacturer2 = device2.manufacturer

            when {
                manufacturer1 != null && manufacturer2 != null -> manufacturer1.compareTo(
                    manufacturer2
                )

                manufacturer1 != null -> 1
                manufacturer2 != null -> -1
                else -> 0
            }
        }
        return sortedDevices
    }

    override fun onResume() {
        super.onResume()
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                requireActivity().supportFragmentManager.popBackStack()
                return@setOnKeyListener true
            }
            false
        }
    }

    override fun onDeviceAdded(manager: DiscoveryManager?, device: ConnectableDevice?) {
        if (device != null) {
            val manufacturer = device.manufacturer?.lowercase(Locale.ROOT) ?: ""
            val friendlyName = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
            val servicedevice = device.toJSONObject()
            val ip = device.ipAddress
            Log.d("####", "onDeviceAdded manufacturer: $manufacturer")
            Log.d("####", "onDeviceAdded friendlyName: $friendlyName")
            Log.d("####", "onDeviceAdded serviceName: $servicedevice")
            Log.d("####", "onDeviceUpdated serviceName: $ip")

//            deviceMapCheck[ip] = device
//            for ((existingIP, existingDevice) in deviceMapCheck) {
//                if (existingDevice.friendlyName?.lowercase(Locale.ROOT) == friendlyName && existingIP != ip) {
//                    if (!Singleton.getInstance().duplicateDevicesList.contains(existingDevice)) {
//                        Singleton.getInstance().duplicateDevicesList.add(existingDevice)
//                    }
//                    if (!Singleton.getInstance().duplicateDevicesList.contains(device)) {
//                        Singleton.getInstance().duplicateDevicesList.add(device)
//                    }
//                }
//            }

            if (isValidIPv4(ip) || isValidIPv6(ip)) {
                deviceMap[ip] = device
            }else{
                Singleton.getInstance().duplicateDevicesList.add(device)
            }
        }
    }

    override fun onDeviceUpdated(manager: DiscoveryManager?, device: ConnectableDevice?) {
        if (device != null) {
            val manufacturer = device.manufacturer?.lowercase(Locale.ROOT) ?: ""
            val friendlyName = device.friendlyName?.lowercase(Locale.ROOT) ?: ""
            val servicedevice =  device.toJSONObject()
            val servicedeviceT =  device.ipAddress
            Log.d("####", "onDeviceUpdated: $manufacturer")
            Log.d("####", "onDeviceUpdated: $friendlyName")
            Log.d("####", "onDeviceUpdated serviceName: $servicedevice")
            Log.d("####", "onDeviceUpdated serviceName: $servicedeviceT")
            deviceMap[device.ipAddress] = device
            updateDeviceList()
        }
    }

    override fun onDeviceRemoved(manager: DiscoveryManager?, device: ConnectableDevice?) {
        if (device != null)
            Util.runOnUI {
                deviceMap.remove(device.ipAddress)
                updateDeviceList()
            }
        }

    override fun onDiscoveryFailed(manager: DiscoveryManager?, error: ServiceCommandError?) {
        Util.runOnUI { Log.d("FailedDevice###", "FailedDevice: $error") }
    }

    private fun getDeviceMap(): HashMap<String, ConnectableDevice> {
        val deviceMapCopy = HashMap(deviceMap)
        deviceMapCopy.values
        return deviceMapCopy
    }

    private fun updateDeviceList() {
        val sortedDevices = ArrayList(deviceMap.values)
        sortDevices(sortedDevices)
        deviceAdapter?.updateData(ArrayList(sortedDevices))
    }
    // Hàm kiểm tra xem một chuỗi có phải là địa chỉ IPv4 hợp lệ không
    fun isValidIPv4(ip: String): Boolean {
        try {
            val inetAddress = InetAddress.getByName(ip)
            return inetAddress is Inet4Address
        } catch (e: Exception) {
            return false
        }
    }

    // Hàm kiểm tra xem một chuỗi có phải là địa chỉ IPv6 hợp lệ không
    fun isValidIPv6(ip: String): Boolean {
        try {
            val inetAddress = InetAddress.getByName(ip)
            return inetAddress is Inet6Address
        } catch (e: Exception) {
            return false
        }
    }
}