package com.mentos_koder;

import android.util.Log;

import com.mentos_koder.exception.PairingException;
import com.mentos_koder.remote.Remotemessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

public class Main {
    public static void main(String ipAddress) throws GeneralSecurityException, IOException, InterruptedException, PairingException {
        AndroidRemoteTv androidRemoteTv = new AndroidRemoteTv();
        androidRemoteTv.connect(ipAddress,new AndroidTvListener() {
            @Override
            public void onSessionCreated() {
                Log.d("AndroidRemoteTv", "onSessionCreated: ");
            }

            @Override
            public void onSecretRequested() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in));

                try {
                    String name = reader.readLine();
                    androidRemoteTv.sendSecret(name);
                    Log.d("AndroidRemoteTv", "onSecretRequested: "+ name);
                } catch (IOException e) {
                    Log.e("AndroidRemoteTv", "onSecretRequested: "+ e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onPaired() {
                Log.d("AndroidRemoteTv", "onPaired ");
            }

            @Override
            public void onConnectingToRemote() {
                Log.d("AndroidRemoteTv", "onConnectingToRemote ");
            }

            @Override
            public void onConnected() {
                System.out.println("Connected");
                Log.d("AndroidRemoteTv", "onConnected ");
                androidRemoteTv.sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_POWER, Remotemessage.RemoteDirection.SHORT);

            }

            @Override
            public void onDisconnect() {
                Log.d("AndroidRemoteTv", "onDisconnect ");
            }

            @Override
            public void onError(String error) {
                Log.e("AndroidRemoteTv", "onError "+ error);
            }
        });
    }
}
