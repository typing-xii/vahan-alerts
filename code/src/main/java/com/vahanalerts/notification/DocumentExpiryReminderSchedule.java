package com.vahanalerts.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "vahan.reminders.document-expiry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DocumentExpiryReminderSchedule {

    private static final Logger log = LoggerFactory.getLogger(DocumentExpiryReminderSchedule.class);

    private final DocumentExpiryReminderGenerator reminderGenerator;

    public DocumentExpiryReminderSchedule(DocumentExpiryReminderGenerator reminderGenerator) {
        this.reminderGenerator = reminderGenerator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissedRemindersAtStartup() {
        generateDueReminders();
    }

    @Scheduled(
            cron = "${vahan.reminders.document-expiry.cron:0 0 9 * * *}",
            zone = "${vahan.reminders.time-zone:Asia/Kolkata}")
    public void generateDailyReminders() {
        generateDueReminders();
    }

    private void generateDueReminders() {
        try {
            int created = reminderGenerator.generateDueReminders();
            log.info("Document expiry reminder run completed; {} in-app reminder(s) created.", created);
        } catch (RuntimeException exception) {
            log.error("Document expiry reminder run failed.", exception);
        }
    }
}
