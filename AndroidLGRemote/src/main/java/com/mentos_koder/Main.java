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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onPaired() {
            }

            @Override
            public void onConnectingToRemote() {
            }

            @Override
            public void onConnected() {
                androidRemoteTv.sendCommand(Remotemessage.RemoteKeyCode.KEYCODE_POWER, Remotemessage.RemoteDirection.SHORT);

            }

            @Override
            public void onDisconnect() {
            }

            @Override
            public void onError(String error) {
            }
        });
    }
}
