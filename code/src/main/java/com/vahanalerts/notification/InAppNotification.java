package com.vahanalerts.notification;

import java.time.Instant;
import java.time.LocalDate;

import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.user.UserAccount;
import com.vahanalerts.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "in_app_notification")
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private VehicleDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "reminder_offset_days", nullable = false)
    private int reminderOffsetDays;

    @Column(name = "document_type_snapshot", nullable = false, length = 32)
    private String documentTypeSnapshot;

    @Column(name = "registration_number_snapshot", nullable = false, length = 20)
    private String registrationNumberSnapshot;

    @Column(name = "expiry_date_snapshot", nullable = false)
    private LocalDate expiryDateSnapshot;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected InAppNotification() {
    }

    public InAppNotification(
            UserAccount owner,
            Vehicle vehicle,
            VehicleDocument document,
            int reminderOffsetDays,
            LocalDate expiryDateSnapshot,
            String message) {
        this.owner = owner;
        this.vehicle = vehicle;
        this.document = document;
        this.type = NotificationType.DOCUMENT_EXPIRY;
        this.reminderOffsetDays = reminderOffsetDays;
        this.documentTypeSnapshot = document.getType().name();
        this.registrationNumberSnapshot = vehicle.getRegistrationNumber();
        this.expiryDateSnapshot = expiryDateSnapshot;
        this.message = message;
    }

    @PrePersist
    void initializeTimestamp() {
        createdAt = Instant.now();
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    public Long getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public VehicleDocument getDocument() {
        return document;
    }

    public NotificationType getType() {
        return type;
    }

    public int getReminderOffsetDays() {
        return reminderOffsetDays;
    }

    public String getDocumentTypeSnapshot() {
        return documentTypeSnapshot;
    }

    public String getRegistrationNumberSnapshot() {
        return registrationNumberSnapshot;
    }

    public LocalDate getExpiryDateSnapshot() {
        return expiryDateSnapshot;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }
}
