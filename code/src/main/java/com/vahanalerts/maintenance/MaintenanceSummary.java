package com.vahanalerts.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceSummary(long serviceCount, BigDecimal totalCostInr, LocalDate mostRecentServiceDate) {
}
