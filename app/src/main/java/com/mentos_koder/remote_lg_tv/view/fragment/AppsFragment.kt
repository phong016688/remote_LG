package com.mentos_koder.remote_lg_tv.view.fragment

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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.connectsdk.core.AppInfo
import com.connectsdk.service.command.ServiceCommandError
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.AppAdapter
import com.mentos_koder.remote_lg_tv.adapter.AppLGAdapter
import com.mentos_koder.remote_lg_tv.adapter.FavouriteAppAdapter
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.event.OnItemClickListener
import com.mentos_koder.remote_lg_tv.model.Favorite
import com.mentos_koder.remote_lg_tv.util.Constants
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.Singleton.GetImageCallback
import com.mentos_koder.remote_lg_tv.util.showDialogDisconnect
import com.mentos_koder.remote_lg_tv.view.MainActivity
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
                (activity as? MainActivity)?.showFragmentDevice()
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
                context?.showDialogDisconnect {
                    singleton.setConnected(false)
                    singleton.disconnectDevice()
                    visibleNoConnect()
                }
            } else {
                (activity as? MainActivity)?.showFragmentDevice()
            }
        }
    }

    private fun setupRecyclerViews() {
        favouriteRecyclerView!!.setLayoutManager(
            LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false
            )
        )
        val gridLayoutManager = GridLayoutManager(context, 3)
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
                progressBar!!.visibility = View.GONE
            }
        })
    }

    private fun loadAppsLG(singleton: Singleton) {
        progressBar!!.visibility = View.VISIBLE
        val sharedPreferences =
            context?.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val savedData = sharedPreferences?.getString(Constants.APP_LIST, null)

        if (savedData != null) {
            val jsonArray = JSONArray(savedData)
            val ipAddress = sharedPreferences.getString(Constants.IP_ADDRESS, "") ?: ""
            appLGAdapter = AppLGAdapter(requireContext(), jsonArray, ipAddress, this)
            channelRecyclerView!!.adapter = appLGAdapter
            progressBar!!.visibility = View.GONE
        } else {
            progressBar!!.visibility = View.VISIBLE
            singleton.getImageLG(object : Singleton.GetImageCallbackLG {
                override fun onSuccess(objectLG: JSONArray?, ipAddress: String, name: String) {
                    appLGAdapter = objectLG?.let {
                        AppLGAdapter(requireContext(), it, ipAddress, this@AppsFragment)
                    }
                    channelRecyclerView!!.adapter = appLGAdapter

                    sharedPreferences?.edit {
                        this.putString(Constants.APP_LIST, objectLG?.toString())
                        this.putString(Constants.IP_ADDRESS, ipAddress)
                    }

                    progressBar!!.visibility = View.GONE
                }

                override fun onError(error: ServiceCommandError, ipAddress: String, name: String) {
                    progressBar!!.visibility = View.GONE
                }
            })
        }
    }

    private fun loadFavouriteApps() {
        val favorites: MutableList<Favorite> = getFavourites(ipAddress)
        if (favorites.isNotEmpty()) {
            favouriteAppAdapter = FavouriteAppAdapter(requireContext(), favorites, ipAddress)
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
            (activity as? MainActivity)?.showFragmentDevice()
        }

    }

    private fun getFavourites(ipAddress: String): MutableList<Favorite> {
        val deviceDao = AppDatabase.getDatabase(requireContext()).deviceDao()
        return deviceDao.getFavouritesByIp(ipAddress)?.filterNotNull()?.toMutableList()
            ?: mutableListOf()
    }


    override fun onDestroy() {
        super.onDestroy()
        clearSharedPreferencesData()
        clearLoadData()
    }


    private fun clearSharedPreferencesData() {
        val sharedPreferences =
            requireContext().getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            remove(Constants.APP_INFO_LIST)
        }
    }

    private fun clearLoadData() {
        val sharedPreferences =
            context?.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences?.edit {
            this.clear()
        }
    }

    override fun onItemClicked() {
        val singleton = Singleton.getInstance()
        refreshData(singleton)
    }

}