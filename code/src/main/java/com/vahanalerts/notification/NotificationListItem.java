package com.vahanalerts.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.vahanalerts.document.DocumentType;

public record NotificationListItem(
        Long id,
        NotificationType type,
        int reminderOffsetDays,
        String documentTypeSnapshot,
        String registrationNumberSnapshot,
        LocalDate expiryDateSnapshot,
        LocalDate currentDocumentExpiryDate,
        boolean documentExists,
        String message,
        Instant createdAt,
        Instant readAt,
        String documentUrl) {

    public String getDocumentTypeLabel() {
        return DocumentType.valueOf(documentTypeSnapshot).getLabel();
    }

    public boolean isExpiryDateChanged() {
        return documentExists && !Objects.equals(currentDocumentExpiryDate, expiryDateSnapshot);
    }

    public boolean isRead() {
        return readAt != null;
    }

    public String getReminderLabel() {
        return reminderOffsetDays == 0
                ? "Expiry day"
                : reminderOffsetDays + (reminderOffsetDays == 1 ? " day before" : " days before");
    }
}
