package com.mentos_koder.remote_lg_tv.view

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.provider.CastDiscoveryProvider
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider
import com.connectsdk.service.AirPlayService
import com.connectsdk.service.CastService
import com.connectsdk.service.DIALService
import com.connectsdk.service.DLNAService
import com.connectsdk.service.FireTVService
import com.connectsdk.service.WebOSTVService
import com.connectsdk.service.roku.AndroidService
import com.connectsdk.service.roku.NewAndroidService
import com.connectsdk.service.roku.VizioService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.Constants
import com.mentos_koder.remote_lg_tv.util.NotificationWorker
import com.mentos_koder.remote_lg_tv.util.OnBack
import com.mentos_koder.remote_lg_tv.util.PermissionUtils
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.fragment.AppsFragment
import com.mentos_koder.remote_lg_tv.view.fragment.CastFragment
import com.mentos_koder.remote_lg_tv.view.fragment.DeviceFragment
import com.mentos_koder.remote_lg_tv.view.fragment.HomeFragment
import com.mentos_koder.remote_lg_tv.view.fragment.SettingsFragment
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), OnBack {
    private var deviceListFragment: Fragment? = null
    private var castFragment: Fragment? = null
    private var settingsFragment: Fragment? = null
    private var homeFragment: Fragment? = null
    private var bottomNavView: BottomNavigationView? = null
    private var connectableDevice: ConnectableDevice? = null
    private var alertDialog: AlertDialog? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val sharedPreferences by lazy {
        getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        saveFirstTime()
        showRating()
        initializeFragments()
        createNotificationChannel()
        setupBottomNavigationView()
        displayDefaultFragment()
        initializeDiscoveryManager()
        Singleton.getInstance().setActivity(this)
        logFirebaseEvent()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment =
                    supportFragmentManager.findFragmentById(R.id.fragment_container)
                when (currentFragment) {
                    is HomeFragment -> {
                        finish()
                    }

                    is CastFragment, is AppsFragment, is SettingsFragment -> {
                        bottomNavView?.selectedItemId = R.id.menu_home
                    }

                    else -> {
                        supportFragmentManager.popBackStack()
                    }
                }
            }
        })
        PermissionUtils.checkAndRequestNotificationPermission(this)
    }

    override fun onStop() {
        super.onStop()
        WorkManager.getInstance(this).cancelAllWorkByTag("NotUsedApp")
        scheduleWork("LG Remote", "It's been a day since you used the app.", 1)
        scheduleWork("LG Remote", "It's been a week since you used the app.", 7)
    }

    private fun logFirebaseEvent() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "connected")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Done connected")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun showFragmentDevice() {
        val deviceFrag = DeviceFragment()
        supportFragmentManager.beginTransaction().setCustomAnimations(
            R.anim.slide_in_right,  // enter
            R.anim.slide_out_left // exit
        ).replace(R.id.fragment_container, deviceFrag, "findThisFragment")
            .addToBackStack("findThisFragment").commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    private fun initializeDiscoveryManager() {
        val discoveryManager = DiscoveryManager.getInstance()
        discoveryManager.registerDefaultDeviceTypes()
        discoveryManager.pairingLevel = DiscoveryManager.PairingLevel.ON
        discoveryManager.setServiceIntegration(true)
        try {
            // AirPlay
            discoveryManager.registerDeviceService(
                AirPlayService::class.java, ZeroconfDiscoveryProvider::class.java
            )
//            // webOS SSAP (Simple Service Access Protocol)
            discoveryManager.registerDeviceService(
                WebOSTVService::class.java, SSDPDiscoveryProvider::class.java
            )
//            // DLNA
            discoveryManager.registerDeviceService(
                DLNAService::class.java, SSDPDiscoveryProvider::class.java
            )
            // DIAL
            discoveryManager.registerDeviceService(
                DIALService::class.java, SSDPDiscoveryProvider::class.java
            )
//            //AndroidService
            discoveryManager.registerDeviceService(
                AndroidService::class.java, ZeroconfDiscoveryProvider::class.java
            )
//            //NewAndroidService
            discoveryManager.registerDeviceService(
                NewAndroidService::class.java, ZeroconfDiscoveryProvider::class.java
            )
//            //VizioService
            discoveryManager.registerDeviceService(
                VizioService::class.java, ZeroconfDiscoveryProvider::class.java
            )
//            //FireTVService
            discoveryManager.registerDeviceService(
                FireTVService::class.java, FireTVDiscoveryProvider::class.java
            )
            //CastService
            discoveryManager.registerDeviceService(
                CastService::class.java, CastDiscoveryProvider::class.java
            )

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        DiscoveryManager.getInstance().start()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (alertDialog != null) {
            alertDialog!!.dismiss()
        }
        if (connectableDevice != null) {
            connectableDevice!!.disconnect()
        }
    }

    private fun initializeFragments() {
        castFragment = CastFragment()
        deviceListFragment = AppsFragment()
        settingsFragment = SettingsFragment()
        homeFragment = HomeFragment()
    }

    private fun setupBottomNavigationView() {
        bottomNavView = findViewById(R.id.bottom_navigation)
        bottomNavView?.setOnNavigationItemSelectedListener { item ->
            val itemId = item.itemId
            when (itemId) {
                R.id.menu_home -> {
                    displayDefaultFragment()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.menu_list -> {
                    deviceListFragment?.let { replaceFragment(it) }
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.menu_cast -> {
                    castFragment?.let { replaceFragment(it) }
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.menu_setting -> {
                    settingsFragment?.let { replaceFragment(it) }
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
    }

    private fun displayDefaultFragment() {
        val fragment: Fragment = HomeFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun saveFirstTime() {
        val firstLaunchMillis = sharedPreferences.getLong("firstTime", 0L)

        if (firstLaunchMillis == 0L) {
            sharedPreferences.edit { putLong("firstTime", System.currentTimeMillis()) }
        }
    }

    private fun getFirstTime(): Date? {
        val firstLaunchMillis = sharedPreferences.getLong("firstTime", 0L)
        return if (firstLaunchMillis != 0L) {
            Date(firstLaunchMillis)
        } else {
            null
        }
    }

    private fun showRating() {
        val firstTime = getFirstTime()
        if (firstTime != null) {
            val firstTimeLocalDate =
                firstTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val todayLocalDate = LocalDate.now()
            val daysDifference = ChronoUnit.DAYS.between(firstTimeLocalDate, todayLocalDate)
            val reviewRequested3 = sharedPreferences.getBoolean("reviewRequested3", false)
            val reviewRequested10 = sharedPreferences.getBoolean("reviewRequested10", false)
            val reviewRequested45 = sharedPreferences.getBoolean("reviewRequested45", false)
            val reviewRequested365 = sharedPreferences.getBoolean("reviewRequested365", false)

            if (daysDifference in 2L..8L && !reviewRequested3) {
                sharedPreferences.edit { putBoolean("reviewRequested3", true) }
            } else if (daysDifference in 9L..43L && !reviewRequested10) {
                sharedPreferences.edit { putBoolean("reviewRequested10", true) }
            } else if (daysDifference in 44L..363L && !reviewRequested45) {
                sharedPreferences.edit { putBoolean("reviewRequested45", true) }
            } else if (daysDifference >= 364 && !reviewRequested365) {
                sharedPreferences.edit { putBoolean("reviewRequested365", true) }
            }
        }
    }

    private fun scheduleWork(title: String, message: String, delaySeconds: Long) {
        val data = Data.Builder().putString("title", title).putString("message", message).build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().setInitialDelay(
            delaySeconds, TimeUnit.DAYS
        ).addTag("NotUsedApp").setInputData(data).build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.reminder_channel)
        val descriptionText = getString(R.string.channel_for_reminder_notifications)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("MyChannel", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun back(position: Int) {
        displayDefaultFragment()
        bottomNavView?.selectedItemId = R.id.menu_home
        Toast.makeText(this, getString(R.string.connection_successful), Toast.LENGTH_SHORT).show()
    }
}
