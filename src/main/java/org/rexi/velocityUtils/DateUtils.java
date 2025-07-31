package org.rexi.velocityUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class DateUtils {

    private final ConfigManager configManager;

    public DateUtils(ConfigManager configManager) {
        this.configManager = new ConfigManager();
    }

    public LocalDate getStartOfWeek() {
        String startOfWeek = configManager.getString("stafftime.week_start");

        if (startOfWeek.isEmpty() || startOfWeek == null) {
            startOfWeek = "MONDAY"; // Default to Monday if not set
        } else {
            startOfWeek = startOfWeek.toUpperCase(); // Ensure the day is in uppercase
        }

        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(startOfWeek)));
    }

    public LocalDate getEndOfWeek() {
        String startOfWeek = configManager.getString("stafftime.week_start").toUpperCase();

        if (startOfWeek.isEmpty() || startOfWeek == null) {
            startOfWeek = "MONDAY"; // Default to Monday if not set
        } else {
            startOfWeek = startOfWeek.toUpperCase(); // Ensure the day is in uppercase
        }

        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.valueOf(startOfWeek))).plusDays(6);
    }

    public LocalDate getStartOfMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    public LocalDate getEndOfMonth() {
        return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }
}

