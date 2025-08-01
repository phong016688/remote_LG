package com.connectsdk;

import android.util.Log;

import java.util.HashMap;

public class DefaultPlatform {



    public DefaultPlatform() {
    }

    public static HashMap<String, String> getDeviceServiceMap() {
        Log.d("getDeviceServiceMap", "getDeviceServiceMap: ");
        HashMap<String, String> devicesList = new HashMap<String, String>();
        devicesList.put("com.connectsdk.service.WebOSTVService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.connectsdk.service.NetcastTVService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.connectsdk.service.DLNAService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.connectsdk.service.DIALService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.connectsdk.service.RokuService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.connectsdk.service.CastService", "com.connectsdk.discovery.provider.CastDiscoveryProvider");
        devicesList.put("com.connectsdk.service.AirPlayService", "com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider");
        devicesList.put("com.connectsdk.service.FireTVService", "com.connectsdk.discovery.provider.FireTVDiscoveryProvider");
        return devicesList;
    }

}
