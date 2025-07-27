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
    private var model: String? = null
    private var Name: String? = null
    private var ipAddress: String? = null
    private var friendlyName: String? = null
    var context = MainApplication.getAppContext()

    constructor(
        serverUri: URI,
        model: String?,
        friendlyName: String?,
        deviceName: String?,
        ipAddress: String?
    ) : super(serverUri) {
        Log.d("####", "ConnectURI: $serverUri")
        Name = deviceName
        this.model = model
        this.ipAddress = ipAddress
        this.friendlyName = friendlyName
    }

    constructor(serverUri: URI) : super(serverUri) {
        Log.d("####", "ConnectURI: $serverUri")
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        Log.d("#####", "Connection opened")
    }

    override fun onMessage(message: String) {
        Log.d("####", "onMessage: $message")
        val gson = Gson()
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val data = json.getAsJsonObject("data")
            val token = data["token"].asString
            if (token.isNotEmpty()) {
                val deviceDao = AppDatabase.getDatabase(context).deviceDao()
                val deviceCount = ipAddress?.let { deviceDao.countDevicesWithAddress(it) }
                val singleton = Singleton.getInstance()
                singleton.setConnected(true)
                if (deviceCount == 0) {
                    val device = Device()
                    device.address = ipAddress!!
                    device.model = model
                    device.token = token
                    device.firstDateConnect = currentTime
                    device.lastDateConnect = currentTime
                    device.name = friendlyName
                    device.typeDevice = Name
                    device.typeConnect = "handwork"
                    deviceDao.insert(device)
                    Log.d("#####", "Device saved successfully")
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("name", friendlyName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    val device = ipAddress?.let { deviceDao.getDeviceByAddress(it) }
                    device!!.lastDateConnect = currentTime
                    device.typeConnect = "handwork"
                    deviceDao.update(device)
                    Log.d("#####", "Device updated successfully")
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("name", friendlyName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
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

    private val currentTime: String
        get() {
            val currentTimeMillis = System.currentTimeMillis()
            val currentTime = Date(currentTimeMillis)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return dateFormat.format(currentTime)
        }
}

