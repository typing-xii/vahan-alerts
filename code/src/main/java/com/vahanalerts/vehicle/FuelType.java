package com.vahanalerts.vehicle;

public enum FuelType {
    PETROL("Petrol"),
    DIESEL("Diesel"),
    CNG("CNG"),
    ELECTRIC("Electric"),
    HYBRID("Hybrid"),
    OTHER("Other");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
