package com.vahanalerts.vehicle;

public enum VehicleType {
    CAR("Car"),
    MOTORCYCLE("Motorcycle"),
    SCOOTER("Scooter"),
    OTHER("Other");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
