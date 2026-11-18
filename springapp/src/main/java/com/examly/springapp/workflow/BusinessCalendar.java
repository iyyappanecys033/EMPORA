package com.examly.springapp.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business Calendar providing timezone-aware working hours and holiday calculations
 * for SLA deadline projection, dispute windows, and automatic ticket closure.
 * Baseline: Asia/Kolkata timezone, Monday-Friday 09:00-18:00 (9 hrs/day).
 * Supports externalized multi-year holidays via epams.calendar.holidays (comma-separated ISO dates).
 */
@Component
public class BusinessCalendar {

    private static final Logger log = LoggerFactory.getLogger(BusinessCalendar.class);

    public static final ZoneId EPAMS_TIMEZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalTime WORK_START = LocalTime.of(9, 0);
    public static final LocalTime WORK_END = LocalTime.of(18, 0);
    public static final int DISPUTE_WINDOW_BUSINESS_HOURS = 48;

    private final Clock clock;
    private final Set<LocalDate> configuredHolidays;

    @Autowired
    public BusinessCalendar(Clock clock,
                            @Value("${epams.calendar.holidays:}") String externalHolidaysCsv,
                            @Value("${epams.calendar.include-default-2026:true}") boolean includeDefault2026) {
        this.clock = clock;
        this.configuredHolidays = ConcurrentHashMap.newKeySet();
        if (includeDefault2026) {
            // Existing baseline 2026 gazetted corporate holidays
            this.configuredHolidays.add(LocalDate.of(2026, 1, 26));  // Republic Day
            this.configuredHolidays.add(LocalDate.of(2026, 8, 15));  // Independence Day
            this.configuredHolidays.add(LocalDate.of(2026, 10, 2));  // Gandhi Jayanti
            this.configuredHolidays.add(LocalDate.of(2026, 12, 25)); // Christmas
        }
        loadExternalHolidays(externalHolidaysCsv);
    }

    public BusinessCalendar(Clock clock) {
        this(clock, "", true);
    }

    public BusinessCalendar() {
        this(Clock.system(EPAMS_TIMEZONE));
    }

    /**
     * Parses comma-separated ISO-8601 dates (YYYY-MM-DD) for future-year corporate holiday management
     * without hardcoding unverified national/company holidays. Duplicate dates are safely deduplicated.
     */
    public final void loadExternalHolidays(String csvDates) {
        if (csvDates == null || csvDates.isBlank()) {
            return;
        }
        for (String token : csvDates.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                this.configuredHolidays.add(LocalDate.parse(trimmed));
            } catch (DateTimeParseException ex) {
                log.warn("Ignoring malformed holiday date entry in epams.calendar.holidays: '{}'", trimmed);
            }
        }
    }

    public void addHoliday(LocalDate holiday) {
        if (holiday != null) {
            this.configuredHolidays.add(holiday);
        }
    }

    public void addHolidays(Collection<LocalDate> holidays) {
        if (holidays != null) {
            for (LocalDate d : holidays) {
                addHoliday(d);
            }
        }
    }

    public boolean removeHoliday(LocalDate holiday) {
        return holiday != null && this.configuredHolidays.remove(holiday);
    }

    public Set<LocalDate> getConfiguredHolidays() {
        return Collections.unmodifiableSet(configuredHolidays);
    }

    public boolean isBusinessDay(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !configuredHolidays.contains(date);
    }

    public boolean isWithinBusinessHours(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        if (!isBusinessDay(dateTime.toLocalDate())) return false;
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(WORK_START) && time.isBefore(WORK_END);
    }

    /**
     * Returns the current LocalDateTime normalized to the authoritative EPAMS timezone (Asia/Kolkata).
     */
    public LocalDateTime nowInBusinessZone() {
        return nowInBusinessZone(this.clock);
    }

    /**
     * Returns the current LocalDateTime for the provided Clock, normalized to Asia/Kolkata.
     */
    public LocalDateTime nowInBusinessZone(Clock evaluationClock) {
        Clock effectiveClock = evaluationClock != null ? evaluationClock : this.clock;
        return LocalDateTime.now(effectiveClock.withZone(EPAMS_TIMEZONE));
    }

    /**
     * Calculates the exact future deadline given starting timestamp and target business hours
     * using exact Duration arithmetic (preserving sub-minute precision) across working days
     * (Monday-Friday, 09:00-18:00 Asia/Kolkata, excluding weekends and configured holidays).
     */
    public LocalDateTime calculateTargetDate(LocalDateTime start, int targetHours) {
        if (start == null) {
            start = nowInBusinessZone();
        }
        if (targetHours <= 0) {
            return start;
        }

        LocalDateTime current = normalizeToBusinessHours(start);
        Duration remaining = Duration.ofHours(targetHours);

        while (!remaining.isZero()) {
            if (!isBusinessDay(current.toLocalDate())) {
                current = current.plusDays(1).with(WORK_START);
                continue;
            }

            LocalTime currentTime = current.toLocalTime();
            if (currentTime.isBefore(WORK_START)) {
                current = current.with(WORK_START);
                currentTime = WORK_START;
            }

            if (!currentTime.isBefore(WORK_END)) {
                current = current.plusDays(1).with(WORK_START);
                continue;
            }

            Duration untilEndOfDay = Duration.between(currentTime, WORK_END);

            if (remaining.compareTo(untilEndOfDay) <= 0) {
                current = current.plus(remaining);
                remaining = Duration.ZERO;
            } else {
                remaining = remaining.minus(untilEndOfDay);
                current = current.plusDays(1).with(WORK_START);
            }
        }

        return current;
    }

    private LocalDateTime normalizeToBusinessHours(LocalDateTime timestamp) {
        LocalDateTime current = timestamp;
        if (!isBusinessDay(current.toLocalDate()) || current.toLocalTime().isBefore(WORK_START)) {
            while (!isBusinessDay(current.toLocalDate())) {
                current = current.plusDays(1).with(WORK_START);
            }
            if (current.toLocalTime().isBefore(WORK_START)) {
                current = current.with(WORK_START);
            }
        } else if (!current.toLocalTime().isBefore(WORK_END)) {
            current = current.plusDays(1).with(WORK_START);
            while (!isBusinessDay(current.toLocalDate())) {
                current = current.plusDays(1).with(WORK_START);
            }
        }
        return current;
    }

    /**
     * Calculates the cumulative elapsed business minutes between two timestamps.
     */
    public long calculateBusinessMinutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }

        long totalMinutes = 0;
        LocalDateTime current = start;

        while (current.isBefore(end)) {
            if (isBusinessDay(current.toLocalDate())) {
                LocalTime time = current.toLocalTime();
                if (!time.isBefore(WORK_START) && time.isBefore(WORK_END)) {
                    totalMinutes++;
                }
            }
            current = current.plusMinutes(1);
        }

        return totalMinutes;
    }

    /**
     * Calculates the authoritative 48-business-hour dispute/reopen deadline (BR-003).
     */
    public LocalDateTime calculateReopenDeadline(LocalDateTime resolvedAt) {
        if (resolvedAt == null) {
            return null;
        }
        return calculateTargetDate(resolvedAt, DISPUTE_WINDOW_BUSINESS_HOURS);
    }

    /**
     * Evaluates whether a reopen request at the current business time is within the
     * 48-business-hour dispute window. The exact deadline instant is INCLUSIVE (now <= deadline).
     */
    public boolean isWithinReopenWindow(LocalDateTime resolvedAt) {
        return isWithinReopenWindow(resolvedAt, nowInBusinessZone());
    }

    /**
     * Evaluates whether a reopen request at evaluationTime is within the 48-business-hour
     * dispute window. The exact deadline instant is INCLUSIVE (evaluationTime <= deadline).
     */
    public boolean isWithinReopenWindow(LocalDateTime resolvedAt, LocalDateTime evaluationTime) {
        if (resolvedAt == null || evaluationTime == null) {
            return false;
        }
        LocalDateTime deadline = calculateReopenDeadline(resolvedAt);
        return !evaluationTime.isAfter(deadline);
    }

    /**
     * Returns true strictly AFTER the 48-business-hour dispute window has elapsed
     * (now > deadline; exact deadline instant remains open for reopening).
     */
    public boolean isDisputeWindowExpired(LocalDateTime resolvedAt) {
        return isDisputeWindowExpired(resolvedAt, nowInBusinessZone());
    }

    /**
     * Returns true strictly AFTER the 48-business-hour dispute window has elapsed
     * relative to evaluationTime (evaluationTime > deadline).
     */
    public boolean isDisputeWindowExpired(LocalDateTime resolvedAt, LocalDateTime evaluationTime) {
        if (resolvedAt == null || evaluationTime == null) {
            return false;
        }
        LocalDateTime deadline = calculateReopenDeadline(resolvedAt);
        return evaluationTime.isAfter(deadline);
    }

    /**
     * A resolved ticket becomes eligible for auto-closure strictly AFTER the
     * 48-business-hour dispute window expires (aligned identically with isDisputeWindowExpired).
     */
    public boolean isAutoClosureEligible(LocalDateTime resolvedAt) {
        return isDisputeWindowExpired(resolvedAt);
    }

    public boolean isAutoClosureEligible(LocalDateTime resolvedAt, LocalDateTime evaluationTime) {
        return isDisputeWindowExpired(resolvedAt, evaluationTime);
    }
}

