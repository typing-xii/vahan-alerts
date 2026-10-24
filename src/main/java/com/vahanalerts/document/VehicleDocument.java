package com.vahanalerts.document;

import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_document")
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 32)
    private DocumentType type;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 1000)
    private String notes;

    @Column(name = "attachment_key", length = 50)
    private String attachmentKey;

    @Column(name = "attachment_media_type", length = 32)
    private String attachmentMediaType;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VehicleDocument() {
    }

    public VehicleDocument(Vehicle vehicle, DocumentForm form) {
        this.vehicle = vehicle;
        updateFrom(form);
    }

    public void updateFrom(DocumentForm form) {
        this.type = form.getType();
        this.documentNumber = trimToNull(form.getDocumentNumber());
        this.issueDate = form.getIssueDate();
        this.expiryDate = form.getExpiryDate();
        this.notes = trimToNull(form.getNotes());
    }

    public void setAttachment(String key, String mediaType, long size) {
        this.attachmentKey = key;
        this.attachmentMediaType = mediaType;
        this.attachmentSize = size;
    }

    public void removeAttachment() {
        this.attachmentKey = null;
        this.attachmentMediaType = null;
        this.attachmentSize = null;
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public DocumentType getType() {
        return type;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public String getAttachmentKey() {
        return attachmentKey;
    }

    public String getAttachmentMediaType() {
        return attachmentMediaType;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
