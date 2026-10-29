package com.vahanalerts.user;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Enter your name.")
    @Size(max = 100, message = "Name must be 100 characters or fewer.")
    private String name;

    @NotBlank(message = "Enter your phone number with its country code, such as +91.")
    @Size(max = 16, message = "Phone number must use the international format.")
    @Pattern(
            regexp = "\\+(?:91[6-9][0-9]{9}|(?!91)[1-9][0-9]{7,14})",
            message = "Enter a valid phone number with country code, such as +91 98765 43210.")
    private String phoneNumber;

    @NotBlank(message = "Create a password.")
    @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    @Size(max = 72, message = "Password confirmation must be 72 characters or fewer.")
    private String passwordConfirmation;

    @NotNull(message = "Enter your date of birth.")
    @PastOrPresent(message = "Date of birth cannot be in the future.")
    private LocalDate dateOfBirth;

    private boolean whatsappReminderConsent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = PhoneNumber.normalize(phoneNumber);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean isWhatsappReminderConsent() {
        return whatsappReminderConsent;
    }

    public void setWhatsappReminderConsent(boolean whatsappReminderConsent) {
        this.whatsappReminderConsent = whatsappReminderConsent;
    }
}
