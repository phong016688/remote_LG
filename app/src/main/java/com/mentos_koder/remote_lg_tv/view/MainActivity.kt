package com.mentos_koder.remote_lg_tv.view

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.device.DevicePicker
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.provider.CastDiscoveryProvider
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider
import com.connectsdk.service.AirPlayService
import com.connectsdk.service.CastService
import com.connectsdk.service.DIALService
import com.connectsdk.service.DLNAService
import com.connectsdk.service.DeviceService
import com.connectsdk.service.DeviceService.PairingType
import com.connectsdk.service.FireTVService
import com.connectsdk.service.WebOSTVService
import com.connectsdk.service.command.ServiceCommandError
import com.connectsdk.service.roku.AndroidService
import com.connectsdk.service.roku.NewAndroidService
import com.connectsdk.service.roku.VizioService
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.NotificationWorker
import com.mentos_koder.remote_lg_tv.util.OnBack
import com.mentos_koder.remote_lg_tv.util.PermissionUtils
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.fragment.AppsFragment
import com.mentos_koder.remote_lg_tv.view.fragment.CastFragment
import com.mentos_koder.remote_lg_tv.view.fragment.homeFragment
import com.mentos_koder.remote_lg_tv.view.fragment.settingsFragment
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.content.edit
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity(), OnBack {
    private var deviceListFragment: Fragment? = null
    private var castFragment: Fragment? = null
    private var settingsFragment: Fragment? = null
    private var homeFragment: Fragment? = null
    private var bottomNavView: BottomNavigationView? = null
    private var connectableDevice: ConnectableDevice? = null
    private var alertDialog: AlertDialog? = null
    private var pairingAlertDialog: AlertDialog? = null
    private var pairingCodeDialog: AlertDialog? = null
    private var devicePicker: DevicePicker? = null
    private val PREF_NAME = "MyPrefs"
    private lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val sharedPreferences by lazy { getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

    companion object {
        val listImageFolder: MutableList<String> = mutableListOf()
    }


    private val deviceListener: ConnectableDeviceListener = object : ConnectableDeviceListener {
        override fun onPairingRequired(
            device: ConnectableDevice,
            service: DeviceService,
            pairingType: PairingType,
        ) {
            Log.d("2ndScreenAPP", "Connected to " + connectableDevice!!.ipAddress)
            when (pairingType) {
                PairingType.FIRST_SCREEN -> {
                    Log.d("2ndScreenAPP", "First Screen")
                    pairingAlertDialog!!.show()
                }

                PairingType.PIN_CODE, PairingType.MIXED -> {
                    Log.d("2ndScreenAPP", "Pin Code")
                    pairingCodeDialog!!.show()
                }

                PairingType.NONE -> {}
                else -> {}
            }
        }

        override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
            Log.d("2ndScreenAPP", "onConnectFailed")
            onConnectionFailed(connectableDevice)
        }

        override fun onDeviceReady(device: ConnectableDevice) {
            Log.d("2ndScreenAPP", "onPairingSuccess")
            if (pairingAlertDialog!!.isShowing) {
                pairingAlertDialog!!.dismiss()
            }
            if (pairingCodeDialog!!.isShowing) {
                pairingCodeDialog!!.dismiss()
            }
            onRegisterSuccess()
        }

        override fun onDeviceDisconnected(device: ConnectableDevice) {
            Log.d("2ndScreenAPP", "Device Disconnected")
            onConnectionEnded()
        }

        override fun onCapabilityUpdated(
            device: ConnectableDevice,
            added: List<String>,
            removed: List<String>,
        ) {
            Log.d("2ndScreenAPP", "onCapabilityUpdated")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestFormGDPR()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        saveFirstTime()
        showRating()
        initializeFragments()
        createNotificationChannel()
        //scheduleNotification(this)
        setupBottomNavigationView()
        displayDefaultFragment()
        setupDevicePicker()
        initializeDiscoveryManager()
        Singleton.getInstance().setActivity(this)
        logFirebaseEvent()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                if (currentFragment !is homeFragment) {
                    bottomNavView?.selectedItemId = R.id.menu_home
                } else {
                    finish()
                }
            }
        })
        PermissionUtils.checkAndRequestNotificationPermission(this)
    }

    override fun onStop() {
        super.onStop()
        WorkManager.getInstance(this).cancelAllWorkByTag("NotUsedApp")
        scheduleWork("LG Remote", "It's been a day since you used the app.",1)
        scheduleWork("LG Remote", "It's been a week since you used the app.",7)
    }

    private fun logFirebaseEvent() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "connected")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Done connected")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
                AirPlayService::class.java,
                ZeroconfDiscoveryProvider::class.java
            )
//            // webOS SSAP (Simple Service Access Protocol)
            discoveryManager.registerDeviceService(
                WebOSTVService::class.java,
                SSDPDiscoveryProvider::class.java
            )
//            // DLNA
            discoveryManager.registerDeviceService(
                DLNAService::class.java,
                SSDPDiscoveryProvider::class.java
            )
            // DIAL
            discoveryManager.registerDeviceService(
                DIALService::class.java,
                SSDPDiscoveryProvider::class.java
            )
//            //AndroidService
            discoveryManager.registerDeviceService(
                AndroidService::class.java,
                ZeroconfDiscoveryProvider::class.java
            )
//            //NewAndroidService
            discoveryManager.registerDeviceService(
                NewAndroidService::class.java,
                ZeroconfDiscoveryProvider::class.java
            )
//            //VizioService
            discoveryManager.registerDeviceService(
                VizioService::class.java,
                ZeroconfDiscoveryProvider::class.java
            )
//            //FireTVService
            discoveryManager.registerDeviceService(
                FireTVService::class.java,
                FireTVDiscoveryProvider::class.java
            )
            //CastService
            discoveryManager.registerDeviceService(
                CastService::class.java,
                CastDiscoveryProvider::class.java
            )

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        DiscoveryManager.getInstance().start()
    }

    override fun onResume() {
        super.onResume()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun toggleConnection() {
        if (!this.isFinishing) {
            if (connectableDevice != null) {
                if (connectableDevice!!.isConnected()) {
                    connectableDevice!!.disconnect()
                }
                connectableDevice!!.removeListener(deviceListener)
                connectableDevice = null
            } else {
                alertDialog!!.show()
            }
        }
    }

    private fun setupDevicePicker() {
        devicePicker = DevicePicker(this)
        alertDialog = devicePicker!!.getPickerDialog(
            "Device List"
        ) { arg0: AdapterView<*>, _: View?, arg2: Int, _: Long ->
            connectableDevice = arg0.getItemAtPosition(arg2) as ConnectableDevice
            connectableDevice!!.addListener(deviceListener)
            connectableDevice!!.setPairingType(null)
            connectableDevice!!.connect()
            devicePicker!!.pickDevice(connectableDevice)
        }
        pairingAlertDialog = AlertDialog.Builder(this)
            .setTitle("Pairing with TV")
            .setMessage("Please confirm the connection on your TV")
            .setPositiveButton("Okay", null)
            .setNegativeButton("Cancel") { _: DialogInterface?, _: Int ->
                devicePicker!!.cancelPicker()
                toggleConnection()
            }
            .create()
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        pairingCodeDialog = AlertDialog.Builder(this)
            .setTitle("Enter Pairing Code on TV")
            .setView(input)
            .setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                if (connectableDevice != null) {
                    val value = input.text.toString().trim { it <= ' ' }
                    connectableDevice!!.sendPairingKey(value)
                    imm.hideSoftInputFromWindow(input.windowToken, 0)
                }
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                devicePicker!!.cancelPicker()
                toggleConnection()
                imm.hideSoftInputFromWindow(input.windowToken, 0)
            }
            .create()
    }

    private fun onRegisterSuccess() {
        Log.d("2ndScreenAPP", "successful register")
        logFirebaseEvent()
//        BaseFragment baseFragment = new BaseFragment();
//        baseFragment.setTv(device);
    }

    private fun onConnectionFailed(device: ConnectableDevice?) {
        if (device != null) Log.d("2ndScreenAPP", "Failed to connect to " + device.ipAddress)
        if (connectableDevice != null) {
            connectableDevice!!.removeListener(deviceListener)
            connectableDevice!!.disconnect()
            connectableDevice = null
        }
    }

    private fun onConnectionEnded() {
        if (pairingAlertDialog!!.isShowing) {
            pairingAlertDialog!!.dismiss()
        }
        if (pairingCodeDialog!!.isShowing) {
            pairingCodeDialog!!.dismiss()
        }
        if (!connectableDevice!!.isConnecting) {
            connectableDevice!!.removeListener(deviceListener)
            connectableDevice = null
        }
    }

    private fun initializeFragments() {
        castFragment = CastFragment()
        deviceListFragment = AppsFragment()
        settingsFragment = settingsFragment()
        homeFragment = homeFragment()
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
//        val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
        val fragment: Fragment = homeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setNavigationItemSelected(menuItemId: Int) {
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavView.selectedItemId = menuItemId
    }


//    private fun scheduleNotification(context: Context) {
//        val sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
//        val firstLaunchDateTime = sharedPreferences.getString(PREF_FIRST_LAUNCH_DATETIME, null)
//        if (firstLaunchDateTime != null) {
//            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            try {
//                val firstLaunchDate = sdf.parse(firstLaunchDateTime)
//                val calendar = Calendar.getInstance()
//                if (firstLaunchDate != null) {
//                    calendar.time = firstLaunchDate
//                }
//                val triggerAtMillis = calendar.timeInMillis
//                val notificationIntent = Intent(context, NotificationReceiver::class.java)
//                val pendingIntent = PendingIntent.getBroadcast(
//                    context,
//                    NOTIFICATION_ID,
//                    notificationIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT
//                )
//                val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
//                alarmManager.setRepeating(
//                    AlarmManager.RTC_WAKEUP,
//                    triggerAtMillis,
//                    AlarmManager.INTERVAL_DAY,
//                    pendingIntent
//                )
//            } catch (e: ParseException) {
//                e.printStackTrace()
//            }
//        }
//    }

    fun requestFormGDPR() {
        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("858607A2E3F8583F26AA44E1F4C3C827")
            .build()

        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            .build()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@MainActivity,
                    ConsentForm.OnConsentFormDismissedListener { loadAndShowError ->
                        // Consent gathering failed.
                        if (loadAndShowError != null) {
                            Log.w(
                                "TAG#####", String.format(
                                    "%s: %s",
                                    loadAndShowError.errorCode,
                                    loadAndShowError.message
                                )
                            )
                        }

                        // Consent has been gathered.
                        if (consentInformation.canRequestAds()) {
                            initializeMobileAdsSdk()
                        }
                    }
                )
            },
            { requestConsentError ->
                Log.w(
                    "TAG#####", String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
            })
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }


    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
    }

    private fun saveFirstTime() {
        val firstLaunchMillis = sharedPreferences.getLong("firstTime", 0L)

        if (firstLaunchMillis == 0L) {
            sharedPreferences.edit()
                .putLong("firstTime", System.currentTimeMillis())
                .apply()
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
            val firstTimeLocalDate = firstTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val todayLocalDate = LocalDate.now()
            val daysDifference = ChronoUnit.DAYS.between(firstTimeLocalDate, todayLocalDate)
            val reviewRequested3 = sharedPreferences.getBoolean("reviewRequested3", false)
            val reviewRequested10 = sharedPreferences.getBoolean("reviewRequested10", false)
            val reviewRequested45 = sharedPreferences.getBoolean("reviewRequested45", false)
            val reviewRequested365 = sharedPreferences.getBoolean("reviewRequested365", false)

            if (daysDifference in 2L..8L && !reviewRequested3) {
                sharedPreferences.edit() { putBoolean("reviewRequested3", true) }
            }else if(daysDifference in 9L..43L && !reviewRequested10){
                sharedPreferences.edit() { putBoolean("reviewRequested10", true)}
            }else if(daysDifference in 44L..363L && !reviewRequested45){
                sharedPreferences.edit() { putBoolean("reviewRequested45", true)}
            } else if(daysDifference >= 364 && !reviewRequested365){
                sharedPreferences.edit() { putBoolean("reviewRequested365", true) }
            }
        }
    }

    private fun scheduleWork(title: String, message: String, delaySeconds: Long) {
        val data = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.DAYS)
            .addTag("NotUsedApp")
            .setInputData(data)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun createNotificationChannel() {
        val name = "Reminder Channel"
        val descriptionText = "Channel for reminder notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("MyChannel", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun back(position: Int) {
        displayDefaultFragment()
        bottomNavView?.selectedItemId = R.id.menu_home
        Toast.makeText(this, "Connection successful", Toast.LENGTH_SHORT).show()
    }

}
