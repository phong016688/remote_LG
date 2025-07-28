package com.mentos_koder.remote_lg_tv.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
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
import com.mentos_koder.AndroidRemoteTv
import com.mentos_koder.AndroidTvListener
import com.mentos_koder.remote.Remotemessage
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.database.DeviceDao
import com.mentos_koder.remote_lg_tv.database.LGApiService
import com.mentos_koder.remote_lg_tv.model.Device
import com.mentos_koder.remote_lg_tv.notifiSocket.AutoConnectWebSocket
import com.mentos_koder.remote_lg_tv.notifiSocket.RelationshipWebSocket
import com.mentos_koder.remote_lg_tv.view.MainActivity
import com.mentos_koder.remote_lg_tv.view.MainApplication
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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
    var isLoaderDataApi = false
    var checkIpAddress = ""
    private lateinit var sharedPreferences: SharedPreferences
    var mLaunchSession: LaunchSession? = null
    var mMediaControl: MediaControl? = null
    val _pathList = mutableListOf<String>()
    var subtitles: SubtitleInfo? = null
    var pos = 0
    private var activity: Activity? = null
    val duplicateDevicesList = ArrayList<ConnectableDevice>()
    val client = OkHttpClient.Builder()
        .sslSocketFactory(
            TrustConnection.createUnsafeSSLSocketFactory(),
            TrustConnection.TrustAllCerts()
        )
        .hostnameVerifier { _, _ -> true }
        .build()

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
            Log.d("####Singleton", "onConnectionRequired")
        }

        override fun onConnectionSuccess(service: DeviceService?) {
            Log.d("####Singleton", "onConnectionSuccess")
            setConnected(true)
        }

        override fun onCapabilitiesUpdated(
            service: DeviceService?,
            added: MutableList<String>?,
            removed: MutableList<String>?
        ) {
            Log.d("####Singleton", "onCapabilitiesUpdated")
        }

        override fun onDisconnect(service: DeviceService?, error: Error?) {
            Log.d("####Singleton", "onDisconnect")
            setConnected(false)
        }

        override fun onConnectionFailure(service: DeviceService?, error: Error?) {
            Log.d("####Singleton", "onConnectionFailure")
            setConnected(false)
        }

        override fun onPairingRequired(
            service: DeviceService?,
            pairingType: PairingType?,
            pairingData: Any?
        ) {
            Log.d("####Singleton", "onPairingRequired")
        }

        override fun onPairingSuccess(service: DeviceService?) {
            Log.d("####Singleton", "onPairingSuccess")
        }

        override fun onPairingFailed(service: DeviceService?, error: Error?) {
            Log.d("####Singleton", "onPairingFailed")
        }

    }

    private val deviceListener = object : ConnectableDeviceListener {
        override fun onPairingRequired(
            device: ConnectableDevice,
            service: DeviceService,
            pairingType: PairingType
        ) {
            Log.d("####", "onPairingRequired Connected to " + deviceConnected!!.ipAddress)
            if (pairingType == PairingType.PIN_CODE) {
                showDialog(deviceConnected!!)
                service.listener = deviceListenerPinCode
            } else {
                Log.d("####", "Pin Code wrong: " + pairingType)
                setConnected(false)
            }
        }

        override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
            Log.d("####", "onConnectFailed")
        }

        override fun onDeviceReady(device: ConnectableDevice) {
            try {
                Log.d("####", "onDeviceReady getFriendlyName: " + device.friendlyName)
                saveRoku()
            } catch (e: NullPointerException) {
                Log.e("####", "Error occurred: " + e.message)
            }
        }

        override fun onDeviceDisconnected(device: ConnectableDevice) {
            Log.d("###", "Device Disconnected")
        }

        override fun onCapabilityUpdated(
            device: ConnectableDevice,
            added: List<String>,
            removed: List<String>
        ) {
            Log.d("###", "onCapabilityUpdated")
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
            Log.d("#####", "Device saved successfully")
        } else {
            val device = deviceDao.getDeviceByAddress(deviceConnected?.ipAddress ?: "")
            if (device != null) {
                device.lastDateConnect = getCurrentTime()
                device.typeConnect = "handwork"
            }
            if (device != null) {
                deviceDao.update(device)
            }
            Log.d("#####", "Device updated successfully")
        }
    }

    fun setConnected(connected: Boolean) {
        isConnectedCustom = connected
    }

    fun isConnected(): Boolean {
        return isConnectedCustom
    }

    fun setLoaderDataApiFlag(loaderApi: Boolean) {
        isLoaderDataApi = loaderApi
    }

    fun getLoaderDataApi(): Boolean {
        return isLoaderDataApi
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

    fun getCommandByKey(eventName: String) {
        val jsonString = convertEventToJson(eventName)
        Log.d("####", "getCommandByKey: $jsonString")
        sendKeyPromise(jsonString)
    }

    private fun convertEventToJson(eventName: String): JSONObject {
        val command = JSONObject()
        try {
            val params = JSONObject()
            params.put("Cmd", "Click")
            params.put("DataOfCmd", eventName)
            params.put("Option", "false")
            params.put("TypeOfRemote", "SendRemoteKey")
            command.put("method", "ms.remote.control")
            command.put("params", params)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return command
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
        Log.d("####", "getCommandByKey: $jsonString")
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

    fun ConnectURI(
        model: String?,
        friendlyName: String?,
        deviceName: String?,
        ipAddress: String,
        token: String?
    ) {
        val base64Name: String? = deviceName?.let { getStringBase64From(it) }
        var tokenn = ""
        if (!token.isNullOrEmpty()) {
            tokenn = "&token=$token"
        }
        val path =
            "wss://$ipAddress:8002/api/v2/channels/samsung.remote.control?name=$base64Name$tokenn"
        val uri = createWebSocketURI(path)
        if (relationshipWebSocket != null && relationshipWebSocket!!.isOpen) {
            relationshipWebSocket!!.close()
        }
        try {
            val sslContext = createSSLContext()
            val sslSocketFactory = sslContext.socketFactory
            relationshipWebSocket =
                RelationshipWebSocket(uri!!, model, friendlyName, deviceName, ipAddress)
            relationshipWebSocket!!.setSocketFactory(sslSocketFactory)
            relationshipWebSocket!!.connectionLostTimeout = 60
            relationshipWebSocket!!.connect()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }

    fun AutoConnectURI(deviceName: String?, ipAddress: String, token: String?) {
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
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
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
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                        Log.d("checkClientTrusted", "checkClientTrusted: ")
                    }

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                        Log.d("checkServerTrusted", "checkServerTrusted: ")
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }
            )
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
        device: ConnectableDevice?,
        deviceDao: DeviceDao,
        deviceCount: Int,
        showCustomDialog: Runnable,
        context: Context,
        onBack: OnBack?
    ) {
        deviceConnected = device
        val type = getTypeTV(deviceConnected).lowercase(Locale.ROOT)
        deviceConnected?.addListener(deviceListener)
        setNameDevice(deviceConnected?.friendlyName ?: "")
        Log.d("###", "handelTypeTV: $type")
        deviceConnected?.let { LGApiClient.getClient(it.ipAddress, "") }
        setIpAddressFlag(deviceConnected!!.ipAddress)
        Log.d("handelTypeTV", "handelTypeTV: " + type)
        Log.d("handelTypeTV", "handelTypeTV: " + deviceConnected!!.ipAddress)
        when (type) {

            "samsung" -> checkTokenSamsung(
                device,
                deviceDao,
                deviceCount,
                showCustomDialog,
                context
            )

            "lg" -> {
                sharedPreferences = context.getSharedPreferences("pinCheck", Context.MODE_PRIVATE)
                val isFirstLogin = sharedPreferences.getBoolean("is_first_login", true)
                if (isFirstLogin) {
                    deviceConnected!!.setPairingType(PairingType.PIN_CODE)
                    connectDeviceHandle(type, onBack)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("is_first_login", false)
                    editor.apply()
                } else {
                    connectDeviceHandle(type, onBack)
                }
            }

            "android" -> {
                connectAndroidTv(deviceConnected!!.ipAddress)
            }

            else -> {
                connectDeviceHandle(type, null)
            }
        }
    }

    private fun connectAndroidTv(ip: String) {
        Thread {
            try {
                val androidTV = AndroidRemoteTv()
                androidTV.connect(ip, androidTvListener)
            } catch (e: Exception) {
                Log.e("AndroidRemoteTv", "testI: " + e.message)
            }
        }.start()
    }

    val androidTvListener = object : AndroidTvListener {
        val androidRemoteTv = AndroidRemoteTv()
        override fun onSessionCreated() {
            Log.d("AndroidRemoteTv", "onSessionCreated: ")
        }

        override fun onSecretRequested() {
            val reader = BufferedReader(
                InputStreamReader(System.`in`)
            )

            try {
                val name = reader.readLine()
                androidRemoteTv.sendSecret(name)
                Log.d("AndroidRemoteTv", "onSecretRequested: $name")
            } catch (e: IOException) {
                Log.e("AndroidRemoteTv", "onSecretRequested: " + e.message)
                throw RuntimeException(e)
            }
        }

        override fun onPaired() {
            Log.d("AndroidRemoteTv", "onPaired ")
        }

        override fun onConnectingToRemote() {
            Log.d("AndroidRemoteTv", "onConnectingToRemote ")
        }

        override fun onConnected() {
            Log.d("AndroidRemoteTv", "onConnected ")
            androidRemoteTv.sendCommand(
                Remotemessage.RemoteKeyCode.KEYCODE_POWER,
                Remotemessage.RemoteDirection.SHORT
            )
        }

        override fun onDisconnect() {
            Log.d("AndroidRemoteTv", "onDisconnect ")
        }

        override fun onError(error: String) {
            Log.e("AndroidRemoteTv", "onError $error")
        }

    }


    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun showDialog(device: ConnectableDevice) {
        val context = activity ?: return
        val pairingDialog = PairingDialog(context, device)
        val pairing: AlertDialog = pairingDialog.getPairingDialog("Enter Pairing Code on TV")
        pairing.show()
    }

    private fun connectDeviceHandle(type: String, onBack: OnBack?) {
        deviceConnected?.connect()
        if (deviceConnected?.isConnected == true) {
            setConnected(true)
            typeDeviceCustom = type
            onBack?.back(0)
        }
    }

    private fun checkTokenSamsung(
        device: ConnectableDevice?,
        deviceDao: DeviceDao,
        deviceCount: Int,
        showCustomDialog: Runnable,
        context: Context
    ) {
        device?.let {
            if (deviceCount > 0) {
                val deviceDB = deviceDao.getDeviceByAddress(it.ipAddress)
                if (deviceDB != null) {
                    deviceDB.typeDevice?.let { it1 ->
                        AutoConnectURI(
                            it1,
                            deviceDB.address,
                            deviceDB.token
                        )
                    }
                }
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)

            } else {
                ConnectURI(
                    device.modelName ?: "",
                    device.friendlyName ?: "",
                    device.manufacturer ?: "",
                    device.ipAddress ?: "",
                    null
                )
                showCustomDialog.run()
            }
        }
    }

    fun getService(keycode: String?) {
        val key = deviceConnected!!.keyControl
        key.sendKeyCodeString(keycode, object : ResponseListener<Any?> {
            override fun onError(error: ServiceCommandError) {
                Log.e("####", "onError: ", error)
            }

            override fun onSuccess(`object`: Any?) {
                Log.d("####", "onSuccess")
            }
        })
    }

    fun sendText(text: String) {
        val key = deviceConnected!!.textInputControl
        key.sendText(text)
        Log.d("909023", "sendText: $text")
    }

    fun deleteText() {
        Log.d("sendDelete", "sendDelete: ")
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
                        error,
                        deviceConnected?.ipAddress ?: "",
                        deviceConnected?.friendlyName ?: ""
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
                        error,
                        deviceConnected?.ipAddress ?: "",
                        deviceConnected?.friendlyName ?: ""
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
                    Log.d("####", "onSuccess LaunchSession: ")
                }

                override fun onError(error: ServiceCommandError) {
                    Log.d("####", "onError LaunchSession: $error")
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
        val mediaInfo: MediaInfo = MediaInfo.Builder(mediaURL + pathRP, mimeType)
            .setTitle(title)
            .setDescription(description)
            .setIcon(iconURL)
            .build()
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
                .setMimeType("text/vtt")
                .setLanguage("en")
                .setLabel("English subtitles")
                .build()

        val mediaInfo: MediaInfo = MediaInfo.Builder(mediaURL + pathRP, mimeType)
            .setTitle(title)
            .setDescription(description)
            .setIcon(iconURL)
            .setSubtitleInfo(subtitles!!)
            .build()
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