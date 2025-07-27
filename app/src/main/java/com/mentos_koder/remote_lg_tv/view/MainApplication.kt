package com.mentos_koder.remote_lg_tv.view

import android.app.Application
import android.content.Context
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.service.DIALService

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DIALService.registerApp("Levak")
        DiscoveryManager.init(applicationContext)
        appContext = applicationContext
    }

    companion object {
        private lateinit var instance: MainApplication
        private lateinit var appContext: Context

        @JvmStatic
        fun getAppContext(): Context {
            return appContext
        }
    }
}

