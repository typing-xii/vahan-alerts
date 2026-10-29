package com.vahanalerts.notification;

import java.time.LocalDate;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.document.VehicleDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentExpiryReminderGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocumentExpiryReminderGenerator.class);
    private static final List<Integer> REMINDER_OFFSETS = List.of(30, 7, 1, 0);

    private final VehicleDocumentRepository documentRepository;
    private final DocumentExpiryReminderWriter reminderWriter;
    private final Clock clock;

    public DocumentExpiryReminderGenerator(
            VehicleDocumentRepository documentRepository,
            DocumentExpiryReminderWriter reminderWriter,
            Clock clock) {
        this.documentRepository = documentRepository;
        this.reminderWriter = reminderWriter;
        this.clock = clock;
    }

    public int generateDueReminders() {
        return generateForDate(LocalDate.now(clock));
    }

    public int generateForDate(LocalDate today) {
        int created = 0;
        for (VehicleDocument document : documentRepository.findAllByExpiryDateBetween(
                today, today.plusDays(30))) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(today, document.getExpiryDate());
            for (int offsetDays : REMINDER_OFFSETS) {
                if (daysUntilExpiry <= offsetDays) {
                    try {
                        if (reminderWriter.createIfAbsent(document, offsetDays)) {
                            created++;
                        }
                    } catch (RuntimeException exception) {
                        log.warn("Reminder generation failed for document id {} at offset {}.",
                                document.getId(), offsetDays, exception);
                    }
                }
            }
        }
        return created;
    }
}
