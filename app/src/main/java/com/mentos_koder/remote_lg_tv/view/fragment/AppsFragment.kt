package com.mentos_koder.remote_lg_tv.view.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectsdk.core.AppInfo
import com.connectsdk.service.command.ServiceCommandError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mentos_koder.remote_lg_tv.event.OnItemClickListener
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.AppAdapter
import com.mentos_koder.remote_lg_tv.adapter.AppLGAdapter
import com.mentos_koder.remote_lg_tv.adapter.FavouriteAppAdapter
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.model.LGAppInfo
import com.mentos_koder.remote_lg_tv.model.Favourite
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.Singleton.GetImageCallback
import org.json.JSONArray
import java.util.Locale


class AppsFragment : Fragment(), OnItemClickListener {
    private var appAdapter: AppAdapter? = null
    private var appLGAdapter: AppLGAdapter? = null
    private var favouriteAppAdapter: FavouriteAppAdapter? = null
    private var channelRecyclerView: RecyclerView? = null
    private var favouriteRecyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var nameDV: TextView? = null
    private var tvChannel: TextView? = null
    private var tvFavourite: TextView? = null
    private var castButton: ImageView? = null
    private var linerFavourite: LinearLayout? = null
    private var linearNoConnect: LinearLayout? = null
    private var linearChannel: LinearLayout? = null
    private var btnConnect: Button? = null

    lateinit var ipAddress: String
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_apps, container, false)
        initializeViews(view)
        val singleton = Singleton.getInstance()
        ipAddress = Singleton.getInstance().getIpAddress()
        setupRecyclerViews()
        checkConnect(singleton)
        setTextName()
        setDisconnect(singleton)
        return view
    }

    private fun checkConnect(singleton: Singleton) {
        if (singleton.isConnectedCustom) {
            visibleConnect()
            val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
            Log.d("####", "displayDefaultFragment: $type")
            if (type == "lg") {
                tvChannel?.text = getString(R.string.all_apps)
                tvFavourite?.text = getString(R.string.favourite_apps)
                loadAppsLG(singleton)
            } else {
                loadApps(singleton)
            }
            loadFavouriteApps()
        } else {
            visibleNoConnect()
            btnConnect?.setOnClickListener {
                showFragmentDevice()
            }
        }
    }

    private fun visibleNoConnect() {
        linearNoConnect!!.visibility = View.VISIBLE
        linearChannel!!.visibility = View.GONE
        progressBar!!.visibility = View.GONE
        linerFavourite!!.visibility = View.GONE
    }

    private fun visibleConnect() {
        linearNoConnect!!.visibility = View.GONE
        linearChannel!!.visibility = View.VISIBLE
        progressBar!!.visibility = View.VISIBLE
        linerFavourite!!.visibility = View.VISIBLE
    }

    private fun initializeViews(view: View) {
        channelRecyclerView = view.findViewById(R.id.recycler_channel)
        favouriteRecyclerView = view.findViewById(R.id.recycler_favourite)
        progressBar = view.findViewById(R.id.progress_bar)
        nameDV = view.findViewById(R.id.nameDV)
        castButton = view.findViewById(R.id.img_cast)
        linerFavourite = view.findViewById(R.id.linear_favourite)
        tvChannel = view.findViewById(R.id.tv_Channel)
        tvFavourite = view.findViewById(R.id.tv_favourite)
        btnConnect = view.findViewById(R.id.btn_connect)
        linearNoConnect = view.findViewById(R.id.linear_no_connect)
        linearChannel = view.findViewById(R.id.linear_channel)
    }

    private fun setDisconnect(singleton: Singleton) {
        castButton?.setOnClickListener {
            if (singleton.isConnectedCustom) {
                showAlertDialogDisconnected(nameDV)
            } else {
                showFragmentDevice()
            }
        }
    }

    private fun setupRecyclerViews() {
        favouriteRecyclerView!!.setLayoutManager(
            LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        val gridLayoutManager = GridLayoutManager(context, 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when {
                    appAdapter != null && appAdapter!!.getItemViewType(position) == appAdapter!!.VIEW_TYPE_APP -> 1
                    //appInfoSamsungAdapter != null && appInfoSamsungAdapter!!.getItemViewType(position) == appInfoSamsungAdapter!!.VIEW_TYPE_APP -> 1
                    appLGAdapter != null && appLGAdapter!!.getItemViewType(position) == appLGAdapter!!.VIEW_TYPE_APP -> 1
//                    appVizioAdapter != null && appVizioAdapter!!.getItemViewType(position) == appVizioAdapter!!.VIEW_TYPE_APP -> 1
//                    appFireTVAdapter != null && appFireTVAdapter!!.getItemViewType(position) == appFireTVAdapter!!.VIEW_TYPE_APP -> 1
                    appAdapter != null && appAdapter!!.getItemViewType(position) == appAdapter!!.VIEW_TYPE_TEMPLATE -> 3
                    // appInfoSamsungAdapter != null && appInfoSamsungAdapter!!.getItemViewType(position) == appInfoSamsungAdapter!!.VIEW_TYPE_TEMPLATE -> 3
                    appLGAdapter != null && appLGAdapter!!.getItemViewType(position) == appLGAdapter!!.VIEW_TYPE_TEMPLATE -> 3
//                    appVizioAdapter != null && appVizioAdapter!!.getItemViewType(position) == appVizioAdapter!!.VIEW_TYPE_TEMPLATE -> 3
//                    appFireTVAdapter != null && appFireTVAdapter!!.getItemViewType(position) == appFireTVAdapter!!.VIEW_TYPE_TEMPLATE -> 3
                    else -> 1
                }
            }
        }
        channelRecyclerView!!.layoutManager = gridLayoutManager
    }

    private fun setTextName() {
        val name = Singleton.getInstance().getNameDevice()
        if (name.isNotEmpty()) {
            nameDV!!.text = name
        }
    }

    private fun loadApps(singleton: Singleton) {
        progressBar!!.visibility = View.VISIBLE
        singleton.getImage(object : GetImageCallback {
            override fun onSuccess(appList: List<AppInfo>, ipAddress: String, name: String) {
                appAdapter = AppAdapter(context!!, appList, ipAddress, this@AppsFragment)
                channelRecyclerView!!.setAdapter(appAdapter)
                progressBar!!.visibility = View.GONE
            }

            override fun onError(error: ServiceCommandError, ipAddress: String, name: String) {
                Log.e("####", "onError: ", error)
                progressBar!!.visibility = View.GONE
            }
        })
    }

    private fun loadAppsLG(singleton: Singleton) {
        progressBar!!.visibility = View.VISIBLE
        val sharedPreferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedData = sharedPreferences?.getString("appList", null)

        if (savedData != null) {
            val jsonArray = JSONArray(savedData)
            val ipAddress = sharedPreferences.getString("ipAddress", "") ?: ""
            appLGAdapter = AppLGAdapter(requireContext(), jsonArray, ipAddress, this)
            channelRecyclerView!!.adapter = appLGAdapter
            progressBar!!.visibility = View.GONE
        } else {
            progressBar!!.visibility = View.VISIBLE
            singleton.getImageLG(object : Singleton.GetImageCallbackLG {
                override fun onSuccess(objectLG: JSONArray?, ipAddress: String, name: String) {
                    appLGAdapter = objectLG?.let { AppLGAdapter(requireContext(), it, ipAddress, this@AppsFragment) }
                    channelRecyclerView!!.adapter = appLGAdapter

                    val editor = sharedPreferences?.edit()
                    editor?.putString("appList", objectLG?.toString())
                    editor?.putString("ipAddress", ipAddress)
                    editor?.apply()

                    progressBar!!.visibility = View.GONE
                }

                override fun onError(error: ServiceCommandError, ipAddress: String, name: String) {
                    Log.e("####", "onError: ", error)
                    progressBar!!.visibility = View.GONE
                }
            })
        }
    }

    private fun loadFavouriteApps() {
        val favourites: MutableList<Favourite> = getFavourites(ipAddress)
        if (favourites.isNotEmpty()) {
            favouriteAppAdapter = FavouriteAppAdapter(requireContext(), favourites, ipAddress)
            favouriteRecyclerView?.adapter = favouriteAppAdapter
            linerFavourite?.visibility = View.VISIBLE
        } else {
            linerFavourite?.visibility = View.GONE
        }
    }

    private fun refreshData(singleton: Singleton) {
        if (singleton.isConnectedCustom) {
            checkConnect(singleton)
            loadFavouriteApps()
        } else {
            showFragmentDevice()
        }

    }

    private fun showFragmentDevice() {
        val deviceFragment = DeviceFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left // exit
            )
            .replace(R.id.fragment_container, deviceFragment, "findThisFragment")
            .addToBackStack("findThisFragment")
            .commit()
    }

    private fun showAlertDialogDisconnected(txtDevice: TextView?): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(
            activity
        )
        val view: View = getLayoutInflater().inflate(R.layout.item_cast, null)
        alertDialogBuilder.setView(view)
        val textName = view.findViewById<TextView>(R.id.textNameDevice)
        val btnDisconnect = view.findViewById<Button>(R.id.btnDisconnect)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        textName.text = txtDevice!!.text
        val alertDialog = alertDialogBuilder.create()
        btnDisconnect.setOnClickListener { _: View? ->
            val singleton =
                Singleton.getInstance()
            singleton.setConnected(false)
            singleton.disconnectDevice()
            txtDevice.text = " "
            alertDialog.dismiss()
            visibleNoConnect()
        }
        btnCancel.setOnClickListener { _: View? -> alertDialog.dismiss() }
        alertDialog.show()
        return alertDialog
    }

    private fun getFavourites(ipAddress: String): MutableList<Favourite> {
        val deviceDao = AppDatabase.getDatabase(requireContext()).deviceDao()
        return deviceDao.getFavouritesByIp(ipAddress)?.filterNotNull()?.toMutableList() ?: mutableListOf()
    }


    private fun saveDataToSharedPreferences(appInfoList: MutableList<LGAppInfo>) {
        val gson = Gson()
        val jsonString = gson.toJson(appInfoList)

        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("appInfoList", jsonString)
        editor.apply()
    }

    private fun loadDataFromSharedPreferences(): MutableList<LGAppInfo>? {
        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("appInfoList", null)

        if (jsonString != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<LGAppInfo>>() {}.type
            return gson.fromJson(jsonString, type)

        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        clearSharedPreferencesData()
        clearloadData()
    }


    private fun clearSharedPreferencesData() {
        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("appInfoList")
        editor.apply()
    }

    private fun clearloadData() {
        val sharedPreferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        editor?.clear()
        editor?.apply()
    }

    override fun onItemClicked() {
        val singleton = Singleton.getInstance()
        refreshData(singleton)
    }

}