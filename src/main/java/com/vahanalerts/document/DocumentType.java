package com.vahanalerts.document;

public enum DocumentType {
    INSURANCE("Vehicle insurance", true),
    PUC("Pollution Under Control (PUC) certificate", true),
    REGISTRATION_CERTIFICATE("Registration Certificate (RC)", true),
    FITNESS_CERTIFICATE("Fitness Certificate", true),
    PERMIT("Permit", true),
    DRIVING_LICENCE("Driving Licence", true),
    OTHER("Other", false);

    private final String label;
    private final boolean expiryRequired;

    DocumentType(String label, boolean expiryRequired) {
        this.label = label;
        this.expiryRequired = expiryRequired;
    }

    public String getLabel() {
        return label;
    }

    public boolean isExpiryRequired() {
        return expiryRequired;
    }
}
