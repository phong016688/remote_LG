package com.connectsdk.service.upnp;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.URLServiceSubscription;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DLNAHttpServer {
    final int port = 5555; // đổi post 5555

    volatile ServerSocket welcomeSocket;

    volatile boolean running = false;

    CopyOnWriteArrayList<URLServiceSubscription<?>> subscriptions;

    public DLNAHttpServer() {
        subscriptions = new CopyOnWriteArrayList<URLServiceSubscription<?>>();
    }

    public synchronized void start(String imagePath,Context context) {
        Log.d("#DLNA", "run: start ");
        if (running) {
            return;
        }

        running = true;

        try {
            welcomeSocket = new ServerSocket(this.port);
            Log.d("#DLNA", "run: " + welcomeSocket.getInetAddress());
            Log.d("#DLNA", "Server is running on port " + port);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                //processRequests(imagePath,context);
                processR(imagePath,context);
            }
        }, true);
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }

        for (URLServiceSubscription<?> sub : subscriptions) {
            Log.d("#DLNA", "stop: " + sub);
            sub.unsubscribe();
        }
        subscriptions.clear();

        if (welcomeSocket != null && !welcomeSocket.isClosed()) {
            try {
                welcomeSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        welcomeSocket = null;
        running = false;
    }
    public void processR(String imagePath, Context context) {
        Log.d("#DLNA", "processRequests running: " + running);
        while (running) {
            if (welcomeSocket == null || welcomeSocket.isClosed()) {
                break;
            }
            try {
                Socket connectionSocket = welcomeSocket.accept();
                Log.i("#DLNA", "Accepted : " + connectionSocket.toString());
                new Thread(() -> processConnection(connectionSocket, imagePath, context)).start();
            } catch (IOException ex) {
                Log.e("#DLNA", "IOException occurred while accepting connection: " + ex.toString());
                break;
            }

        }
    }

    private void processConnection(Socket connectionSocket, String imagePath, Context context) {
        BufferedReader inFromClient = null;
        DataOutputStream outToClient = null;
        PrintWriter out = null;

        try {
            inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = inFromClient.read()) != -1) {
                sb.append((char) c);
                if (sb.toString().endsWith("\r\n\r\n"))
                    break;
            }
            String request = sb.toString();
            Log.d("#DLNA", "Request received: " + request);

            String rangeHeader = null;
            String[] requestLines = request.split("\r\n");
            for (String line : requestLines) {
                if (line.startsWith("Range:")) {
                    rangeHeader = line;
                    break;
                }
            }

            outToClient = new DataOutputStream(new BufferedOutputStream(connectionSocket.getOutputStream()));
            out = new PrintWriter(outToClient, true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                byte[] imageData = Files.readAllBytes(Paths.get(imagePath));
                int lastDotIndex = imagePath.lastIndexOf('.');
                if (!connectionSocket.isClosed() && connectionSocket.isConnected()) {
                    String fileExtension = "";
                    if (lastDotIndex > 0) {
                        fileExtension = imagePath.substring(lastDotIndex + 1).toLowerCase();
                    }
                    String contentType = getContentType(fileExtension);

                    if (rangeHeader != null) {
                        handleRangeRequest(rangeHeader, imageData, outToClient, out, contentType);
                    } else {
                        sendFullResponse(imageData, outToClient, out, contentType);
                    }
                } else {
                    sendNotFoundResponse(out);
                }
            } else {
                sendNotFoundResponse(out);
            }
        } catch (IOException ex) {
            Log.e("#DLNA", "IOException: " + ex.getMessage());
        } finally {
            closeResources(inFromClient, out, outToClient, connectionSocket);
        }
    }
    private void handleRangeRequest(String rangeHeader, byte[] imageData, DataOutputStream outToClient, PrintWriter out, String contentType) throws IOException {
        String rangeValue = rangeHeader.split("=")[1];
        long rangeStart = Long.parseLong(rangeValue.split("-")[0]);
        long rangeEnd = imageData.length - 1;
        if (rangeValue.contains("-") && !rangeValue.endsWith("-")) {
            rangeEnd = Long.parseLong(rangeValue.split("-")[1]);
        }
        long contentLength = rangeEnd - rangeStart + 1;

        out.println("HTTP/1.1 206 Partial Content");
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + contentLength);
        out.println("Content-Range: bytes " + rangeStart + "-" + rangeEnd + "/" + imageData.length);
        out.println();
        out.flush();

        outToClient.write(imageData, (int) rangeStart, (int) contentLength);
        outToClient.flush();
    }
    private void sendFullResponse(byte[] imageData, DataOutputStream outToClient, PrintWriter out, String contentType) throws IOException {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + imageData.length);
        out.println();
        out.flush();
        outToClient.write(imageData);
        outToClient.flush();
    }
    private String getContentType(String fileExtension) {
        switch (fileExtension.toLowerCase()) {
            case "jpg":
            case "jpeg":
            case "png":
                return "image/jpeg";
            case "mp4":
            case "avi":
            case "mkv":
                return "video/mp4";
            case "mp3":
            case "wav":
            case "flac":
            case "m4a":
                return  "audio/mp3";
            default:
                return "application/octet-stream";
        }
    }
    private void sendNotFoundResponse(PrintWriter out) {
        out.println("HTTP/1.1 404 Not Found");
        out.println();
        out.flush();
    }

    private void closeResources(BufferedReader inFromClient, PrintWriter out, DataOutputStream outToClient, Socket connectionSocket) {
        try {
            if (inFromClient != null) inFromClient.close();
            if (out != null) out.close();
            if (outToClient != null) outToClient.close();
            if (connectionSocket != null && !connectionSocket.isClosed()) connectionSocket.close();
        } catch (IOException ex) {
            Log.e("#DLNA", "IOException while closing resources: " + ex.getMessage());
        }
    }
    public static String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();

        String ipAddressString = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

        return ipAddressString;
    }

    public static String getServerAddress(Context context, int port,String path) {
        String ipAddress = getLocalIpAddress(context);
        return "http://" + ipAddress + ":" + port + path;
    }

    private void handleLastChange(JSONObject lastChange) throws JSONException {
        Log.d("#DLNA", "handleLastChange: " + lastChange);
        if (lastChange.has("InstanceID")) {
            JSONArray instanceIDs = lastChange.getJSONArray("InstanceID");

            for (int i = 0; i < instanceIDs.length(); i++) {
                JSONArray events = instanceIDs.getJSONArray(i);

                for (int j = 0; j < events.length(); j++) {
                    JSONObject entry = events.getJSONObject(j);
                    handleEntry(entry);
                }
            }
        }
    }

    private void handleEntry(JSONObject entry) throws JSONException {
        Log.d("#DLNA", "handleEntry: " + entry);
        if (entry.has("TransportState")) {
            String transportState = entry.getString("TransportState");
            PlayStateStatus status = PlayStateStatus.convertTransportStateToPlayStateStatus(transportState);

            for (URLServiceSubscription<?> sub: subscriptions) {
                if (sub.getTarget().equalsIgnoreCase("playState")) {
                    for (int j = 0; j < sub.getListeners().size(); j++) {
                        @SuppressWarnings("unchecked")
                        ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(j);
                        Util.postSuccess(listener, status);
                    }
                }
            }
        }

        if ((entry.has("Volume")&&!entry.has("channel"))||(entry.has("Volume")&&entry.getString("channel").equals("Master"))) {
            int intVolume = entry.getInt("Volume");
            float volume = (float) intVolume / 100;

            for (URLServiceSubscription<?> sub : subscriptions) {
                if (sub.getTarget().equalsIgnoreCase("volume")) {
                    for (int j = 0; j < sub.getListeners().size(); j++) {
                        @SuppressWarnings("unchecked")
                        ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(j);
                        Util.postSuccess(listener, volume);
                    }
                }
            }
        }

        if ((entry.has("Mute")&&!entry.has("channel"))||(entry.has("Mute")&&entry.getString("channel").equals("Master"))) {
            String muteStatus = entry.getString("Mute");
            boolean mute;

            try {
                mute = (Integer.parseInt(muteStatus) == 1);
            } catch(NumberFormatException e) {
                mute = Boolean.parseBoolean(muteStatus);
            }

            for (URLServiceSubscription<?> sub : subscriptions) {
                if (sub.getTarget().equalsIgnoreCase("mute")) {
                    for (int j = 0; j < sub.getListeners().size(); j++) {
                        @SuppressWarnings("unchecked")
                        ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(j);
                        Util.postSuccess(listener, mute);
                    }
                }
            }
        }

        if (entry.has("CurrentTrackMetaData")) {

            String trackMetaData = entry.getString("CurrentTrackMetaData");

            MediaInfo info = DLNAMediaInfoParser.getMediaInfo(trackMetaData);

            for (URLServiceSubscription<?> sub : subscriptions) {
                if (sub.getTarget().equalsIgnoreCase("info")) {
                    for (int j = 0; j < sub.getListeners().size(); j++) {
                        @SuppressWarnings("unchecked")
                        ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(j);
                        Util.postSuccess(listener, info);
                    }
                }
            }

        }

    }

    public int getPort() {
        return port;
    }

    public List<URLServiceSubscription<?>> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<URLServiceSubscription<?>> subscriptions) {
        this.subscriptions = new CopyOnWriteArrayList<URLServiceSubscription<?>>(subscriptions);
    }

    public boolean isRunning() {
        return running;
    }
}
