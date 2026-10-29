package com.vahanalerts.vehicle;

public class DuplicateVehicleRegistrationException extends RuntimeException {

    public DuplicateVehicleRegistrationException() {
        super("This registration number is already in your vehicles.");
    }
}
