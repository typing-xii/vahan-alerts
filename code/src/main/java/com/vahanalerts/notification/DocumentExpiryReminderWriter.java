package com.vahanalerts.notification;

import java.time.LocalDate;

import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.vehicle.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentExpiryReminderWriter {

    private final InAppNotificationRepository notificationRepository;

    public DocumentExpiryReminderWriter(InAppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createIfAbsent(VehicleDocument document, int offsetDays) {
        LocalDate expiryDate = document.getExpiryDate();
        if (expiryDate == null || notificationRepository
                .existsByDocument_IdAndExpiryDateSnapshotAndReminderOffsetDays(
                        document.getId(), expiryDate, offsetDays)) {
            return false;
        }

        Vehicle vehicle = document.getVehicle();
        String message = buildMessage(document, vehicle, expiryDate, offsetDays);
        notificationRepository.saveAndFlush(new InAppNotification(
                vehicle.getOwner(), vehicle, document, offsetDays, expiryDate, message));
        return true;
    }

    private String buildMessage(
            VehicleDocument document,
            Vehicle vehicle,
            LocalDate expiryDate,
            int offsetDays) {
        String timing = switch (offsetDays) {
            case 30 -> "expires in 30 days";
            case 7 -> "expires in 7 days";
            case 1 -> "expires tomorrow";
            case 0 -> "expires today";
            default -> throw new IllegalArgumentException("Unsupported document expiry reminder offset");
        };
        return document.getType().getLabel() + " for vehicle "
                + vehicle.getRegistrationNumber() + " " + timing + " (" + expiryDate + ").";
    }
}
