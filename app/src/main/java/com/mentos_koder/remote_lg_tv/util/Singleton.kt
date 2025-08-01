package com.mentos_koder.remote_lg_tv.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.connectsdk.core.AppInfo
import com.connectsdk.core.MediaInfo
import com.connectsdk.core.SubtitleInfo
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.device.PairingDialog
import com.connectsdk.service.DeviceService
import com.connectsdk.service.DeviceService.PairingType
import com.connectsdk.service.capability.Launcher
import com.connectsdk.service.capability.MediaControl
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.capability.MediaPlayer.MediaLaunchObject
import com.connectsdk.service.capability.listeners.ResponseListener
import com.connectsdk.service.command.ServiceCommandError
import com.connectsdk.service.sessions.LaunchSession
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.database.DeviceDao
import com.mentos_koder.remote_lg_tv.database.LGApiService
import com.mentos_koder.remote_lg_tv.model.Device
import com.mentos_koder.remote_lg_tv.notifiSocket.AutoConnectWebSocket
import com.mentos_koder.remote_lg_tv.notifiSocket.RelationshipWebSocket
import com.mentos_koder.remote_lg_tv.view.MainApplication
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class Singleton {
    private var relationshipWebSocket: RelationshipWebSocket? = null
    private var autoConnectWebSocket: AutoConnectWebSocket? = null
    var deviceConnected: ConnectableDevice? = null
    var isConnectedCustom = false
    var typeDeviceCustom = ""
    private var nameDevice = ""
    private val contextApp: Context = MainApplication.getAppContext()
    val lG = "lg"
    var checkIpAddress = ""
    private lateinit var sharedPreferences: SharedPreferences
    var mLaunchSession: LaunchSession? = null
    var mMediaControl: MediaControl? = null
    val _pathList = mutableListOf<String>()
    var subtitles: SubtitleInfo? = null
    private var activity: Activity? = null
    val duplicateDevicesList = ArrayList<ConnectableDevice>()

    fun addBitmap(path: String) {
        _pathList.add(path)
    }

    fun containsBitmap(path: String): Boolean {
        return _pathList.contains(path)
    }

    fun getPathList(): List<String> {
        return _pathList.toList()
    }

    val deviceListenerPinCode = object : DeviceService.DeviceServiceListener {
        override fun onConnectionRequired(service: DeviceService?) {
        }

        override fun onConnectionSuccess(service: DeviceService?) {
            setConnected(true)
        }

        override fun onCapabilitiesUpdated(
            service: DeviceService?, added: MutableList<String>?, removed: MutableList<String>?
        ) {
        }

        override fun onDisconnect(service: DeviceService?, error: Error?) {
            setConnected(false)
        }

        override fun onConnectionFailure(service: DeviceService?, error: Error?) {
            setConnected(false)
        }

        override fun onPairingRequired(
            service: DeviceService?, pairingType: PairingType?, pairingData: Any?
        ) {
        }

        override fun onPairingSuccess(service: DeviceService?) {
        }

        override fun onPairingFailed(service: DeviceService?, error: Error?) {
        }

    }

    private val deviceListener = object : ConnectableDeviceListener {
        override fun onPairingRequired(
            device: ConnectableDevice, service: DeviceService, pairingType: PairingType
        ) {
            if (pairingType == PairingType.PIN_CODE) {
                showDialog(deviceConnected!!)
                service.listener = deviceListenerPinCode
            } else {
                setConnected(false)
            }
        }

        override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
            Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
        }

        override fun onDeviceReady(device: ConnectableDevice) {
            try {
                saveRoku()
            } catch (_: NullPointerException) {
            }
        }

        override fun onDeviceDisconnected(device: ConnectableDevice) {
        }

        override fun onCapabilityUpdated(
            device: ConnectableDevice, added: List<String>, removed: List<String>
        ) {
        }
    }


    private fun saveRoku() {
        val deviceDao: DeviceDao = AppDatabase.getDatabase(contextApp).deviceDao()
        val deviceCount = deviceDao.countDevicesWithAddress(deviceConnected?.ipAddress ?: "")
        if (deviceCount == 0) {
            val device = Device()
            device.address = deviceConnected?.ipAddress ?: ""
            device.model = deviceConnected?.modelName ?: ""
            device.firstDateConnect = getCurrentTime()
            device.lastDateConnect = getCurrentTime()
            device.name = deviceConnected?.friendlyName ?: ""
            device.typeDevice = deviceConnected?.serviceName ?: ""
            device.typeConnect = "handwork"
            deviceDao.insert(device)
        } else {
            val device = deviceDao.getDeviceByAddress(deviceConnected?.ipAddress ?: "")
            if (device != null) {
                device.lastDateConnect = getCurrentTime()
                device.typeConnect = "handwork"
            }
            if (device != null) {
                deviceDao.update(device)
            }
        }
    }

    fun setConnected(connected: Boolean) {
        isConnectedCustom = connected
    }

    fun isConnected(): Boolean {
        return isConnectedCustom
    }

    fun setIpAddressFlag(ipAddress: String) {
        checkIpAddress = ipAddress
    }

    fun getIpAddress(): String {
        return checkIpAddress
    }

    companion object {
        private var instance: Singleton? = null

        @Synchronized
        fun getInstance(): Singleton {
            if (instance == null) {
                instance = Singleton()

            }
            return instance!!
        }
    }

    private fun sendKeyPromise(jsonString: JSONObject) {
        if (relationshipWebSocket != null) {
            relationshipWebSocket?.send(jsonString.toString())
        } else {
            autoConnectWebSocket?.send(jsonString.toString())
        }
    }

    fun getCommandByInput(eventName: String?) {
        val jsonString: JSONObject? = eventName?.let { convertEventInputToJson(it) }
        if (jsonString != null) {
            sendKeyPromise(jsonString)
        }
    }

    private fun convertEventInputToJson(eventName: String): JSONObject {
        val command = JSONObject()
        try {
            val params = JSONObject()
            params.put("Cmd", eventName)
            params.put("DataOfCmd", "eventName")
            params.put("Option", "false")
            params.put("TypeOfRemote", "SendInputString")
            command.put("method", "ms.remote.control")
            command.put("params", params)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return command
    }

    fun disconnect() {
        if (relationshipWebSocket != null) {
            relationshipWebSocket?.close()
        } else {
            autoConnectWebSocket?.close()
        }
    }

    fun autoConnectURI(deviceName: String?, ipAddress: String, token: String?) {
        var tokenDevice = token
        val base64Name: String? = deviceName?.let { getStringBase64From(it) }
        if (!token.isNullOrEmpty()) {
            tokenDevice = "&token=$token"
        }
        val path =
            "wss://$ipAddress:8002/api/v2/channels/samsung.remote.control?name=$base64Name$tokenDevice"
        val uri = createWebSocketURI(path)
        if (autoConnectWebSocket != null && autoConnectWebSocket!!.isOpen) {
            autoConnectWebSocket!!.close()
        }
        try {
            val sslContext = createSSLContext()
            val sslSocketFactory = sslContext.socketFactory
            autoConnectWebSocket = AutoConnectWebSocket(uri!!, deviceName, ipAddress)
            autoConnectWebSocket!!.setSocketFactory(sslSocketFactory)
            autoConnectWebSocket!!.connectionLostTimeout = 60
            autoConnectWebSocket!!.connect()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }


    object LGApiClient {
        private var retrofit: Retrofit? = null
        lateinit var ipAdd: String
        lateinit var idApp: String

        fun getClient(ipAddress: String, appid: String): Retrofit {
            if (retrofit == null) {
                ipAdd = ipAddress
                idApp = appid
                val baseUrl = "http://$ipAddress:8001/api/v2/$appid"
                val okHttpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
                retrofit = Retrofit.Builder().baseUrl(baseUrl).client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create()).build()
            }
            return retrofit!!
        }

        val service: LGApiService by lazy {
            getClient(ipAdd, idApp).create(LGApiService::class.java)
        }

    }

    private fun getStringBase64From(text: String): String {
        val encodedBytes: ByteArray? =
            java.util.Base64.getEncoder().encode(text.toByteArray(StandardCharsets.UTF_8))
        return String(encodedBytes!!, StandardCharsets.UTF_8)
    }

    private fun createWebSocketURI(uriString: String): URI? {
        return try {
            URI(uriString)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            null
        }
    }

    private fun createSSLContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLSv1.2")
        try {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null, null)
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>, authType: String
                ) {
                    Log.d("checkClientTrusted", "checkClientTrusted: ")
                }

                override fun checkServerTrusted(
                    chain: Array<X509Certificate>, authType: String
                ) {
                    Log.d("checkServerTrusted", "checkServerTrusted: ")
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(ks, null)
            sslContext.init(null, trustAllCerts, SecureRandom())
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: UnrecoverableKeyException) {
            e.printStackTrace()
        }
        return sslContext
    }

    private fun getTypeTV(device: ConnectableDevice?): String {
        val name: String = device?.friendlyName?.lowercase(Locale.ROOT) ?: ""
        val serviceName: String = device?.serviceName?.lowercase(Locale.ROOT) ?: ""
        val manufacturer: String = device?.manufacturer?.lowercase(Locale.ROOT) ?: ""

        return when {
            name.contains(lG) || serviceName.contains(lG) || manufacturer.contains(lG) -> lG
            else -> {
                lG
            }
        }
    }

    private fun setNameDevice(name: String) {
        nameDevice = name
    }

    fun getNameDevice(): String {
        return nameDevice
    }

    fun getTypeDevice(): String {
        return getTypeTV(deviceConnected)
    }

    fun handelTypeTV(
        device: ConnectableDevice?, context: Context, onBack: OnBack?
    ) {
        deviceConnected = device
        val type = getTypeTV(deviceConnected).lowercase(Locale.ROOT)
        deviceConnected?.addListener(deviceListener)
        setNameDevice(deviceConnected?.friendlyName ?: "")
        deviceConnected?.let { LGApiClient.getClient(it.ipAddress, "") }
        setIpAddressFlag(deviceConnected!!.ipAddress)
        when (type) {
            "lg" -> {
                sharedPreferences =
                    context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
                val isFirstLogin = sharedPreferences.getBoolean(Constants.IS_FIRST_LOGIN, true)
                if (isFirstLogin) {
                    deviceConnected!!.setPairingType(PairingType.PIN_CODE)
                    connectDeviceHandle(type, onBack)
                    sharedPreferences.edit {
                        putBoolean(Constants.IS_FIRST_LOGIN, false)
                    }
                } else {
                    connectDeviceHandle(type, onBack)
                }
            }
        }
    }


    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun showDialog(device: ConnectableDevice) {
        val context = activity ?: return
        context.showDialogPairing {
            for (service in device.services)
                service.sendPairingKey(it)
        }
    }

    private fun connectDeviceHandle(type: String, onBack: OnBack?) {
        deviceConnected?.connect()
        if (deviceConnected?.isConnected == true) {
            setConnected(true)
            typeDeviceCustom = type
            onBack?.back(0)
        }
    }

    fun getService(keycode: String?) {
        val key = deviceConnected!!.keyControl
        key.sendKeyCodeString(keycode, object : ResponseListener<Any?> {
            override fun onError(error: ServiceCommandError) {
            }

            override fun onSuccess(`object`: Any?) {
            }
        })
    }

    fun sendText(text: String) {
        val key = deviceConnected!!.textInputControl
        key.sendText(text)
    }

    fun deleteText() {
        val key = deviceConnected!!.textInputControl
        key.sendDelete()
    }

    fun disconnectDevice() {
        deviceConnected?.disconnect()
        setConnected(false)
    }

    fun getImage(callback: GetImageCallback) {
        if (isConnectedCustom) {
            deviceConnected?.launcher?.getAppList(object : Launcher.AppListListener {
                override fun onSuccess(appList: List<AppInfo>) {
                    callback.onSuccess(
                        appList,
                        deviceConnected?.ipAddress ?: "",
                        deviceConnected?.friendlyName ?: ""
                    )
                }

                override fun onError(error: ServiceCommandError) {
                    Log.d("####", "onError: $error")
                    callback.onError(
                        error, deviceConnected?.ipAddress ?: "", deviceConnected?.friendlyName ?: ""
                    )
                }
            })
        } else {
            callback.onSuccess(emptyList(), "", "")
        }
    }

    fun getImageLG(callback: GetImageCallbackLG) {
        if (isConnectedCustom) {
            deviceConnected?.launcher?.getLaunchPoints(object : Launcher.LaunchPointsListener {

                override fun onError(error: ServiceCommandError) {
                    Log.d("####", "onError: $error")
                    callback.onError(
                        error, deviceConnected?.ipAddress ?: "", deviceConnected?.friendlyName ?: ""
                    )
                }

                override fun onSuccess(objectLG: JSONArray?) {
                    callback.onSuccess(
                        objectLG,
                        deviceConnected?.ipAddress ?: "",
                        deviceConnected?.friendlyName ?: ""
                    )

                }
            })
        } else {
            callback.onSuccess(null, "", "")
        }
    }

    fun openAppOnTV(idLaunchApp: String) {
        if (isConnectedCustom) {
            deviceConnected?.launcher?.launchApp(idLaunchApp, object : Launcher.AppLaunchListener {
                override fun onSuccess(session: LaunchSession) {
                }

                override fun onError(error: ServiceCommandError) {
                }
            })
        }
    }

    fun showMediaImage(
        mediaURL: String,
        path: String,
        mimeType: String,
        title: String,
        iconURL: String,
        description: String
    ) {
        val pathRP = path.replace(" ", "")
        val mediaInfo: MediaInfo = MediaInfo.Builder(mediaURL + pathRP, mimeType).setTitle(title)
            .setDescription(description).setIcon(iconURL).build()
        Log.d("AppTag###", "showMediaImage: " + mediaURL + pathRP)
        val launchListener = object : MediaPlayer.LaunchListener {
            override fun onSuccess(`object`: MediaLaunchObject) {
                mLaunchSession = `object`.launchSession
                mMediaControl = `object`.mediaControl
                Log.d("AppTag###", "onSuccess showMediaImage")
            }

            override fun onError(error: ServiceCommandError) {
                Log.d("AppTag###", "Display photo failure: $error")
            }
        }

        val type = getTypeTV(deviceConnected).lowercase(Locale.ROOT)
        if (type == "fire") {
            duplicateDevicesList.forEach { device ->
                if (device.connectedServiceNames != null) {
                    device.mediaPlayer?.displayImage(mediaInfo, launchListener)
                }
            }

        } else {
            deviceConnected?.mediaPlayer?.displayImage(mediaInfo, launchListener)
        }
    }

    fun showMediaVideoAndAudio(
        mediaURL: String,
        path: String,
        mimeType: String,
        title: String,
        iconURL: String,
        description: String
    ) {
        val pathRP = path.replace(" ", "");
        Log.d("AppTag###", "showMediaVideo: " + pathRP)
        Log.d("AppTag###", "showMediaVideo: " + mediaURL + pathRP)

        subtitles =
            SubtitleInfo.Builder("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/sintel_en.vtt")
                .setMimeType("text/vtt").setLanguage("en").setLabel("English subtitles").build()

        val mediaInfo: MediaInfo = MediaInfo.Builder(mediaURL + pathRP, mimeType).setTitle(title)
            .setDescription(description).setIcon(iconURL).setSubtitleInfo(subtitles!!).build()
        val launchListener = object : MediaPlayer.LaunchListener {
            override fun onSuccess(`object`: MediaLaunchObject) {
                mLaunchSession = `object`.launchSession
                mMediaControl = `object`.mediaControl
                Log.d("AppTag###", "onSuccess showMediaVideo")
            }

            override fun onError(error: ServiceCommandError) {
                Log.d("AppTag###", "Display Video failure: ${error.message}")
            }
        }

        val type = getTypeTV(deviceConnected).lowercase(Locale.ROOT)
        if (type == "fire") {
            duplicateDevicesList.forEach { device ->
                if (device.connectedServiceNames != null) {
                    device.mediaPlayer?.playMedia(mediaInfo, false, launchListener)
                }
            }

        } else {
            deviceConnected?.mediaPlayer?.playMedia(mediaInfo, false, launchListener)
        }
    }

    fun stopAudio() {
        val type = getTypeTV(deviceConnected).lowercase(Locale.ROOT)
        if (type == "fire") {
            duplicateDevicesList.forEach { device ->
                if (device.connectedServiceNames != null) {
                    device.mediaControl?.pause(null)
                }
            }
        } else {
            deviceConnected?.mediaControl?.pause(null)
        }

    }

    fun playAudio() {
        val type = getTypeTV(deviceConnected).lowercase(Locale.ROOT)
        if (type == "fire") {
            duplicateDevicesList.forEach { device ->
                if (device.connectedServiceNames != null) {
                    device.mediaControl?.play(null)
                }
            }
        } else {
            deviceConnected?.mediaControl?.play(null)
        }
    }

    interface GetImageCallback {
        fun onSuccess(appList: List<AppInfo>, ipAddress: String, name: String)
        fun onError(error: ServiceCommandError, ipAddress: String, name: String)
    }

    interface GetImageCallbackLG {
        fun onSuccess(objectLG: JSONArray?, ipAddress: String, name: String)
        fun onError(error: ServiceCommandError, ipAddress: String, name: String)
    }

    private fun getCurrentTime(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTime = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }
}