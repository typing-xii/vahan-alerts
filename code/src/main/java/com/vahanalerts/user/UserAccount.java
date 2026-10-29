package com.vahanalerts.user;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 254)
    private String email;

    @Column(name = "phone_number", length = 16, unique = true)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "whatsapp_reminder_consent", nullable = false)
    private boolean whatsappReminderConsent;

    @Column(name = "whatsapp_consent_at")
    private Instant whatsappConsentAt;

    @Column(name = "whatsapp_consent_withdrawn_at")
    private Instant whatsappConsentWithdrawnAt;

    @Column(name = "whatsapp_consent_version", length = 32)
    private String whatsappConsentVersion;

    protected UserAccount() {
    }

    public UserAccount(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public UserAccount(
            String name,
            String phoneNumber,
            LocalDate dateOfBirth,
            String passwordHash,
            boolean whatsappReminderConsent) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.whatsappReminderConsent = whatsappReminderConsent;
        if (whatsappReminderConsent) {
            this.whatsappConsentAt = this.createdAt;
            this.whatsappConsentVersion = "whatsapp-reminders-v1";
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isWhatsappReminderConsent() {
        return whatsappReminderConsent;
    }

    public Instant getWhatsappConsentAt() {
        return whatsappConsentAt;
    }

    public Instant getWhatsappConsentWithdrawnAt() {
        return whatsappConsentWithdrawnAt;
    }

    public String getWhatsappConsentVersion() {
        return whatsappConsentVersion;
    }

    public void withdrawWhatsappReminderConsent(Instant withdrawnAt) {
        if (whatsappReminderConsent) {
            whatsappReminderConsent = false;
            whatsappConsentWithdrawnAt = withdrawnAt;
        }
    }
}
