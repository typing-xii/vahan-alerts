package com.vahanalerts.maintenance;

public enum ServiceType {
    GENERAL_SERVICE("General Service"),
    ENGINE_OIL_CHANGE("Engine Oil Change"),
    BRAKE_SERVICE("Brake Service"),
    TYRE_REPLACEMENT("Tyre Replacement"),
    BATTERY_REPLACEMENT("Battery Replacement"),
    REPAIRS("Repairs"),
    OTHER("Other");

    private final String label;

    ServiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
