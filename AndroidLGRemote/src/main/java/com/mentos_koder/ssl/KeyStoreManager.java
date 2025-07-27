package com.mentos_koder.ssl;

import android.util.Log;

import com.mentos_koder.AndroidRemoteContext;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

public final class KeyStoreManager {
    private static final String TAG = "KeyStoreManager";
    private DynamicTrustManager mDynamicTrustManager;

    private AndroidRemoteContext androidRemoteContext = AndroidRemoteContext.getInstance();

    public KeyStoreManager() {
        this.mDynamicTrustManager = new DynamicTrustManager();
    }

    private static final String getCertificateName() {
        return getCertificateName(getUniqueId());
    }

    private static final String getCertificateName(String str) {
        return "CN=androidtv/livingTV";
    }

    private static String getSubjectDN(Certificate certificate) {
        X500Principal subjectX500Principal;
        if (!(certificate instanceof X509Certificate) || (subjectX500Principal = ((X509Certificate) certificate).getSubjectX500Principal()) == null) {
            return null;
        }
        return subjectX500Principal.getName();
    }

    private static final String getUniqueId() {
        return UUID.randomUUID().toString();
    }

//    public KeyManager[] getKeyManagers() throws GeneralSecurityException {
//        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//        keyManagerFactory.init(null, null);
//        return keyManagerFactory.getKeyManagers();
//    }
    public static SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, null);
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException |
                 UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        return sslContext;
    }
    public TrustManager[] getTrustManagers() throws GeneralSecurityException {
        try {
            return new DynamicTrustManager[]{this.mDynamicTrustManager};
        } catch (Exception e) {
            throw new GeneralSecurityException(e);
        }
    }

    private static class DynamicTrustManager implements javax.net.ssl.X509TrustManager {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] x509CertificateArr, String str) throws java.security.cert.CertificateException {
            // Your implementation here
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] x509CertificateArr, String str) throws java.security.cert.CertificateException {
            // Your implementation here
        }
    }
}
