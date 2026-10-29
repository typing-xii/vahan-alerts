package com.vahanalerts.user;

import java.util.regex.Pattern;

public final class PhoneNumber {

    private static final Pattern FORMATTING = Pattern.compile("[\\s().-]");

    private PhoneNumber() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String normalized = FORMATTING.matcher(phoneNumber.trim()).replaceAll("");
        if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }
        return normalized;
    }
}
