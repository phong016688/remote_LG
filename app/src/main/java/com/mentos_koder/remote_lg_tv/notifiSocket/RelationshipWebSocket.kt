package com.mentos_koder.remote_lg_tv.notifiSocket

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.model.Device
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.MainActivity
import com.mentos_koder.remote_lg_tv.view.MainApplication
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RelationshipWebSocket : WebSocketClient {
    private var deviceModel: String? = null
    private var deviceName: String? = null
    private var deviceIpAddress: String? = null
    private var deviceFriendlyName: String? = null
    private val appContext = MainApplication.getAppContext()

    constructor(
        serverUri: URI,
        deviceModel: String?,
        deviceFriendlyName: String?,
        deviceName: String?,
        deviceIpAddress: String?
    ) : super(serverUri) {
        Log.d("####", "ConnectURI: $serverUri")
        this.deviceName = deviceName
        this.deviceModel = deviceModel
        this.deviceIpAddress = deviceIpAddress
        this.deviceFriendlyName = deviceFriendlyName
    }

    constructor(serverUri: URI) : super(serverUri) {
        Log.d("####", "ConnectURI: $serverUri")
    }

    override fun onOpen(handshakeData: ServerHandshake) {
        Log.d("#####", "Connection opened")
    }

    override fun onMessage(message: String) {
        Log.d("####", "onMessage: $message")
        val gson = Gson()
        try {
            val jsonObject = gson.fromJson(message, JsonObject::class.java)
            val dataObject = jsonObject.getAsJsonObject("data")
            val token = dataObject["token"].asString
            if (token.isNotEmpty()) {
                val deviceDao = AppDatabase.getDatabase(appContext).deviceDao()
                val deviceCount = deviceIpAddress?.let { deviceDao.countDevicesWithAddress(it) }
                val singleton = Singleton.getInstance()
                singleton.setConnected(true)
                if (deviceCount == 0) {
                    val device = Device()
                    device.address = deviceIpAddress!!
                    device.model = deviceModel
                    device.token = token
                    device.firstDateConnect = getCurrentTime()
                    device.lastDateConnect = getCurrentTime()
                    device.name = deviceFriendlyName
                    device.typeDevice = deviceName
                    device.typeConnect = "handwork"
                    deviceDao.insert(device)
                    Log.d("#####", "Device saved successfully")
                    val intent = Intent(appContext, MainActivity::class.java)
                    intent.putExtra("name", deviceFriendlyName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(intent)
                } else {
                    val device = deviceIpAddress?.let { deviceDao.getDeviceByAddress(it) }
                    device!!.lastDateConnect = getCurrentTime()
                    device.typeConnect = "handwork"
                    deviceDao.update(device)
                    Log.d("#####", "Device updated successfully")
                    val intent = Intent(appContext, MainActivity::class.java)
                    intent.putExtra("name", deviceFriendlyName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(intent)
                }
            } else {
                Log.e("#####", "Token is empty")
                Singleton.getInstance().setConnected(false)
            }
        } catch (e: JsonSyntaxException) {
            Log.e("#####", "Error: " + e.message)
            Singleton.getInstance().setConnected(false)
        } catch (e: IllegalStateException) {
            Log.e("#####", "Error: " + e.message)
            Singleton.getInstance().setConnected(false)
        } catch (e: Exception) {
            Log.e("#####", "Unknown error: " + e.message)
            Singleton.getInstance().setConnected(false)
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        Log.d(
            "#####",
            "Connection closed. Code: $code, Reason: $reason, Remote: $remote"
        )
    }

    override fun onError(ex: Exception) {
        Log.e("#####", "Error occurred: " + ex.message)
    }

    private fun getCurrentTime(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTime = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }
}

