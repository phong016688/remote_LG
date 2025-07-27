package com.mentos_koder;


import android.util.Log;

import com.mentos_koder.exception.PairingException;
import com.mentos_koder.pairing.PairingListener;
import com.mentos_koder.pairing.PairingSession;
import com.mentos_koder.remote.RemoteSession;
import com.mentos_koder.remote.Remotemessage;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;


public class AndroidRemoteTv extends BaseAndroidRemoteTv {

//    private final Logger logger = LoggerFactory.getLogger(AndroidRemoteTv.class);

    private PairingSession mPairingSession;

    private RemoteSession mRemoteSession;

    public void connect(String ip, AndroidTvListener androidTvListener) throws GeneralSecurityException, IOException, InterruptedException, PairingException {
        mRemoteSession = new RemoteSession(ip, 6466, new RemoteSession.RemoteSessionListener() {
            @Override
            public void onConnected() {
                androidTvListener.onConnected();
            }

            @Override
            public void onSslError()  {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onError(String message) {

            }
        });

        if (AndroidRemoteContext.getInstance().getKeyStoreFile().exists())
            mRemoteSession.connect();
        else {
            mPairingSession = new PairingSession();
            mPairingSession.pair(ip, 6467, new PairingListener() {
                @Override
                public void onSessionCreated() {
                    Log.d("AndroidRemoteTv", "onSessionCreated: ");
                }

                @Override
                public void onPerformInputDeviceRole() {
                    Log.d("AndroidRemoteTv", "onPerformInputDeviceRole: ");
                }

                @Override
                public void onPerformOutputDeviceRole(byte[] gamma) {
                    Log.d("AndroidRemoteTv", "onPerformOutputDeviceRole: " + gamma);
                }

                @Override
                public void onSecretRequested() {
                    androidTvListener.onSecretRequested();
                }

                @Override
                public void onSessionEnded() {
                    Log.d("AndroidRemoteTv", "onSessionEnded: " );
                }

                @Override
                public void onError(String message) {
                    Log.e("AndroidRemoteTv", "onError: " + message );
                }

                @Override
                public void onPaired() {
                    try {
                        mRemoteSession.connect();
                    } catch (GeneralSecurityException | IOException | InterruptedException |
                             PairingException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onLog(String message) {
                    Log.e("AndroidRemoteTv", "onLog: " + message );
                }
            });
        }

    }

    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        mRemoteSession.sendCommand(remoteKeyCode, remoteDirection);
    }

    public void sendSecret(String code) {
        mPairingSession.provideSecret(code);
    }

}
