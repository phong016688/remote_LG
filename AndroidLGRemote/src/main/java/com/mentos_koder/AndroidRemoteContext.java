package com.mentos_koder;

import java.io.File;
import java.nio.file.Paths;

public class AndroidRemoteContext {

    private String serviceName = "com.github.kunal52/androidTvRemote";

    private String clientName = "androidTvRemoteJava";

    private File keyStoreFile = Paths.get("androidtv.keystore").toFile();

    private char[] keyStorePass = "KeyStore_Password".toCharArray();
    ;

    private static volatile AndroidRemoteContext instance;


    private AndroidRemoteContext() {

    }

    public static AndroidRemoteContext getInstance() {
        AndroidRemoteContext result = instance;
        if (result != null) {
            return result;
        }
        synchronized (AndroidRemoteContext.class) {
            if (instance == null) {
                instance = new AndroidRemoteContext();
            }
            return instance;
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public File getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(File keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public char[] getKeyStorePass() {
        return keyStorePass;
    }

    public void setKeyStorePass(char[] keyStorePass) {
        this.keyStorePass = keyStorePass;
    }
}
