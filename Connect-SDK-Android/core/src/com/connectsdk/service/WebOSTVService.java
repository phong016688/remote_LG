/*
 * WebOSTVService
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.ExternalInputInfo;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MouseControl;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.PowerControl;
import com.connectsdk.service.capability.RemoteCameraControl;
import com.connectsdk.service.capability.ScreenMirroringControl;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.config.WebOSTVServiceConfig;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.connectsdk.service.sessions.WebAppSession;
import com.connectsdk.service.sessions.WebAppSession.WebAppPinStatusListener;
import com.connectsdk.service.sessions.WebOSWebAppSession;
import com.connectsdk.service.webos.WebOSTVDeviceService;
import com.connectsdk.service.webos.WebOSTVKeyboardInput;
import com.connectsdk.service.webos.WebOSTVMouseSocketConnection;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener;
import com.connectsdk.service.webos.lgcast.common.utils.XmlUtil;
import com.connectsdk.service.webos.lgcast.remotecamera.api.RemoteCameraApi;
import com.connectsdk.service.webos.lgcast.screenmirroring.api.ScreenMirroringApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@SuppressLint("DefaultLocale")
public class WebOSTVService extends WebOSTVDeviceService implements Launcher, MediaPlayer, PlaylistControl, VolumeControl, TVControl, ToastControl, ExternalInputControl, MouseControl, KeyControl, TextInputControl, WebAppLauncher, ScreenMirroringControl, RemoteCameraControl {

    public static final String ID = "webOS TV";
    private static final String MEDIA_PLAYER_ID = "MediaPlayer";

    public interface SecureAccessTestListener extends ResponseListener<Boolean> { }

    public interface ACRAuthTokenListener extends ResponseListener<String> { }

    public interface LaunchPointsListener extends ResponseListener<JSONArray> { }

    static String FOREGROUND_APP = "ssap://com.webos.applicationManager/getForegroundAppInfo";
    static String APP_STATUS = "ssap://com.webos.service.appstatus/getAppStatus";
    static String APP_STATE = "ssap://system.launcher/getAppState";
    static String VOLUME = "ssap://audio/getVolume";
    static String MUTE = "ssap://audio/getMute";
    static String VOLUME_STATUS = "ssap://audio/getStatus";
    static String CHANNEL_LIST = "ssap://tv/getChannelList";
    static String CHANNEL = "ssap://tv/getCurrentChannel";
    static String PROGRAM = "ssap://tv/getChannelProgramInfo";

    static final String CLOSE_APP_URI = "ssap://system.launcher/close";
    static final String CLOSE_MEDIA_URI = "ssap://media.viewer/close";
    static final String CLOSE_WEBAPP_URI = "ssap://webapp/closeWebApp";

    WebOSTVMouseSocketConnection mouseSocket;

    WebOSTVKeyboardInput keyboardInput;

    ConcurrentHashMap<String, String> mAppToAppIdMappings;

    ConcurrentHashMap<String, WebOSWebAppSession> mWebAppSessions;

    WebOSTVServiceSocketClient socket;

    List<String> permissions;

    public WebOSTVService(ServiceDescription serviceDescription, ServiceConfig serviceConf) {
        super(serviceDescription, serviceConf);
        serviceConfig = new WebOSTVServiceConfig(serviceConf.toJSONObject());
        pairingType = PairingType.FIRST_SCREEN;
        mAppToAppIdMappings = new ConcurrentHashMap<String, String>();
        mWebAppSessions = new ConcurrentHashMap<String, WebOSWebAppSession>();
    }

    public WebOSTVServiceConfig getWebOSTVServiceConfig() {
        return (WebOSTVServiceConfig) serviceConfig;
    }

    @Override
    public void setPairingType(PairingType pairingType) {
        this.pairingType = pairingType;
    }

    @Override
    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        }
        else if (clazz.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        }
        else if (clazz.equals(Launcher.class)) {
            return getLauncherCapabilityLevel();
        }
        else if (clazz.equals(TVControl.class)) {
            return getTVControlCapabilityLevel();
        }
        else if (clazz.equals(VolumeControl.class)) {
            return getVolumeControlCapabilityLevel();
        }
        else if (clazz.equals(ExternalInputControl.class)) {
            return getExternalInputControlPriorityLevel();
        }
        else if (clazz.equals(MouseControl.class)) {
            return getMouseControlCapabilityLevel();
        }
        else if (clazz.equals(TextInputControl.class)) {
            return getTextInputControlCapabilityLevel();
        }
        else if (clazz.equals(PowerControl.class)) {
            return getPowerControlCapabilityLevel();
        }
        else if (clazz.equals(KeyControl.class)) {
            return getKeyControlCapabilityLevel();
        }
        else if (clazz.equals(ToastControl.class)) {
            return getToastControlCapabilityLevel();
        }
        else if (clazz.equals(WebAppLauncher.class)) {
            return getWebAppLauncherCapabilityLevel();
        }
        else if (clazz.equals(PlaylistControl.class)) {
            return getPlaylistControlCapabilityLevel();
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);

        if (this.serviceDescription.getVersion() == null && this.serviceDescription.getResponseHeaders() != null)
        {
            String serverInfo = serviceDescription.getResponseHeaders().get("Server").get(0);
            String systemOS = serverInfo.split(" ")[0];
            String[] versionComponents = systemOS.split("/");
            String systemVersion = versionComponents[versionComponents.length - 1];

            this.serviceDescription.setVersion(systemVersion);

            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
                InputSource source = new InputSource();
                source.setCharacterStream(new StringReader(serviceDescription.getLocationXML()));
                Document doc = docBuilder.parse(source);
                NodeList nodes = doc.getElementsByTagName("serviceId");
                Node node = nodes.item(0);

                String[] tempArr = node.getTextContent().split("-");
                this.serviceDescription.setPort(Integer.parseInt(tempArr[tempArr.length - 1]));
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.updateCapabilities();
        }
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "urn:lge-com:service:webos-second-screen:1");
    }

    @Override
    public boolean isConnected() {
        if (this.socket == null) return false;
        try {
            if (DiscoveryManager.getInstance().getPairingLevel().compareTo(PairingLevel.PROTECTED) >= 0) {
                return this.socket.isConnected() && this.socket.getClientKey() != "";
            } else {
                return this.socket.isConnected();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void connect() {
        if (this.socket == null) {
            this.socket = new WebOSTVServiceSocketClient(this.getWebOSTVServiceConfig(), this.getPairingType(), getPermissions(),
                    WebOSTVServiceSocketClient.getURI(this.getServiceDescription().getIpAddress(), this.getServiceDescription().getPort()));
            this.socket.setListener(mSocketListener);
        }

        if (!this.isConnected())
            this.socket.connect();
    }

    @Override
    public void disconnect() {
        Log.d(Util.T, "attempting to disconnect to " + serviceDescription.getIpAddress());

        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                if (listener != null)
                    listener.onDisconnect(WebOSTVService.this, null);
            }
        });
        try {
            if (socket != null) {
                socket.setListener(null);
                socket.disconnect();
                socket = null;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if (mAppToAppIdMappings != null)
            mAppToAppIdMappings.clear();

        if (mWebAppSessions != null) {
            Enumeration<WebOSWebAppSession> iterator = mWebAppSessions.elements();

            while (iterator.hasMoreElements()) {
                WebOSWebAppSession session = iterator.nextElement();
                session.disconnectFromWebApp();
            }

            mWebAppSessions.clear();
        }
    }

    @Override
    public void cancelPairing() {
        try {
            if (this.socket != null) {
                this.socket.disconnect();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public String getClientKey() {
        return this.getWebOSTVServiceConfig().getClientKey();
    }

    public void setClientKey(String ClientKey) {
        this.getWebOSTVServiceConfig().setClientKey(ClientKey);
    }

    private WebOSTVServiceSocketClientListener mSocketListener = new WebOSTVServiceSocketClientListener() {

        @Override
        public void onRegistrationFailed(final ServiceCommandError error) {
            disconnect();

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {
                    if (listener != null)
                        listener.onConnectionFailure(WebOSTVService.this, error);

                }
            });
        }

        @Override
        public Boolean onReceiveMessage(JSONObject message) { return true; }

        @Override
        public void updateClientKey(String ClientKey) {
            try {
                setClientKey(ClientKey);
            } catch (Exception e) {
                Log.e("ConnectSDK",e.getMessage(), e);
            }
        }

        @Override
        public void updateUUID(String UUID) {
            serviceDescription.setUUID(UUID);
        }

        @Override
        public void updateIPAddress(String IPAddress) {
            serviceDescription.setIpAddress(IPAddress);
        }

        @Override
        public void onFailWithError(final ServiceCommandError error) {
            try {
                socket.setListener(null);
                socket.disconnect();
                socket = null;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {
                    if (listener != null)
                        listener.onConnectionFailure(WebOSTVService.this, error);
                }
            });
        }

        @Override
        public void onConnect() {
            reportConnected(true);
        }

        @Override
        public void onCloseWithError(final ServiceCommandError error) {
            try {
                socket.setListener(null);
                socket.disconnect();
                socket = null;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {
                    if (listener != null)
                        listener.onDisconnect(WebOSTVService.this, error);
                }
            });
        }

        @Override
        public void onBeforeRegister(final PairingType pairingType) {
            if (DiscoveryManager.getInstance().getPairingLevel().compareTo(PairingLevel.PROTECTED) >= 0) {
                Util.runOnUI(new Runnable() {

                    @Override
                    public void run() {
                        if (listener != null)
                            listener.onPairingRequired(WebOSTVService.this, pairingType, null);
                    }
                });
            }
        }
    };

    // @cond INTERNAL

    public ConcurrentHashMap<String, String> getWebAppIdMappings() {
        return mAppToAppIdMappings;
    }

    // @endcond

    @Override
    public Launcher getLauncher() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void launchApp(String appId, AppLaunchListener listener) {
        AppInfo appInfo = new AppInfo();
        appInfo.setId(appId);

        launchAppWithInfo(appInfo, listener);
    }

    @Override
    public void launchAppWithInfo(AppInfo appInfo, Launcher.AppLaunchListener listener) {
        launchAppWithInfo(appInfo, null, listener);
    }

    @Override
    public void launchAppWithInfo(final AppInfo appInfo, Object params, final Launcher.AppLaunchListener listener) {
        String uri = "ssap://system.launcher/launch";
        JSONObject payload = new JSONObject();

        final String appId = appInfo.getId();

        String contentId = null;

        if (params != null) {
            try {
                contentId = (String) ((JSONObject) params).get("contentId");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            payload.put("id", appId);

            if (contentId != null)
                payload.put("contentId", contentId);

            if (params != null)
                payload.put("params", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                JSONObject obj = (JSONObject) response;
                LaunchSession launchSession = new LaunchSession();

                launchSession.setService(WebOSTVService.this);
                launchSession.setAppId(appId); // note that response uses id to mean appId
                launchSession.setSessionId(obj.optString("sessionId"));
                launchSession.setSessionType(LaunchSessionType.App);

                Util.postSuccess(listener, launchSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
        request.send();
    }


    @Override
    public void launchBrowser(String url, final Launcher.AppLaunchListener listener) {
        String uri = "ssap://system.launcher/open";
        JSONObject payload = new JSONObject();

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                JSONObject obj = (JSONObject) response;
                LaunchSession launchSession = new LaunchSession();

                launchSession.setService(WebOSTVService.this);
                launchSession.setAppId(obj.optString("id")); // note that response uses id to mean appId
                launchSession.setSessionId(obj.optString("sessionId"));
                launchSession.setSessionType(LaunchSessionType.App);
                launchSession.setRawData(obj);

                Util.postSuccess(listener, launchSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        try {
            payload.put("target", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
        request.send();
    }

    @Override
    public void launchYouTube(String contentId, Launcher.AppLaunchListener listener) {
        launchYouTube(contentId, (float)0.0, listener);
    }

    @Override
    public void launchYouTube(final String contentId, float startTime, final AppLaunchListener listener) {
        JSONObject params = new JSONObject();

        if (contentId != null && contentId.length() > 0) {
            if (startTime < 0.0) {
                Util.postError(listener, new ServiceCommandError(0, "Start time may not be negative", null));
                return;
            }

            try {
                params.put("contentId", String.format("%s&pairingCode=%s&t=%.1f", contentId, UUID.randomUUID().toString(), startTime));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        AppInfo appInfo = new AppInfo() {{
            setId("youtube.leanback.v4");
            setName("YouTube");
        }};

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void launchHulu(String contentId, Launcher.AppLaunchListener listener) {
        JSONObject params = new JSONObject();

        try {
            params.put("contentId", contentId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AppInfo appInfo = new AppInfo() {{
            setId("hulu");
            setName("Hulu");
        }};

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void launchNetflix(String contentId, Launcher.AppLaunchListener listener) {
        JSONObject params = new JSONObject();
        String netflixContentId = "m=http%3A%2F%2Fapi.netflix.com%2Fcatalog%2Ftitles%2Fmovies%2F" + contentId + "&source_type=4";

        try {
            params.put("contentId", netflixContentId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AppInfo appInfo = new AppInfo() {{
            setId("netflix");
            setName("Netflix");
        }};

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void launchAppStore(String appId, AppLaunchListener listener) {
        AppInfo appInfo = new AppInfo("com.webos.app.discovery");
        appInfo.setName("LG Store");

        JSONObject params = new JSONObject();

        if (appId != null && appId.length() > 0) {
            String query = String.format("category/GAME_APPS/%s", appId);
            try {
                params.put("query", query);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void closeApp(LaunchSession launchSession, ResponseListener<Object> listener) {
        String uri = "ssap://system.launcher/close";
        String appId = launchSession.getAppId();
        String sessionId = launchSession.getSessionId();

        JSONObject payload = new JSONObject();

        try {
            payload.put("id", appId);
            payload.put("sessionId", sessionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(launchSession.getService(), uri, payload, true, listener);
        request.send();
    }

    @Override
    public void getAppList(final AppListListener listener) {
        String uri = "ssap://com.webos.applicationManager/listApps";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {

                try {
                    JSONObject jsonObj = (JSONObject) response;

                    JSONArray apps = (JSONArray) jsonObj.get("apps");
                    List<AppInfo> appList = new ArrayList<AppInfo>();

                    for (int i = 0; i < apps.length(); i++)
                    {
                        final JSONObject appObj = apps.getJSONObject(i);

                        AppInfo appInfo = new AppInfo() {{
                            setId(appObj.getString("id"));
                            setName(appObj.getString("title"));
                            setRawData(appObj);
                        }};

                        appList.add(appInfo);
                        Log.d("#####Service", "onSuccess: "+ appInfo.getRawData());
                    }

                    Util.postSuccess(listener, appList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
    }

    @Override
    public void getLaunchPoints(Launcher.LaunchPointsListener listener) {
        String uri = "ssap://com.webos.applicationManager/listLaunchPoints";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {

                try {
                    JSONObject jsonObj = (JSONObject) response;
                    JSONArray launchPoints = (JSONArray) jsonObj.get("launchPoints");
                    Util.postSuccess(listener, launchPoints);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
    }


    @Override
    public void getRunningApp(AppInfoListener listener) {
        getRunningApp(false, listener);
    }

    @Override
    public ServiceSubscription<AppInfoListener> subscribeRunningApp(AppInfoListener listener) {
        return (URLServiceSubscription<AppInfoListener>) getRunningApp(true, listener);
    }

    @Override
    public void getAppState(LaunchSession launchSession, AppStateListener listener) {
        getAppState(false, launchSession, listener);
    }

    @Override
    public ServiceSubscription<AppStateListener> subscribeAppState(LaunchSession launchSession, AppStateListener listener) {
        return (URLServiceSubscription<AppStateListener>) getAppState(true, launchSession, listener);
    }


    /******************
    TOAST CONTROL
     *****************/
    @Override
    public ToastControl getToastControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getToastControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void showToast(String message, ResponseListener<Object> listener) {
        showToast(message, null, null, listener);
    }

    @Override
    public void showToast(String message, String iconData, String iconExtension, ResponseListener<Object> listener)
    {
        JSONObject payload = new JSONObject();

        try {
            payload.put("message", message);

            if (iconData != null)
            {
                payload.put("iconData", iconData);
                payload.put("iconExtension", iconExtension);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendToast(payload, listener);
    }

    @Override
    public void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, ResponseListener<Object> listener) {
        showClickableToastForApp(message, appInfo, params, null, null, listener);
    }

    @Override
    public void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, String iconData, String iconExtension, ResponseListener<Object> listener) {
        JSONObject payload = new JSONObject();

        try {
            payload.put("message", message);

            if (iconData != null) {
                payload.put("iconData", iconData);
                payload.put("iconExtension", iconExtension);
            }

            if (appInfo != null) {
                JSONObject onClick = new JSONObject();
                onClick.put("appId", appInfo.getId());
                if (params != null) {
                    onClick.put("params", params);
                }
                payload.put("onClick", onClick);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendToast(payload, listener);
    }

    @Override
    public void showClickableToastForURL(String message, String url, ResponseListener<Object> listener) {
        showClickableToastForURL(message, url, null, null, listener);
    }

    @Override
    public void showClickableToastForURL(String message, String url, String iconData, String iconExtension, ResponseListener<Object> listener) {
        JSONObject payload = new JSONObject();

        try {
            payload.put("message", message);

            if (iconData != null) {
                payload.put("iconData", iconData);
                payload.put("iconExtension", iconExtension);
            }

            if (url != null) {
                JSONObject onClick = new JSONObject();
                onClick.put("target", url);
                payload.put("onClick", onClick);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendToast(payload, listener);
    }


    /******************
    VOLUME CONTROL
     *****************/
    @Override
    public VolumeControl getVolumeControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void volumeUp() {
        volumeUp(null);
    }

    @Override
    public void volumeUp(ResponseListener<Object> listener) {
        String uri = "ssap://audio/volumeUp";
        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);

        request.send();
    }

    public void volumeDown() {
        volumeDown(null);
    }

    @Override
    public void volumeDown(ResponseListener<Object> listener) {
        String uri = "ssap://audio/volumeDown";
        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);

        request.send();
    }

    public void setVolume(int volume) {
        setVolume(volume, null);
    }

    @Override
    public void setVolume(float volume, ResponseListener<Object> listener) {
        String uri = "ssap://audio/setVolume";
        JSONObject payload = new JSONObject();
        int intVolume = (int) Math.round(volume*100.0f);

        try {
            payload.put("volume", intVolume);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
        request.send();
    }

    @Override
    public void getVolume(VolumeListener listener) {
        getVolume(false, listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
        return (ServiceSubscription<VolumeListener>) getVolume(true, listener);
    }

    @Override
    public void setMute(boolean isMute, ResponseListener<Object> listener) {
        String uri = "ssap://audio/setMute";
        JSONObject payload = new JSONObject();

        try {
            payload.put("mute", isMute);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
        request.send();
    }

    @Override
    public void getMute(MuteListener listener) {
        getMuteStatus(false, listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
        return (ServiceSubscription<MuteListener>) getMuteStatus(true, listener);
    }

    public void getVolumeStatus(VolumeStatusListener listener) {
        getVolumeStatus(false, listener);
    }

    @SuppressWarnings("unchecked")
    public ServiceSubscription<VolumeStatusListener> subscribeVolumeStatus(VolumeStatusListener listener) {
        return (ServiceSubscription<VolumeStatusListener>) getVolumeStatus(true, listener);
    }


    /******************
    MEDIA PLAYER
     *****************/
    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void getMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(
            MediaInfoListener listener) {
        listener.onError(ServiceCommandError.notSupported());
        return null;
    }

    private void displayMedia(JSONObject params, final MediaPlayer.LaunchListener listener) {
        String uri = "ssap://media.viewer/open";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
            @Override
            public void onSuccess(Object response) {
                JSONObject obj = (JSONObject) response;

                LaunchSession launchSession = LaunchSession.launchSessionForAppId(obj.optString("id"));
                launchSession.setService(WebOSTVService.this);
                launchSession.setSessionId(obj.optString("sessionId"));
                launchSession.setSessionType(LaunchSessionType.Media);

                Util.postSuccess(listener, new MediaLaunchObject(launchSession, WebOSTVService.this));
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, params, true, responseListener);
        request.send();
    }

    @Override
    public void displayImage(final String url, final String mimeType, final String title, final String description, final String iconSrc, final MediaPlayer.LaunchListener listener) {
        if ("4.0.0".equalsIgnoreCase(this.serviceDescription.getVersion())) {
            DeviceService dlnaService = this.getDLNAService();

            if (dlnaService != null) {
                MediaPlayer mediaPlayer = dlnaService.getAPI(MediaPlayer.class);

                if (mediaPlayer != null) {
                    mediaPlayer.displayImage(url, mimeType, title, description, iconSrc, listener);
                    return;
                }
            }

            JSONObject params = null;

            try {
                params = new JSONObject() {{
                    put("target", url);
                    put("title", title == null ? NULL : title);
                    put("description", description == null ? NULL : description);
                    put("mimeType", mimeType == null ? NULL : mimeType);
                    put("iconSrc", iconSrc == null ? NULL : iconSrc);
                }};
            } catch (JSONException ex) {
                ex.printStackTrace();
                Util.postError(listener, new ServiceCommandError(-1, ex.getLocalizedMessage(), ex));
            }

            if (params != null)
                this.displayMedia(params, listener);
        } else {
            final WebAppSession.LaunchListener webAppLaunchListener = new WebAppSession.LaunchListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    listener.onError(error);
                }

                @Override
                public void onSuccess(WebAppSession webAppSession) {
                    webAppSession.displayImage(url, mimeType, title, description, iconSrc, listener);
                }
            };

            this.getWebAppLauncher().joinWebApp(MEDIA_PLAYER_ID, new WebAppSession.LaunchListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    getWebAppLauncher().launchWebApp(MEDIA_PLAYER_ID, webAppLaunchListener);
                }

                @Override
                public void onSuccess(WebAppSession webAppSession) {
                    webAppSession.displayImage(url, mimeType, title, description, iconSrc, listener);
                }
            });
        }
    }

    @Override
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener listener) {
        String mediaUrl = null;
        String mimeType = null;
        String title = null;
        String desc = null;
        String iconSrc = null;

        if (mediaInfo != null) {
            mediaUrl = mediaInfo.getUrl();
            mimeType = mediaInfo.getMimeType();
            title = mediaInfo.getTitle();
            desc = mediaInfo.getDescription();

            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                ImageInfo imageInfo = mediaInfo.getImages().get(0);
                iconSrc = imageInfo.getUrl();
            }
        }

        displayImage(mediaUrl, mimeType, title, desc, iconSrc, listener);
    }

    @Override
    public void playMedia(String url, String mimeType, String title, String description,
                          String iconSrc, boolean shouldLoop, MediaPlayer.LaunchListener listener) {
        MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build();
        playMedia(mediaInfo, shouldLoop, listener);
    }

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop,
                          MediaPlayer.LaunchListener listener) {
        if ("4.0.0".equalsIgnoreCase(this.serviceDescription.getVersion())) {
            playMediaByNativeApp(mediaInfo, shouldLoop, listener);
        } else {
            playMediaByWebApp(mediaInfo, shouldLoop, listener);
        }
    }

    private void playMediaByWebApp(final MediaInfo mediaInfo, final boolean shouldLoop,
                                   final LaunchListener listener) {
        final WebAppSession.LaunchListener webAppLaunchListener = new WebAppSession.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.d("playmedia", "onError: ");
                listener.onError(error);
            }

            @Override
            public void onSuccess(WebAppSession webAppSession) {
                Log.d("playmedia", "onSuccess: ");
                webAppSession.playMedia(mediaInfo, shouldLoop, listener);
            }
        };

        getWebAppLauncher().joinWebApp(MEDIA_PLAYER_ID, new WebAppSession.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                getWebAppLauncher().launchWebApp(MEDIA_PLAYER_ID, webAppLaunchListener);
            }

            @Override
            public void onSuccess(WebAppSession webAppSession) {
                webAppSession.playMedia(mediaInfo, shouldLoop, listener);
            }
        });
    }

    private void playMediaByNativeApp(MediaInfo mediaInfo, boolean shouldLoop,
                                      LaunchListener listener) {
        DeviceService dlnaService = this.getDLNAService();

        if (dlnaService != null) {
            MediaPlayer mediaPlayer = dlnaService.getAPI(MediaPlayer.class);

            if (mediaPlayer != null) {
                mediaPlayer.playMedia(mediaInfo, shouldLoop, listener);
                return;
            }
        }

        String iconSrc = null;
        List<ImageInfo> images = mediaInfo.getImages();

        if (images != null && !images.isEmpty()) {
            ImageInfo iconImage = images.get(0);
            if (iconImage != null) {
                iconSrc = iconImage.getUrl();
            }
        }

        try {
            JSONObject params =
                    createPlayMediaJsonRequestForSsap(mediaInfo, shouldLoop, iconSrc);
            displayMedia(params, listener);
        } catch (JSONException ex) {
            Util.postError(listener, new ServiceCommandError(-1, ex.getLocalizedMessage(), ex));
            Log.e(Util.T, "Create JSON request for ssap://media.viewer/open failure", ex);
        }
    }

    @NonNull
    private JSONObject createPlayMediaJsonRequestForSsap(final MediaInfo mediaInfo, final boolean
            shouldLoop, final String iconSrc) throws JSONException {
        return new JSONObject() {{
            put("target", mediaInfo.getUrl());
            put("title", getJsonValue(mediaInfo.getTitle()));
            put("description", getJsonValue(mediaInfo.getDescription()));
            put("mimeType", getJsonValue(mediaInfo.getMimeType()));
            put("iconSrc", getJsonValue(iconSrc));
            put("loop", shouldLoop);
        }};
    }

    private Object getJsonValue(Object value) {
        return value == null ? JSONObject.NULL : value;
    }


        @Override
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
        JSONObject payload = new JSONObject();

        try {
            if (launchSession.getAppId() != null && launchSession.getAppId().length() > 0)
                payload.put("id", launchSession.getAppId());

            if (launchSession.getSessionId() != null && launchSession.getSessionId().length() > 0)
                payload.put("sessionId", launchSession.getSessionId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(launchSession.getService(), CLOSE_MEDIA_URI, payload, true, listener);
        request.send();
    }

     /* ***************
     * TV CONTROL
     * ***************
     */
    @Override
    public TVControl getTVControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getTVControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void channelUp() {
        channelUp(null);
    }

    @Override
    public void channelUp(ResponseListener<Object> listener) {
        String uri = "ssap://tv/channelUp";

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
        request.send();
    }

    public void channelDown() {
        channelDown(null);
    }

    @Override
    public void channelDown(ResponseListener<Object> listener) {
        String uri = "ssap://tv/channelDown";

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
        request.send();
    }

    /**
     * Sets current channel
     * @param channelInfo must not be null
     * @param listener
     * @throws NullPointerException if channelInfo is null
     */
    @Override
    public void setChannel(ChannelInfo channelInfo, ResponseListener<Object> listener) {
        if (channelInfo == null) {
            throw new NullPointerException("channelInfo must not be null");
        }
        String uri = "ssap://tv/openChannel";
        JSONObject payload = new JSONObject();

        try {
            if (channelInfo.getId() != null) {
                payload.put("channelId", channelInfo.getId());
            }
            if (channelInfo.getNumber() != null) {
                payload.put("channelNumber", channelInfo.getNumber());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
        request.send();
    }

    public void setChannelById(String channelId) {
        setChannelById(channelId, null);
    }

    public void setChannelById(String channelId, ResponseListener<Object> listener) {
        String uri = "ssap://tv/openChannel";
        JSONObject payload = new JSONObject();

        try {
            payload.put("channelId", channelId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
        request.send();
    }

    @Override
    public void getCurrentChannel(ChannelListener listener) {
        getCurrentChannel(false, listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceSubscription<ChannelListener> subscribeCurrentChannel(ChannelListener listener) {
        return (ServiceSubscription<ChannelListener>) getCurrentChannel(true, listener);
    }

    @Override
    public void getChannelList(ChannelListListener listener) {
        getChannelList(false, listener);
    }

    @SuppressWarnings("unchecked")
    public ServiceSubscription<ChannelListListener> subscribeChannelList(final ChannelListListener listener) {
        return (ServiceSubscription<ChannelListListener>) getChannelList(true, listener);
    }

    public void getChannelCurrentProgramInfo(ProgramInfoListener listener) {
        getChannelCurrentProgramInfo(false, listener);
    }

    @SuppressWarnings("unchecked")
    public ServiceSubscription<ProgramInfoListener> subscribeChannelCurrentProgramInfo(ProgramInfoListener listener) {
        return (ServiceSubscription<ProgramInfoListener>) getChannelCurrentProgramInfo(true, listener);
    }

    @Override
    public void getProgramInfo(ProgramInfoListener listener) {
        // TODO need to parse current program when program id is correct
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<ProgramInfoListener> subscribeProgramInfo(ProgramInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return new NotSupportedServiceSubscription<ProgramInfoListener>();
    }

    @Override
    public void getProgramList(ProgramListListener listener) {
        getProgramList(false, listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceSubscription<ProgramListListener> subscribeProgramList(ProgramListListener listener) {
        return (ServiceSubscription<ProgramListListener>) getProgramList(true, listener);
    }

    @Override
    public void set3DEnabled(final boolean enabled, final ResponseListener<Object> listener) {
        String uri;
        if (enabled)
            uri = "ssap://com.webos.service.tv.display/set3DOn";
        else
            uri = "ssap://com.webos.service.tv.display/set3DOff";

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);

        request.send();
    }

    private ServiceCommand<State3DModeListener> get3DEnabled(boolean isSubscription, final State3DModeListener listener) {
        String uri = "ssap://com.webos.service.tv.display/get3DStatus";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                JSONObject jsonObj = (JSONObject)response;

                JSONObject status;
                try {
                    status = jsonObj.getJSONObject("status3D");
                    boolean isEnabled = status.getBoolean("status");

                    Util.postSuccess(listener, isEnabled);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<State3DModeListener> request;
        if (isSubscription)
            request = new URLServiceSubscription<State3DModeListener>(this, uri, null, true, responseListener);
        else
            request = new ServiceCommand<State3DModeListener>(this, uri, null, true, responseListener);

        request.send();

        return request;
    }

    @Override
    public void get3DEnabled(final State3DModeListener listener) {
        get3DEnabled(false, listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceSubscription<State3DModeListener> subscribe3DEnabled(final State3DModeListener listener) {
        return (ServiceSubscription<State3DModeListener>) get3DEnabled(true, listener);
    }


    /**************
    EXTERNAL INPUT
     **************/
    @Override
    public ExternalInputControl getExternalInput() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getExternalInputControlPriorityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void launchInputPicker(final AppLaunchListener listener) {
        final AppInfo appInfo = new AppInfo() {{
            setId("com.webos.app.inputpicker");
            setName("InputPicker");
        }};

        launchAppWithInfo(appInfo, null, new AppLaunchListener() {
            @Override
            public void onSuccess(LaunchSession object) {
                listener.onSuccess(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                appInfo.setId("com.webos.app.inputmgr");
                launchAppWithInfo(appInfo, null, listener);
            }
        });
    }

    @Override
    public void closeInputPicker(LaunchSession launchSession, ResponseListener<Object> listener) {
        closeApp(launchSession, listener);
    }

    @Override
    public void getExternalInputList(final ExternalInputListListener listener) {
        String uri = "ssap://tv/getExternalInputList";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                try {
                    JSONObject jsonObj = (JSONObject)response;
                    JSONArray devices = (JSONArray) jsonObj.get("devices");
                    Util.postSuccess(listener, externalnputInfoFromJSONArray(devices));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
    }

    @Override
    public void setExternalInput(ExternalInputInfo externalInputInfo , final ResponseListener<Object> listener) {
        String uri = "ssap://tv/switchInput";

        JSONObject payload = new JSONObject();

        try {
            if (externalInputInfo  != null && externalInputInfo .getId() != null) {
                payload.put("inputId", externalInputInfo.getId());
            }
            else {
                Log.w(Util.T, "ExternalInputInfo has no id");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
        request.send();
    }


    /**************
    MOUSE CONTROL
     **************/
    @Override
    public MouseControl getMouseControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMouseControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void connectMouse() {
        connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
            @Override
            public void onConnected() {
                // intentionally left empty
            }
        });
    }

    @Override
    public void disconnectMouse() {
        if (mouseSocket == null)
            return;
        try {
            mouseSocket.disconnect();
            mouseSocket = null;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void connectMouse(final WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener successHandler) {
        if (mouseSocket != null)
            return;

        String uri = "ssap://com.webos.service.networkinput/getPointerInputSocket";

        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                try {
                    JSONObject jsonObj = (JSONObject)response;
                    String socketPath = (String) jsonObj.get("socketPath");
                    mouseSocket = new WebOSTVMouseSocketConnection(socketPath, successHandler);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Log.w(Util.T, "Connect mouse error: " + error.getMessage());
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
        request.send();
    }

    @Override
    public void click() {
        if (mouseSocket != null) {
            mouseSocket.click();
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.click();
                }
            });
        }
    }

    @Override
    public void move(final double dx, final double dy) {
        if (mouseSocket != null) {
            mouseSocket.move(dx, dy);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.move(dx, dy);
                }
            });
        }
    }

    @Override
    public void move(PointF diff) {
        move(diff.x, diff.y);
    }

    @Override
    public void scroll(final double dx, final double dy) {
        if (mouseSocket != null) {
            mouseSocket.scroll(dx, dy);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.scroll(dx, dy);
                }
            });
        }
    }

    @Override
    public void scroll(PointF diff) {
        scroll(diff.x, diff.y);
    }


    /**************
    KEYBOARD CONTROL
     **************/
    @Override
    public TextInputControl getTextInputControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(TextInputStatusListener listener) {
        keyboardInput = new WebOSTVKeyboardInput(this);
        return keyboardInput.connect(listener);
    }

    @Override
    public void sendText(String input) {
        keyboardInput = new WebOSTVKeyboardInput(this);
        Log.d("sendText", "sendText input: " + input);
        keyboardInput.addToQueue(input);
    }

    @Override
    public void sendKeyCode(KeyCode keycode, ResponseListener<Object> listener) {
        switch (keycode) {
            case NUM_0:
            case NUM_1:
            case NUM_2:
            case NUM_3:
            case NUM_4:
            case NUM_5:
            case NUM_6:
            case NUM_7:
            case NUM_8:
            case NUM_9:
                sendSpecialKey(String.valueOf(keycode.getCode()), listener);
                break;
            case DASH:
                sendSpecialKey("DASH", listener);
                break;
            case ENTER:
                sendSpecialKey("ENTER", listener);
                break;
            default:
                Util.postError(listener, new ServiceCommandError(0, "The keycode is not available", null));
        }
    }

    @Override
    public void sendKeyCodeString(String keycode, ResponseListener<Object> listener) {
        sendSpecialKey(keycode, listener);
    }

    @Override
    public void sendEnter() {
        if (keyboardInput != null) {
            keyboardInput.sendEnter();
        }
    }

    @Override
    public void sendDelete() {
        Log.d("sendDelete", "sendDelete: ");
        keyboardInput = new WebOSTVKeyboardInput(this);
        keyboardInput.sendDel();
    }


    /**
     * ***********
     * KEY CONTROL
     * ************
     */
    @Override
    public KeyControl getKeyControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getKeyControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    private void sendSpecialKey(final String key, final ResponseListener<Object> listener) {
        if (mouseSocket != null) {
            mouseSocket.button(key);
            Util.postSuccess(listener, null);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.button(key);
                    Util.postSuccess(listener, null);
                }
            });
        }
    }

    @Override
    public void up(ResponseListener<Object> listener) {
        sendSpecialKey("UP", listener);
    }

    @Override
    public void down(ResponseListener<Object> listener) {
        sendSpecialKey("DOWN", listener);
    }

    @Override
    public void left(ResponseListener<Object> listener) {
        sendSpecialKey("LEFT", listener);
    }

    @Override
    public void right(ResponseListener<Object> listener) {
        sendSpecialKey("RIGHT", listener);
    }

    @Override
    public void ok(final ResponseListener<Object> listener) {
        if (mouseSocket != null) {
            mouseSocket.click();
            Util.postSuccess(listener, null);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.click();
                    Util.postSuccess(listener, null);
                }
            });
        }
    }

    @Override
    public void back(ResponseListener<Object> listener) {
        sendSpecialKey("BACK", listener);
    }

    @Override
    public void home(ResponseListener<Object> listener) {
        sendSpecialKey("HOME", listener);
    }


    /**************
    Web App Launcher
     **************/

    @Override
    public WebAppLauncher getWebAppLauncher() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void launchWebApp(final String webAppId, final WebAppSession.LaunchListener listener) {
        this.launchWebApp(webAppId, null, true, listener);
    }

    @Override
    public void launchWebApp(String webAppId, boolean relaunchIfRunning, WebAppSession.LaunchListener listener) {
        launchWebApp(webAppId, null, relaunchIfRunning, listener);
    }

    public void launchWebApp(final String webAppId, final JSONObject params, final WebAppSession.LaunchListener listener) {
        if (webAppId == null || webAppId.length() == 0) {
            Util.postError(listener, new ServiceCommandError(-1, "You need to provide a valid webAppId.", null));

            return;
        }

        final WebOSWebAppSession _webAppSession = mWebAppSessions.get(webAppId);

        String uri = "ssap://webapp/launchWebApp";
        JSONObject payload = new JSONObject();

        try {
            payload.put("webAppId", webAppId);

            if (params != null)
                payload.put("urlParams", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(final Object response) {
                JSONObject obj = (JSONObject) response;

                LaunchSession launchSession = null;
                WebOSWebAppSession webAppSession = _webAppSession;

                if (webAppSession != null)
                    launchSession = webAppSession.launchSession;
                else {
                    launchSession = LaunchSession.launchSessionForAppId(webAppId);
                    webAppSession = new WebOSWebAppSession(launchSession, WebOSTVService.this);
                    mWebAppSessions.put(webAppId, webAppSession);
                }

                launchSession.setService(WebOSTVService.this);
                launchSession.setSessionId(obj.optString("sessionId"));
                launchSession.setSessionType(LaunchSessionType.WebApp);
                launchSession.setRawData(obj);

                Util.postSuccess(listener, webAppSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
        request.send();
    }

    @Override
    public void launchWebApp(final String webAppId, final JSONObject params, boolean relaunchIfRunning, final WebAppSession.LaunchListener listener) {
        if (webAppId == null) {
            Util.postError(listener, new ServiceCommandError(0, "Must pass a web App id", null));
            return;
        }

        if (relaunchIfRunning) {
            launchWebApp(webAppId, params, listener);
        } else {
            getLauncher().getRunningApp(new AppInfoListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    Util.postError(listener, error);
                }

                @Override
                public void onSuccess(AppInfo appInfo) {
                    //  TODO: this will only work on pinned apps, currently
                    if (appInfo.getId().indexOf(webAppId) != -1) {
                        LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
                        launchSession.setSessionType(LaunchSessionType.WebApp);
                        launchSession.setService(WebOSTVService.this);
                        launchSession.setRawData(appInfo.getRawData());

                        WebOSWebAppSession webAppSession = webAppSessionForLaunchSession(launchSession);

                        Util.postSuccess(listener, webAppSession);
                    } else {
                        launchWebApp(webAppId, params, listener);
                    }
                }
            });
        }
    }

    @Override
    public void closeWebApp(LaunchSession launchSession, final ResponseListener<Object> listener) {
        if (launchSession == null || launchSession.getAppId() == null || launchSession.getAppId().length() == 0) {
            Util.postError(listener, new ServiceCommandError(0, "Must provide a valid launch session", null));
            return;
        }

        final WebOSWebAppSession webAppSession = mWebAppSessions.get(launchSession.getAppId());
        if (webAppSession != null) {
            webAppSession.disconnectFromWebApp();
        }

        String uri = "ssap://webapp/closeWebApp";
        JSONObject payload = new JSONObject();

        try {
            if (launchSession.getAppId() != null) payload.put("webAppId", launchSession.getAppId());
            if (launchSession.getSessionId() != null) payload.put("sessionId", launchSession.getSessionId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
        request.send();
    }

    public void connectToWebApp(final WebOSWebAppSession webAppSession, final boolean joinOnly, final ResponseListener<Object> connectionListener) {
        if (mWebAppSessions == null)
            mWebAppSessions = new ConcurrentHashMap<String, WebOSWebAppSession>();

        if (mAppToAppIdMappings == null)
            mAppToAppIdMappings = new ConcurrentHashMap<String, String>();

        if (webAppSession == null || webAppSession.launchSession == null) {
            Util.postError(connectionListener, new ServiceCommandError(0, "You must provide a valid LaunchSession object", null));
            return;
        }

        String _appId = webAppSession.launchSession.getAppId();
        String _idKey = null;

        if (webAppSession.launchSession.getSessionType() == LaunchSession.LaunchSessionType.WebApp)
            _idKey = "webAppId";
        else
            _idKey = "appId";

        if (_appId == null || _appId.length() == 0) {
            Util.postError(connectionListener, new ServiceCommandError(-1, "You must provide a valid web app session", null));

            return;
        }

        final String appId = _appId;
        final String idKey = _idKey;

        String uri = "ssap://webapp/connectToApp";
        JSONObject payload = new JSONObject();

        try {
            payload.put(idKey, appId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(final Object response) {
                JSONObject jsonObj = (JSONObject)response;

                String state = jsonObj.optString("state");

                if (!state.equalsIgnoreCase("CONNECTED")) {
                    if (joinOnly && state.equalsIgnoreCase("WAITING_FOR_APP")) {
                        Util.postError(connectionListener, new ServiceCommandError(0, "Web app is not currently running", null));
                    }

                    return;
                }

                String fullAppId = jsonObj.optString("appId");

                if (fullAppId != null && fullAppId.length() != 0) {
                    if (webAppSession.launchSession.getSessionType() == LaunchSessionType.WebApp)
                        mAppToAppIdMappings.put(fullAppId, appId);

                    webAppSession.setFullAppId(fullAppId);
                }

                if (connectionListener != null) {
                    Util.runOnUI(new Runnable() {

                        @Override
                        public void run() {
                            connectionListener.onSuccess(response);
                        }
                    });
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                webAppSession.disconnectFromWebApp();

                boolean appChannelDidClose = false;

                if (error != null && error.getPayload() != null)
                    appChannelDidClose = error.getPayload().toString().contains("app channel closed");

                if (appChannelDidClose) {
                    if (webAppSession.getWebAppSessionListener() != null) {
                        Util.runOnUI(new Runnable() {

                            @Override
                            public void run() {
                                webAppSession.getWebAppSessionListener().onWebAppSessionDisconnect(webAppSession);
                            }
                        });
                    }
                } else {
                    Util.postError(connectionListener, error);
                }
            }
        };

        webAppSession.appToAppSubscription = new URLServiceSubscription<ResponseListener<Object>>(webAppSession.socket, uri, payload, true, responseListener);
        webAppSession.appToAppSubscription.subscribe();
    }

    private void notifyPairingRequired() {
        if (listener != null) {
            listener.onPairingRequired(this, pairingType, null);
        }
    }

    @Override
    public void pinWebApp(String webAppId, final ResponseListener<Object> listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError(-1, "You must provide a valid web app id", null));
            }
            return;
        }

        String uri = "ssap://webapp/pinWebApp";
        JSONObject payload = new JSONObject();

        try {
            payload.put("webAppId", webAppId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(final Object response) {
                JSONObject obj = (JSONObject) response;
                if (obj.has("pairingType")) {
                    notifyPairingRequired();
                }
                else if (listener != null) {
                    listener.onSuccess(response);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request =
                new URLServiceSubscription<ResponseListener<Object>>(this, uri, payload, true, responseListener);
        request.send();
    }

    @Override
    public void unPinWebApp(String webAppId, final ResponseListener<Object> listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError(-1, "You must provide a valid web app id", null));
            }
            return;
        }

        String uri = "ssap://webapp/removePinnedWebApp";
        JSONObject payload = new JSONObject();

        try {
            payload.put("webAppId", webAppId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(final Object response) {
                JSONObject obj = (JSONObject) response;
                if (obj.has("pairingType")) {
                    notifyPairingRequired();
                }
                else if (listener != null) {
                    listener.onSuccess(response);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request =
                new URLServiceSubscription<ResponseListener<Object>>(this, uri, payload, true, responseListener);
        request.send();
    }

    private ServiceCommand<WebAppPinStatusListener> isWebAppPinned(boolean isSubscription, String webAppId, final WebAppPinStatusListener listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError(-1, "You must provide a valid web app id", null));
            }
            return null;
        }

        String uri = "ssap://webapp/isWebAppPinned";
        JSONObject payload = new JSONObject();

        try {
            payload.put("webAppId", webAppId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(final Object response) {
                JSONObject obj = (JSONObject) response;

                boolean status = obj.optBoolean("pinned");

                if (listener != null) {
                    listener.onSuccess(status);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<WebAppPinStatusListener> request;
        if (isSubscription)
            request = new URLServiceSubscription<WebAppPinStatusListener>(this, uri, payload, true, responseListener);
        else
            request = new ServiceCommand<WebAppPinStatusListener>(this, uri, payload, true, responseListener);

        request.send();

        return request;
    }

    @Override
    public void isWebAppPinned(String webAppId, WebAppPinStatusListener listener) {
        isWebAppPinned(false, webAppId, listener);
    }

    @Override
    public ServiceSubscription<WebAppPinStatusListener> subscribeIsWebAppPinned(
            String webAppId, WebAppPinStatusListener listener) {
        return (URLServiceSubscription<WebAppPinStatusListener>)isWebAppPinned(true, webAppId, listener);
    }

    /* Join a native/installed webOS app */
    public void joinApp(String appId, WebAppSession.LaunchListener listener) {
        LaunchSession launchSession = LaunchSession.launchSessionForAppId(appId);
        launchSession.setSessionType(LaunchSessionType.App);
        launchSession.setService(this);

        joinWebApp(launchSession, listener);
    }

    /* Connect to a native/installed webOS app */
    public void connectToApp(String appId, final WebAppSession.LaunchListener listener) {
        LaunchSession launchSession = LaunchSession.launchSessionForAppId(appId);
        launchSession.setSessionType(LaunchSessionType.App);
        launchSession.setService(this);

        final WebOSWebAppSession webAppSession = webAppSessionForLaunchSession(launchSession);

        connectToWebApp(webAppSession, false, new ResponseListener<Object>() {
            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
                Util.postSuccess(listener, webAppSession);
            }
        });
    }

    @Override
    public void joinWebApp(final LaunchSession webAppLaunchSession, final WebAppSession.LaunchListener listener) {
        final WebOSWebAppSession webAppSession = this.webAppSessionForLaunchSession(webAppLaunchSession);

        webAppSession.join(new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
                Util.postSuccess(listener, webAppSession);
            }
        });
    }

    @Override
    public void joinWebApp(String webAppId, WebAppSession.LaunchListener listener) {
        LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
        launchSession.setSessionType(LaunchSessionType.WebApp);
        launchSession.setService(this);

        joinWebApp(launchSession, listener);
    }

    private WebOSWebAppSession webAppSessionForLaunchSession(LaunchSession launchSession) {
        if (mWebAppSessions == null)
            mWebAppSessions = new ConcurrentHashMap<String, WebOSWebAppSession>();

        if (launchSession.getService() == null)
            launchSession.setService(this);

        WebOSWebAppSession webAppSession = mWebAppSessions.get(launchSession.getAppId());

        if (webAppSession == null) {
            webAppSession = new WebOSWebAppSession(launchSession, this);
            mWebAppSessions.put(launchSession.getAppId(), webAppSession);
        }

        return webAppSession;
    }

    @SuppressWarnings("unused")
    private void sendMessage(Object message, LaunchSession launchSession, ResponseListener<Object> listener) {
        if (launchSession == null || launchSession.getAppId() == null) {
            Util.postError(listener, new ServiceCommandError(0, "Must provide a valid LaunchSession object", null));
            return;
        }

        if (message == null) {
            Util.postError(listener, new ServiceCommandError(0, "Cannot send a null message", null));
            return;
        }

        if (socket == null) {
            connect();
        }

        String appId = launchSession.getAppId();
        String fullAppId = appId;

        if (launchSession.getSessionType() == LaunchSessionType.WebApp)
            fullAppId = mAppToAppIdMappings.get(appId);

        if (fullAppId == null || fullAppId.length() == 0) {
            Util.postError(listener, new ServiceCommandError(-1, "You must provide a valid LaunchSession to send messages to", null));

            return;
        }

        JSONObject payload = new JSONObject();

        try {
            payload.put("type", "p2p");
            payload.put("to", fullAppId);
            payload.put("payload", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, null, payload, true, listener);
        sendCommand(request);
    }

    public void sendMessage(String message, LaunchSession launchSession, ResponseListener<Object> listener) {
        if (message != null && message.length() > 0) {
            sendMessage((Object) message, launchSession, listener);
        }
        else {
            Util.postError(listener, new ServiceCommandError(0, "Cannot send a null message", null));
        }
    }

    public void sendMessage(JSONObject message, LaunchSession launchSession, ResponseListener<Object> listener) {
        if (message != null && message.length() > 0)
            sendMessage((Object) message, launchSession, listener);
        else
            Util.postError(listener, new ServiceCommandError(0, "Cannot send a null message", null));
    }


    /**************
    SYSTEM CONTROL
     **************/
    public void getServiceInfo(final ServiceInfoListener listener) {
        String uri = "ssap://api/getServiceList";

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                try {
                    JSONObject jsonObj = (JSONObject)response;
                    JSONArray services = (JSONArray) jsonObj.get("services");
                    Util.postSuccess(listener, services);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });

        request.send();
    }

    public void getSystemInfo(final SystemInfoListener listener) {
        String uri = "ssap://system/getSystemInfo";

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                try {
                    JSONObject jsonObj = (JSONObject)response;
                    JSONObject features = (JSONObject) jsonObj.get("features");
                    Util.postSuccess(listener, features);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });

        request.send();
    }

    public void secureAccessTest(final SecureAccessTestListener listener) {
        String uri = "ssap://com.webos.service.secondscreen.gateway/test/secure";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {

                try {
                    JSONObject jsonObj = (JSONObject) response;
                    boolean isSecure = (Boolean) jsonObj.get("returnValue");
                    Util.postSuccess(listener, isSecure);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
    }

    public void getACRAuthToken(final ACRAuthTokenListener listener) {
        String uri = "ssap://tv/getACRAuthToken";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {

                try {
                    JSONObject jsonObj = (JSONObject) response;
                    String authToken = (String) jsonObj.get("token");
                    Util.postSuccess(listener, authToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
    }

//    public void getLaunchPoints(final LaunchPointsListener listener) {
//        String uri = "ssap://com.webos.applicationManager/listLaunchPoints";
//
//        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
//
//            @Override
//            public void onSuccess(Object response) {
//
//                try {
//                    JSONObject jsonObj = (JSONObject) response;
//                    JSONArray launchPoints = (JSONArray) jsonObj.get("launchPoints");
//                    Util.postSuccess(listener, launchPoints);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onError(ServiceCommandError error) {
//                Util.postError(listener, error);
//            }
//        };
//
//        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
//        request.send();
//    }

    /******************
     PLAYLIST CONTROL
     *****************/
    @Override
    public PlaylistControl getPlaylistControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getPlaylistControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void jumpToTrack(long index, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void setPlayMode(PlayMode playMode, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void sendCommand(ServiceCommand<?> command) {
        if (socket != null)
            socket.sendCommand(command);
    }

    @Override
    public void unsubscribe(URLServiceSubscription<?> subscription) {
        if (socket != null)
            socket.unsubscribe(subscription);
    }

    @Override
    protected void updateCapabilities() {
        List<String> capabilities = new ArrayList<String>();

        Collections.addAll(capabilities, VolumeControl.Capabilities);
        Collections.addAll(capabilities, MediaPlayer.Capabilities);

        if (DiscoveryManager.getInstance().getPairingLevel().compareTo(PairingLevel.PROTECTED) >= 0) {
            Collections.addAll(capabilities, TextInputControl.Capabilities);
            Collections.addAll(capabilities, MouseControl.Capabilities);
            Collections.addAll(capabilities, KeyControl.Capabilities);
            Collections.addAll(capabilities, MediaPlayer.Capabilities);
            Collections.addAll(capabilities, Launcher.Capabilities);
            Collections.addAll(capabilities, TVControl.Capabilities);
            Collections.addAll(capabilities, ExternalInputControl.Capabilities);
            Collections.addAll(capabilities, ToastControl.Capabilities);
            capabilities.add(PowerControl.Off);
        } else {
            capabilities.add(Application);
            capabilities.add(Application_Params);
            capabilities.add(Application_Close);
            capabilities.add(Browser);
            capabilities.add(Browser_Params);
            capabilities.add(Hulu);
            capabilities.add(Netflix);
            capabilities.add(Netflix_Params);
            capabilities.add(YouTube);
            capabilities.add(YouTube_Params);
            capabilities.add(AppStore);
            capabilities.add(AppStore_Params);
            capabilities.add(AppState);
            capabilities.add(AppState_Subscribe);
        }

        if (serviceDescription != null) {
            if (serviceDescription.getVersion() != null
                    && (serviceDescription.getVersion().contains("4.0.0")
                    || serviceDescription.getVersion().contains("4.0.1"))) {
                capabilities.add(Launch);
                capabilities.add(Launch_Params);

                capabilities.add(Play);
                capabilities.add(Pause);
                capabilities.add(Stop);
                capabilities.add(Seek);
                capabilities.add(Position);
                capabilities.add(Duration);
                capabilities.add(PlayState);

                capabilities.add(WebAppLauncher.Close);

                if (getDLNAService() != null) {
                    capabilities.add(MediaPlayer.Subtitle_SRT);
                }
            } else {
                Collections.addAll(capabilities, WebAppLauncher.Capabilities);
                Collections.addAll(capabilities, MediaControl.Capabilities);

                capabilities.add(MediaPlayer.Subtitle_WebVTT);

                capabilities.add(PlaylistControl.JumpToTrack);
                capabilities.add(PlaylistControl.Next);
                capabilities.add(PlaylistControl.Previous);

                capabilities.add(MediaPlayer.Loop);
            }

            String locationXML = serviceDescription.getLocationXML();
            String appCasting = (locationXML != null) ? XmlUtil.findElement(locationXML, "appCasting") : null;
            String appCastingFeature = (locationXML != null) ? XmlUtil.findElement(locationXML, "supportAppcastingFeatures") : null;

            if (appCastingFeature != null) {
                // <supportAppcastingFeatures>mirroring|remote-camera</supportAppcastingFeatures>
                if (appCastingFeature.contains("mirroring")) capabilities.add(ScreenMirroringControl.ScreenMirroring);
                if (appCastingFeature.contains("remote-camera")) capabilities.add(RemoteCameraControl.RemoteCamera);
            } else if (appCasting != null) {
                // <appCasting>support</appCasting>
                if ("support".equals(appCasting)) capabilities.add(ScreenMirroringControl.ScreenMirroring);
            }
        }

        setCapabilities(capabilities);
    }

    public List<String> getPermissions() {
        if (permissions != null)
            return permissions;

        List<String> defaultPermissions = new ArrayList<String>();
        Collections.addAll(defaultPermissions, kWebOSTVServiceOpenPermissions);

        if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.PROTECTED) {
            Collections.addAll(defaultPermissions, kWebOSTVServiceProtectedPermissions);
        } else if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON) {
            Collections.addAll(defaultPermissions, kWebOSTVServiceProtectedPermissions);
            Collections.addAll(defaultPermissions, kWebOSTVServicePersonalActivityPermissions);
        }

        permissions = defaultPermissions;

        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;

        WebOSTVServiceConfig config = (WebOSTVServiceConfig) serviceConfig;

        if (config.getClientKey() != null) {
            config.setClientKey(null);

            if (isConnected()) {
                Log.w(Util.T, "Permissions changed -- you will need to re-pair to the TV.");
                disconnect();
            }
        }
    }

    private List<ExternalInputInfo> externalnputInfoFromJSONArray(JSONArray inputList) {
        List<ExternalInputInfo> externalInputInfoList = new ArrayList<ExternalInputInfo>();

        for (int i = 0; i < inputList.length(); i++) {
            try {
                JSONObject input = (JSONObject) inputList.get(i);

                String id = input.getString("id");
                String name = input.getString("label");
                boolean connected = input.getBoolean("connected");
                String iconURL = input.getString("icon");

                ExternalInputInfo inputInfo = new ExternalInputInfo();
                inputInfo.setRawData(input);
                inputInfo.setId(id);
                inputInfo.setName(name);
                inputInfo.setConnected(connected);
                inputInfo.setIconURL(iconURL);

                externalInputInfoList.add(inputInfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return externalInputInfoList;
    }

//    @Override
//    public LaunchSession decodeLaunchSession(String type, JSONObject obj) throws JSONException {
//        if ("webostv".equals(type)) {
//            LaunchSession launchSession = LaunchSession.launchSessionFromJSONObject(obj);
//            launchSession.setService(this);
//            return launchSession;
//        }
//        return null;
//    }

    @Override
    public void getPlayState(PlayStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return null;
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public void sendPairingKey(String pairingKey) {
        if (this.socket != null) {
            this.socket.sendPairingKey(pairingKey);
        }
    }

    public static interface ServiceInfoListener extends ResponseListener<JSONArray> { }

    public static interface SystemInfoListener extends ResponseListener<JSONObject> { }

    /**********************************************************************************************
     * SCREEN MIRRORING
     *********************************************************************************************/
    @Override
    public ScreenMirroringControl getScreenMirroringControl() {
        return this;
    }

    @Override
    public void startScreenMirroring(Context context, Intent projectionData, ScreenMirroringStartListener startListener) {
        ScreenMirroringApi.getInstance().startMirroring(context, projectionData, getServiceDescription().getIpAddress(), null, startListener);
    }

    @Override
    public void startScreenMirroring(Context context, Intent projectionData, Class secondScreenClass, ScreenMirroringStartListener startListener) {
        ScreenMirroringApi.getInstance().startMirroring(context, projectionData, getServiceDescription().getIpAddress(), secondScreenClass, startListener);
    }

    @Override
    public void stopScreenMirroring(Context context, ScreenMirroringStopListener stopListener) {
        ScreenMirroringApi.getInstance().stopMirroring(context, stopListener);
    }

    @Override
    public void setErrorListener(Context context, ScreenMirroringErrorListener errorListener) {
        ScreenMirroringApi.getInstance().setErrorListener(context, screenMirroringError -> {
            errorListener.onError(screenMirroringError);
        });
    }


    /**********************************************************************************************
     * REMOTE CAMERA
     *********************************************************************************************/
    @Override
    public RemoteCameraControl getRemoteCameraControl() {
        return this;
    }

    @Override
    public void startRemoteCamera(Context context, Surface previewSurface, boolean micMute, int lensFacing, RemoteCameraStartListener startListener) {
        RemoteCameraApi.getInstance().startRemoteCamera(context, previewSurface, getServiceDescription().getIpAddress(), micMute, lensFacing, startListener);
    }

    @Override
    public void stopRemoteCamera(Context context, RemoteCameraStopListener stopListener) {
        RemoteCameraApi.getInstance().stopRemoteCamera(context, stopListener);
    }

    @Override
    public void setMicMute(Context context, boolean micMute) {
        RemoteCameraApi.getInstance().setMicMute(context, micMute);
    }

    @Override
    public void setLensFacing(Context context, int lensFacing) {
        RemoteCameraApi.getInstance().setLensFacing(context, lensFacing);
    }

    @Override
    public void setCameraPlayingListener(Context context, RemoteCameraPlayingListener playingListener) {
        RemoteCameraApi.getInstance().setCameraPlayingListener(context, playingListener);
    }

    @Override
    public void setPropertyChangeListener(Context context, RemoteCameraPropertyChangeListener propertyChangeListener) {
        RemoteCameraApi.getInstance().setPropertyChangeListener(context, propertyChangeListener);
    }

    @Override
    public void setErrorListener(Context context, RemoteCameraErrorListener errorListener) {
        RemoteCameraApi.getInstance().setErrorListener(context, errorListener);
    }
}
