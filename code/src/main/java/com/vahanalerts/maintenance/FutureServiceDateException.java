package com.vahanalerts.maintenance;

public class FutureServiceDateException extends RuntimeException {

    public FutureServiceDateException() {
        super("Service date cannot be in the future.");
    }
}
