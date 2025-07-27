package com.mentos_koder.exception;

public class PairingException extends Exception {
    public PairingException(String message) {
        super(message);
    }

    public PairingException(String message, Exception e) {
        super(message, e);
    }
}
