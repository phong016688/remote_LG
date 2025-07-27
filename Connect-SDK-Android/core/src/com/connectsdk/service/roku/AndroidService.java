package com.connectsdk.service.roku;

import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

public class AndroidService extends DeviceService {
    public static final String ID = "AndroidTV";

    public AndroidService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "_androidtvremote._tcp.local.");
    }

    public void connect() {
        this.connected = true;
        reportConnected(true);
    }

    public void disconnect() {
        this.connected = false;
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isConnected() {
        return this.connected;
    }
}
