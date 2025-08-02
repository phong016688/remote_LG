package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.content.SharedPreferences
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
import com.bumptech.glide.Glide
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import android.util.Base64
import com.mentos_koder.remote_lg_tv.model.Favorite
import com.mentos_koder.remote_lg_tv.util.ImageLoader
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.core.content.edit
import com.mentos_koder.remote_lg_tv.util.Constants

class FavouriteAppAdapter(
    private val context: Context,
    private val listApp: MutableList<Favorite>?,
    private val ipAddress: String
) : RecyclerView.Adapter<FavouriteAppAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favourite = listApp!![position]
        val id = favourite.id
        holder.txtNameChannel.text = favourite.name
        val imgType = getType()
        val ip = "http://" + favourite.ipAddress + ":8060/query/icon/" + id
        val iconUrl = favourite.iconLink

        when (imgType) {
            "lg" -> {
                val bitmapLoaderTask = BitmapLoaderTask(holder.imgChannel)
                bitmapLoaderTask.execute(iconUrl)
            }

            "office", "vizio" -> {
                val drawable =
                    favourite.iconLink?.let { ImageLoader.loadImageFromMipmap(context, it) }
                holder.imgChannel.setImageDrawable(drawable)
            }

            "fire", "amazon" -> {
                Glide.with(context).load(favourite.iconLink).into(holder.imgChannel)
            }

            else -> {
                if (favourite.iconLocal != null) {
                    bindIconLocal(holder.imgChannel, favourite.iconLocal, context)
                } else {
                    Glide.with(context).load(ip).into(holder.imgChannel)
                }
            }
        }
        val preferences =
            context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val isFavourite = preferences.getBoolean(Constants.KEY_FAVORITE + id, false)
        holder.imgFavourite.isSelected = isFavourite
        holder.imgFavourite.setOnClickListener {
            performVibrateAction()
            setStatusSharedPreferences(holder, preferences, favourite)
            deleteDateFavourite(favourite)
            setNotifyDataSetChanged(holder)
        }
        holder.imgChannel.setOnClickListener {
            performVibrateAction()
            checkType(id)
        }
    }

    private fun checkRing(): Boolean {
        return restoreSwitchState(context)
    }

    private fun performVibrateAction() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (checkRing()) {
            vibrator.vibrate(100)
        }
    }

    private fun deleteDateFavourite(favorite: Favorite) {
        val deviceDao = AppDatabase.getDatabase(context).deviceDao()
        deviceDao.deleteFavourite(favorite.id)
    }

    private fun setStatusSharedPreferences(
        holder: ViewHolder, preferences: SharedPreferences, favorite: Favorite
    ) {
        preferences.edit {
            holder.imgFavourite.isSelected = false
            putBoolean(Constants.KEY_FAVORITE + favorite.id, false)
        }
    }

    private fun setNotifyDataSetChanged(holder: ViewHolder) {
        val currentPosition = holder.adapterPosition
        listApp?.let {
            if (currentPosition != RecyclerView.NO_POSITION) {
                it.removeAt(currentPosition)
                notifyItemRemoved(currentPosition)
                notifyDataSetChanged()
            }
        }
    }

    private fun bindIconLocal(imageView: ImageView, iconLocal: String?, context: Context) {
        iconLocal?.let { icon ->
            val iconByteArray = Base64.decode(icon, Base64.DEFAULT)
            val iconBitmap = BitmapFactory.decodeByteArray(iconByteArray, 0, iconByteArray.size)
            imageView.setImageBitmap(iconBitmap)
        }
    }

    override fun getItemCount(): Int {
        return listApp?.size ?: 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtNameChannel: TextView = itemView.findViewById(R.id.txt_NameChannel)
        var imgChannel: ImageView = itemView.findViewById(R.id.img_Channel)
        var imgFavourite: ImageView = itemView.findViewById(R.id.img_favourite)
    }

    private fun getType(): String {
        val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
        return type
    }

    private fun checkType(id: String) {
        val type = getType()
        when (type) {
            "lg" -> {
                Singleton.getInstance().openAppOnTV(id)
            }

            else -> {
                Singleton.getInstance().openAppOnTV(id)
            }
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

