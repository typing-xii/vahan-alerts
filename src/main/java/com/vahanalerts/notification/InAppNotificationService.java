package com.vahanalerts.notification;

import java.time.Clock;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InAppNotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final Clock clock;

    public InAppNotificationService(InAppNotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NotificationListItem> findAllForOwner(Long ownerId) {
        return notificationRepository.findAllByOwner_IdOrderByCreatedAtDescIdDesc(ownerId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForOwner(Long ownerId) {
        return notificationRepository.countByOwner_IdAndReadAtIsNull(ownerId);
    }

    @Transactional
    public void markRead(Long notificationId, Long ownerId) {
        InAppNotification notification = notificationRepository.findByIdAndOwner_Id(notificationId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.markRead(clock.instant());
    }

    @Transactional
    public int markAllRead(Long ownerId) {
        return notificationRepository.markAllReadByOwner(ownerId, clock.instant());
    }

    private NotificationListItem toListItem(InAppNotification notification) {
        String documentUrl = null;
        if (notification.getDocument() != null && notification.getVehicle() != null) {
            documentUrl = "/vehicles/" + notification.getVehicle().getId()
                    + "/documents/" + notification.getDocument().getId();
        }
        return new NotificationListItem(
                notification.getId(),
                notification.getType(),
                notification.getReminderOffsetDays(),
                notification.getDocumentTypeSnapshot(),
                notification.getRegistrationNumberSnapshot(),
                notification.getExpiryDateSnapshot(),
                notification.getDocument() == null ? null : notification.getDocument().getExpiryDate(),
                notification.getDocument() != null,
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                documentUrl);
    }
}
