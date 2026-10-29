package com.vahanalerts.document;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class DocumentExpiryStatusCalculator {

    private final Clock clock;

    public DocumentExpiryStatusCalculator(Clock clock) {
        this.clock = clock;
    }

    public DocumentExpiryStatus calculate(LocalDate expiryDate) {
        if (expiryDate == null) {
            return DocumentExpiryStatus.NO_EXPIRY_DATE;
        }

        LocalDate today = today();
        if (expiryDate.isBefore(today)) {
            return DocumentExpiryStatus.EXPIRED;
        }
        if (!expiryDate.isAfter(today.plusDays(30))) {
            return DocumentExpiryStatus.EXPIRING_SOON;
        }
        return DocumentExpiryStatus.VALID;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
