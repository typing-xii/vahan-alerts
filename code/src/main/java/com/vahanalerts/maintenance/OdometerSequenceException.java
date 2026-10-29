package com.vahanalerts.maintenance;

public class OdometerSequenceException extends RuntimeException {

    public OdometerSequenceException() {
        super("Odometer reading must not be lower than an earlier service or higher than a later service.");
    }
}
