package com.vahanalerts.notification;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    @EntityGraph(attributePaths = {"document", "vehicle"})
    List<InAppNotification> findAllByOwner_IdOrderByCreatedAtDescIdDesc(Long ownerId);

    long countByOwner_IdAndReadAtIsNull(Long ownerId);

    boolean existsByDocument_IdAndExpiryDateSnapshotAndReminderOffsetDays(
            Long documentId,
            LocalDate expiryDate,
            int reminderOffsetDays);

    Optional<InAppNotification> findByIdAndOwner_Id(Long id, Long ownerId);

    @Modifying
    @Query("""
            update InAppNotification notification
            set notification.readAt = :readAt
            where notification.owner.id = :ownerId
              and notification.readAt is null
            """)
    int markAllReadByOwner(@Param("ownerId") Long ownerId, @Param("readAt") Instant readAt);

}
