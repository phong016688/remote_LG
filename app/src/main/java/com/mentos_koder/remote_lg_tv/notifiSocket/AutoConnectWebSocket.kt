package com.mentos_koder.remote_lg_tv.notifiSocket

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.MainApplication
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AutoConnectWebSocket : WebSocketClient {
    private var deviceName: String? = null
    private var deviceIpAddress: String? = null
    private val appContext: Context = MainApplication.getAppContext()!!

    constructor(serverUri: URI) : super(serverUri) {
        Log.d("####", "ConnectURI: $serverUri")
    }

    constructor(serverUri: URI, deviceName: String?, deviceIpAddress: String?) : super(serverUri) {
        Log.d("####", "Auto ConnectURI: $serverUri")
        this.deviceName = deviceName
        this.deviceIpAddress = deviceIpAddress
    }

    override fun onOpen(handshakeData: ServerHandshake) {
        Log.d("#####", "Auto connection opened")
    }

    override fun onMessage(message: String) {
        Log.d("#####", "onMessage: $message")
        val gson = Gson()
        try {
            val jsonObject = gson.fromJson(message, JsonObject::class.java)
            val dataObject = jsonObject.getAsJsonObject("data")
            if (dataObject != null && dataObject.has("clients")) {
                val clientsArray = dataObject.getAsJsonArray("clients")
                if (clientsArray.size() != 0) {
                    val firstClient = clientsArray[0].getAsJsonObject()
                    if (firstClient != null && firstClient.has("attributes")) {
                        val attributes = firstClient.getAsJsonObject("attributes")
                        if (attributes != null && attributes.has("token")) {
                            val token = attributes["token"].asString
                            if (token.isNotEmpty()) {
                                val deviceDao = AppDatabase.getDatabase(appContext).deviceDao()
                                val singleton = Singleton.getInstance()
                                val device = deviceIpAddress?.let { deviceDao.getDeviceByAddress(it) }
                                singleton.setConnected(true)
                                device!!.lastDateConnect = getCurrentTime()
                                device.typeConnect = "auto"
                                deviceDao.update(device)
                                Log.d("#####", "Device auto successfully")
                            } else {
                                Log.e("#####", "Token is empty")
                                Singleton.getInstance().setConnected(false)
                            }
                        } else {
                            Log.e("#####", "Token not found in the 'attributes' object")
                        }
                    } else {
                        Log.e("#####", "'attributes' object not found in the client")
                    }
                } else {
                    Log.e("#####", "No clients found in the JSON data")
                }
            } else {
                Log.e("#####", "'clients' array not found in the JSON data")
            }
        } catch (e: JsonSyntaxException) {
            Log.e("#####", "Error parsing JSON: " + e.message)
        } catch (e: IllegalStateException) {
            Log.e("#####", "Error parsing JSON: " + e.message)
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
        val singleton = Singleton.getInstance()
        singleton.isConnectedCustom = false
    }

    private fun getCurrentTime(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTime = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }
}

