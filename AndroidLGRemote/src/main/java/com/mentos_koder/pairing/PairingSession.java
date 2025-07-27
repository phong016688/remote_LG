package com.mentos_koder.pairing;



import android.util.Log;

import com.mentos_koder.AndroidRemoteContext;
import com.mentos_koder.exception.PairingException;
import com.mentos_koder.ssl.DummyTrustManager;
import com.mentos_koder.ssl.KeyStoreManager;
import com.mentos_koder.util.Utils;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class PairingSession {

//    private final Logger logger = LoggerFactory.getLogger(PairingSession.class);

    private final BlockingQueue<Pairingmessage.PairingMessage> mMessagesQueue;

    private final PairingMessageManager mPairingMessageManager;

    SecretProvider secretProvider;

    private SSLSocket mSslSocket;

    public PairingSession() {
        mMessagesQueue = new LinkedBlockingDeque<>();
        mPairingMessageManager = new PairingMessageManager();
    }

    public void pair(String host, int port, PairingListener pairingListener) throws GeneralSecurityException, IOException, InterruptedException, PairingException {

        SSLContext sslContext = KeyStoreManager.createSSLContext();
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


        SSLSocket sSLSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
        mSslSocket = sSLSocket;
//        sSLSocket.setNeedClientAuth(true);
//        sSLSocket.setUseClientMode(true);
//        sSLSocket.setKeepAlive(true);
//        sSLSocket.setTcpNoDelay(true);
//        sSLSocket.startHandshake();

        pairingListener.onSessionCreated();
        PairingPacketParser pairingPacketParser = new PairingPacketParser(sSLSocket.getInputStream(), mMessagesQueue);
        pairingPacketParser.start();

        final OutputStream outputStream = sSLSocket.getOutputStream();

        byte[] pairingMessage = mPairingMessageManager.createPairingMessage(AndroidRemoteContext.getInstance().getClientName(), AndroidRemoteContext.getInstance().getServiceName());
        outputStream.write(pairingMessage);
        Pairingmessage.PairingMessage pairingMessageResponse = waitForMessage();
        logReceivedMessage(pairingMessageResponse.toString());

        byte[] pairingOption = new PairingMessageManager().createPairingOption();
        outputStream.write(pairingOption);
        Pairingmessage.PairingMessage pairingOptionAck = waitForMessage();
        logReceivedMessage(pairingOptionAck.toString());

        byte[] configMessage = new PairingMessageManager().createConfigMessage();
        outputStream.write(configMessage);
        Pairingmessage.PairingMessage pairingConfigAck = waitForMessage();
        logReceivedMessage(pairingConfigAck.toString());

        if (secretProvider != null)
            secretProvider.requestSecret(this);
        pairingListener.onSecretRequested();
//        logger.info("Waiting for secret");
        Pairingmessage.PairingMessage pairingSecretMessage = waitForMessage();
        byte[] secretMessage = mPairingMessageManager.createSecretMessage(pairingSecretMessage);
        outputStream.write(secretMessage);
        Pairingmessage.PairingMessage pairingSecretAck = waitForMessage();
        logReceivedMessage(pairingSecretAck.toString());

        pairingListener.onPaired();
        pairingListener.onSessionEnded();

    }


    Pairingmessage.PairingMessage waitForMessage() throws InterruptedException, PairingException {
        Pairingmessage.PairingMessage pairingMessage = mMessagesQueue.take();
        if (pairingMessage.getStatus() != Pairingmessage.PairingMessage.Status.STATUS_OK) {
            throw new PairingException(pairingMessage.toString());
        }
        return pairingMessage;
    }


    public void provideSecret(String secret) {
        createCodeSecret(secret);
    }

    private void createCodeSecret(String code) {
        code = code.substring(2);
        PairingChallengeResponse pairingChallengeResponse = new PairingChallengeResponse(Utils.getLocalCert(mSslSocket.getSession()), Utils.getPeerCert(mSslSocket.getSession()));
        byte[] secret = Utils.hexStringToBytes(code);
        System.out.println(Arrays.toString(secret));
        try {
            pairingChallengeResponse.checkGamma(secret);
        } catch (PairingException e) {
            throw new RuntimeException(e);
        }
        byte[] pairingChallengeResponseAlpha;
        try {
            pairingChallengeResponseAlpha = pairingChallengeResponse.getAlpha(secret);
        } catch (PairingException e) {
            throw new RuntimeException(e);
        }
        Pairingmessage.PairingMessage secretMessageProto = new PairingMessageManager().createSecretMessageProto(pairingChallengeResponseAlpha);
        try {
            mMessagesQueue.put(secretMessageProto);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

//    void logSendMessage(String message) {
//        logger.info("Send Message : {}", message);
//    }

    void logReceivedMessage(String message) {
//        logger.info("Received Message : {}", message);
        Log.d("AndroidRemoteTv", "Received Message: "+ message);
    }

}
