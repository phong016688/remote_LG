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
    private var name: String? = null
    private var ipAddress: String? = null
    var context: Context = MainApplication.getAppContext()!!

    constructor(serverUri: URI) : super(serverUri) {
        Log.d("####", "ConnectURI: $serverUri")
    }

    constructor(serverUri: URI, deviceName: String?, ipAddress: String?) : super(serverUri) {
        Log.d("####", "Auto ConnectURI: $serverUri")
        name = deviceName
        this.ipAddress = ipAddress
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        Log.d("#####", "Auto connection opened")
    }

    override fun onMessage(message: String) {
        Log.d("#####", "onMessage: $message")
        val gson = Gson()
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val data = json.getAsJsonObject("data")
            if (data != null && data.has("clients")) {
                val clientsArray = data.getAsJsonArray("clients")
                if (clientsArray.size() != 0) {
                    val firstClient = clientsArray[0].getAsJsonObject()
                    if (firstClient != null && firstClient.has("attributes")) {
                        val attributes = firstClient.getAsJsonObject("attributes")
                        if (attributes != null && attributes.has("token")) {
                            val token = attributes["token"].asString
                            if (token.isNotEmpty()) {
                                val deviceDao = AppDatabase.getDatabase(context).deviceDao()
                                val singleton = Singleton.getInstance()
                                val device = ipAddress?.let { deviceDao.getDeviceByAddress(it) }
                                singleton.setConnected(true)
                                device!!.lastDateConnect = currentTime
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

    private val currentTime: String
        get() {
            val currentTimeMillis = System.currentTimeMillis()
            val currentTime = Date(currentTimeMillis)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return dateFormat.format(currentTime)
        }
}

