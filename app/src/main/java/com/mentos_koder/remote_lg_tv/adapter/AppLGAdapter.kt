package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mentos_koder.remote_lg_tv.event.OnItemClickListener
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.database.DeviceDao
import com.mentos_koder.remote_lg_tv.model.Favorite
import com.mentos_koder.remote_lg_tv.util.Constants
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class AppLGAdapter(
    private val context: Context,
    private val listApp: JSONArray,
    private val ipAddress: String,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val VIEW_TYPE_APP = 1
    val VIEW_TYPE_TEMPLATE = 2
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_APP -> {
                val view: View = inflater.inflate(R.layout.item_app, parent, false)
                ViewHolder(view)
            }

            VIEW_TYPE_TEMPLATE -> {
                val view: View = inflater.inflate(R.layout.item_app, parent, false)
                ViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d(
            "Adapter",
            "onBindViewHolder - position: $position, viewType: ${getItemViewType(position)}"
        )
        when (getItemViewType(position)) {
            VIEW_TYPE_APP -> {
                val app = listApp.getJSONObject(position)
                val appHolder = holder as AppLGAdapter.ViewHolder
                appHolder.bind(app)
            }
        }
    }


    private fun insertDevice(
        favorite: Favorite, app: JSONObject, ip: String, deviceDao: DeviceDao
    ) {
        favorite.id = app.getString("id")
        favorite.name = app.getString("title")
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

    override fun getItemCount(): Int {
        return if (listApp.length() != 0) {
            listApp.length()
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 3) VIEW_TYPE_TEMPLATE else VIEW_TYPE_APP

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var txtNameChannel: TextView = itemView.findViewById(R.id.txt_NameChannel)
        private var imgChannel: ImageView = itemView.findViewById(R.id.img_Channel)
        private var imgFavourite: ImageView = itemView.findViewById(R.id.img_favourite)
        fun bind(app: JSONObject) {
            val appName = app.getString("title")
            val iconUrl = app.getString("icon")
            val id = app.getString("id")

            txtNameChannel.text = appName


            val bitmapLoaderTask = BitmapLoaderTask(imgChannel)
            bitmapLoaderTask.execute(iconUrl)

            val preferences =
                context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
            val isFavourite = preferences.getBoolean(Constants.KEY_FAVORITE + id, false)

            imgFavourite.isSelected = isFavourite
            imgFavourite.setOnClickListener { _: View? ->
                performVibrateAction()
                val isFavouriteCheck = preferences.getBoolean(Constants.KEY_FAVORITE + id, false)
                val editor = preferences.edit()
                val deviceDao = AppDatabase.getDatabase(context).deviceDao()
                val favorite = Favorite()
                val count = deviceDao.countFavouriteWithId(id)

                if (isFavouriteCheck) {
                    imgFavourite.isSelected = false
                    editor.putBoolean(Constants.KEY_FAVORITE + id, false).apply()
                    deviceDao.deleteFavourite(id)
                } else {
                    imgFavourite.isSelected = true
                    editor.putBoolean(Constants.KEY_FAVORITE + id, true).apply()
                    if (count > 0) {
                        deviceDao.updateFavourite(true, id)
                    } else {
                        insertDevice(favorite, app, iconUrl, deviceDao)
                    }
                }
                itemClickListener.onItemClicked()
            }
            imgChannel.setOnClickListener {
                performVibrateAction()
                Singleton.getInstance().openAppOnTV(id)
            }
        }

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

    private object CustomTrustManager : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<java.security.cert.X509Certificate>, authType: String
        ) {
        }

        override fun checkServerTrusted(
            chain: Array<java.security.cert.X509Certificate>, authType: String
        ) {
        }

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    }


    private fun setCustomTrustManagerForUrl(url: String) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(CustomTrustManager)
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.sslSocketFactory = sslContext.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ ->
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class BitmapLoaderTask(private val imgChannel: ImageView) :
        AsyncTask<String, Void, Bitmap?>() {
        override fun doInBackground(vararg params: String): Bitmap? {
            val urlString = params[0]
            return loadBitmapFromUrl(urlString) { url ->
                setCustomTrustManagerForUrl(url)
            }
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            if (bitmap != null) {
                imgChannel.setImageBitmap(bitmap)
                Log.d("BitmapLoaderTask", "Bitmap loaded successfully")
            } else {
                Log.e("BitmapLoaderTask", "Failed to load bitmap")
            }
        }
    }

    private fun loadBitmapFromUrl(
        urlString: String, trustManagerFunction: (String) -> Unit
    ): Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var bitmap: Bitmap? = null

        try {
            trustManagerFunction(urlString)

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = BufferedInputStream(connection.inputStream)
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
        return bitmap
    }
}
