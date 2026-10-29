package com.vahanalerts.document;

public enum DocumentExpiryStatus {
    EXPIRED("Expired"),
    EXPIRING_SOON("Expiring Soon"),
    VALID("Valid"),
    NO_EXPIRY_DATE("No Expiry Date");

    private final String label;

    DocumentExpiryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
