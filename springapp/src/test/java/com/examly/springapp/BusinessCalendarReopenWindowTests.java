package com.examly.springapp;

import com.examly.springapp.exception.BusinessRuleViolationException;
import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.workflow.BusinessCalendar;
import com.examly.springapp.workflow.TicketStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P9.1 Deterministic Boundary Tests for the 48-Business-Hour Ticket Reopen / Dispute Window (BR-003).
 *
 * Calendar Specification:
 *  - Timezone: Asia/Kolkata (UTC+05:30, no DST)
 *  - Working days: Monday-Friday
 *  - Working hours: 09:00-18:00 (9 business hours/day, 45 business hours/week)
 *  - Exclusions: Saturdays, Sundays, and configured corporate holidays
 *  - Deadline rule: INCLUSIVE of the exact deadline instant (now <= deadline allowed; now > deadline rejected)
 */
public class BusinessCalendarReopenWindowTests {

    private static final ZoneId IST = BusinessCalendar.EPAMS_TIMEZONE;
    private static final String VALID_REASON = "Issue recurred after initial patch; requires further investigation.";

    private Clock fixedIstClock(LocalDateTime istDateTime) {
        return Clock.fixed(istDateTime.atZone(IST).toInstant(), IST);
    }

    private ServiceTicket resolvedTicketAt(LocalDateTime resolvedAt) {
        ServiceTicket ticket = new ServiceTicket();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(resolvedAt);
        return ticket;
    }

    @Test
    @DisplayName("Scenario 1: Resolution during the middle of a working day accumulates 48 business hours accurately")
    void scenario1_ResolutionMidWorkingDay() {
        // Mon 2026-03-02 13:30 IST:
        // Mon 4.5h + Tue 9h + Wed 9h + Thu 9h + Fri 9h = 40.5h; Sat/Sun skipped; Next Mon +7.5h = 2026-03-09 16:30
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 2, 13, 30, 0);
        LocalDateTime expectedDeadline = LocalDateTime.of(2026, 3, 9, 16, 30, 0);

        BusinessCalendar calendar = new BusinessCalendar(fixedIstClock(resolvedAt));
        assertEquals(expectedDeadline, calendar.calculateReopenDeadline(resolvedAt));
        assertNotEquals(resolvedAt.plusHours(48), calendar.calculateReopenDeadline(resolvedAt),
                "Deadline must NOT use 48 wall-clock hours");
    }

    @Test
    @DisplayName("Scenario 2: Resolution near end of working hours preserves exact second-level precision")
    void scenario2_ResolutionNearEndOfWorkingHours() {
        // Mon 2026-03-02 17:59:30 IST -> 30s remaining on Mon, 47h 59m 30s remaining:
        // Tue-Fri = 36h (11h 59m 30s left), Sat/Sun skipped, Next Mon = 9h (2h 59m 30s left), Next Tue 09:00 + 2h 59m 30s = 11:59:30
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 2, 17, 59, 30);
        LocalDateTime expectedDeadline = LocalDateTime.of(2026, 3, 10, 11, 59, 30);

        BusinessCalendar calendar = new BusinessCalendar(fixedIstClock(resolvedAt));
        assertEquals(expectedDeadline, calendar.calculateReopenDeadline(resolvedAt));
    }

    @Test
    @DisplayName("Scenario 3: Resolution on Friday afternoon spans weekend cleanly")
    void scenario3_ResolutionOnFriday() {
        // Fri 2026-03-06 14:00 IST -> 4h on Fri, Sat/Sun skipped, Mon-Thu = 36h (8h left), Fri 2026-03-13 09:00 + 8h = 17:00
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 6, 14, 0, 0);
        LocalDateTime expectedDeadline = LocalDateTime.of(2026, 3, 13, 17, 0, 0);

        BusinessCalendar calendar = new BusinessCalendar(fixedIstClock(resolvedAt));
        assertEquals(expectedDeadline, calendar.calculateReopenDeadline(resolvedAt));
    }

    @Test
    @DisplayName("Scenario 4: Resolution immediately before weekend (Friday 18:00) starts counting Monday 09:00")
    void scenario4_ResolutionBeforeWeekend() {
        // Fri 2026-03-06 18:00 IST -> normalizes to Mon 2026-03-09 09:00
        // Mon-Fri = 45h (3h left), Sat/Sun skipped -> Mon 2026-03-16 12:00
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 6, 18, 0, 0);
        LocalDateTime expectedDeadline = LocalDateTime.of(2026, 3, 16, 12, 0, 0);

        BusinessCalendar calendar = new BusinessCalendar(fixedIstClock(resolvedAt));
        assertEquals(expectedDeadline, calendar.calculateReopenDeadline(resolvedAt));
    }

    @Test
    @DisplayName("Scenario 5: Resolution before a configured corporate holiday excludes holiday and weekend")
    void scenario5_ResolutionBeforeConfiguredHoliday() {
        // Thu 2026-10-01 14:00 IST -> 4h on Thu; Fri 2026-10-02 is configured Gandhi Jayanti holiday (skipped);
        // Sat/Sun Oct 3-4 skipped; Mon Oct 5 - Thu Oct 8 = 36h (8h left); Fri Oct 9 09:00 + 8h = 17:00
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 10, 1, 14, 0, 0);
        LocalDateTime expectedDeadline = LocalDateTime.of(2026, 10, 9, 17, 0, 0);

        BusinessCalendar calendar = new BusinessCalendar(fixedIstClock(resolvedAt));
        assertTrue(calendar.getConfiguredHolidays().contains(LocalDate.of(2026, 10, 2)),
                "2026-10-02 must be present in existing configured holiday source");
        assertEquals(expectedDeadline, calendar.calculateReopenDeadline(resolvedAt));

        // Adding another configured holiday on Wed 2026-10-07 shifts the deadline forward by 1 full business day -> Mon 2026-10-12 17:00
        calendar.addHoliday(LocalDate.of(2026, 10, 7));
        assertEquals(LocalDateTime.of(2026, 10, 12, 17, 0, 0), calendar.calculateReopenDeadline(resolvedAt));
    }

    @Test
    @DisplayName("Scenario 6: Reopen within 48-business-hour window (including >50 wall-clock hours) succeeds")
    void scenario6_ReopenWithinWindowSucceeds() {
        // Resolved Fri 2026-03-06 14:00 -> Deadline Fri 2026-03-13 17:00
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 6, 14, 0, 0);
        // Evaluate at Mon 2026-03-09 16:00 (74 wall-clock hours later, only 11 business hours elapsed)
        LocalDateTime nowWithin = LocalDateTime.of(2026, 3, 9, 16, 0, 0);
        Clock clock = fixedIstClock(nowWithin);
        BusinessCalendar calendar = new BusinessCalendar(clock);
        TicketStateMachine stateMachine = new TicketStateMachine(clock, calendar);

        assertTrue(calendar.isWithinReopenWindow(resolvedAt));
        assertFalse(calendar.isDisputeWindowExpired(resolvedAt));
        assertFalse(calendar.isAutoClosureEligible(resolvedAt));
        assertDoesNotThrow(() -> stateMachine.validateTransition(
                resolvedTicketAt(resolvedAt), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));

        // Evaluate 1 second before exact deadline -> STILL ALLOWED
        LocalDateTime oneSecondBefore = LocalDateTime.of(2026, 3, 13, 16, 59, 59);
        Clock clockBefore = fixedIstClock(oneSecondBefore);
        BusinessCalendar calBefore = new BusinessCalendar(clockBefore);
        TicketStateMachine smBefore = new TicketStateMachine(clockBefore, calBefore);
        assertTrue(calBefore.isWithinReopenWindow(resolvedAt));
        assertDoesNotThrow(() -> smBefore.validateTransition(
                resolvedTicketAt(resolvedAt), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));
    }

    @Test
    @DisplayName("Scenario 7: Reopen EXACTLY at the 48-business-hour deadline is INCLUSIVE and succeeds")
    void scenario7_ReopenExactlyAtDeadlineIsInclusive() {
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 6, 14, 0, 0);
        LocalDateTime exactDeadline = LocalDateTime.of(2026, 3, 13, 17, 0, 0);

        Clock clockAtDeadline = fixedIstClock(exactDeadline);
        BusinessCalendar calendar = new BusinessCalendar(clockAtDeadline);
        TicketStateMachine stateMachine = new TicketStateMachine(clockAtDeadline, calendar);

        assertEquals(exactDeadline, calendar.calculateReopenDeadline(resolvedAt));
        assertTrue(calendar.isWithinReopenWindow(resolvedAt), "Exact deadline instant must be INCLUSIVE");
        assertFalse(calendar.isDisputeWindowExpired(resolvedAt), "Exact deadline instant must NOT be expired");
        assertFalse(calendar.isAutoClosureEligible(resolvedAt), "Auto-closure must NOT overlap inclusive reopen boundary");
        assertDoesNotThrow(() -> stateMachine.validateTransition(
                resolvedTicketAt(resolvedAt), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));
    }

    @Test
    @DisplayName("Scenario 8: Reopen strictly AFTER the 48-business-hour deadline is rejected and auto-closure becomes eligible")
    void scenario8_ReopenAfterDeadlineIsRejected() {
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 6, 14, 0, 0);
        LocalDateTime oneSecondAfter = LocalDateTime.of(2026, 3, 13, 17, 0, 1);

        Clock clockAfter = fixedIstClock(oneSecondAfter);
        BusinessCalendar calendar = new BusinessCalendar(clockAfter);
        TicketStateMachine stateMachine = new TicketStateMachine(clockAfter, calendar);

        assertFalse(calendar.isWithinReopenWindow(resolvedAt));
        assertTrue(calendar.isDisputeWindowExpired(resolvedAt));
        assertTrue(calendar.isAutoClosureEligible(resolvedAt));
        assertThrows(BusinessRuleViolationException.class, () -> stateMachine.validateTransition(
                resolvedTicketAt(resolvedAt), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));

        // Null resolvedAt is also safely rejected by TicketStateMachine
        assertThrows(BusinessRuleViolationException.class, () -> stateMachine.validateTransition(
                resolvedTicketAt(null), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));
    }

    @Test
    @DisplayName("Scenario 9: Resolution outside working hours (early morning, late night, weekend, holiday) normalizes to next business day 09:00")
    void scenario9_ResolutionOutsideWorkingHours() {
        BusinessCalendar calendar = new BusinessCalendar(fixedIstClock(LocalDateTime.of(2026, 3, 2, 12, 0)));

        // All of these normalize to Mon 2026-03-09 09:00 -> +48h = Mon 2026-03-16 12:00
        LocalDateTime expected = LocalDateTime.of(2026, 3, 16, 12, 0, 0);
        assertEquals(expected, calendar.calculateReopenDeadline(LocalDateTime.of(2026, 3, 6, 20, 30)), "Friday late night");
        assertEquals(expected, calendar.calculateReopenDeadline(LocalDateTime.of(2026, 3, 7, 11, 0)), "Saturday morning");
        assertEquals(expected, calendar.calculateReopenDeadline(LocalDateTime.of(2026, 3, 8, 16, 45)), "Sunday afternoon");
        assertEquals(expected, calendar.calculateReopenDeadline(LocalDateTime.of(2026, 3, 9, 6, 15)), "Monday before 09:00");

        // Resolved on configured holiday Mon 2026-01-26 11:00 (Republic Day) -> normalizes to Tue 2026-01-27 09:00
        // Tue-Fri = 36h, Sat/Sun skipped, Mon 2026-02-02 = 9h (3h left), Tue 2026-02-03 09:00 + 3h = 12:00
        assertEquals(LocalDateTime.of(2026, 2, 3, 12, 0, 0),
                calendar.calculateReopenDeadline(LocalDateTime.of(2026, 1, 26, 11, 0)),
                "Holiday resolution must start counting next business day 09:00 and cross month boundary cleanly");
    }

    @Test
    @DisplayName("Scenario 10: Asia/Kolkata timezone conversion from UTC Clock, non-DST stability, and year-end Christmas holiday rollover")
    void scenario10_TimezoneNonDstAndYearBoundaryBehavior() {
        // 1. Fixed UTC Clock at 2026-03-13T11:30:00Z == 2026-03-13T17:00:00 IST (exact deadline for Fri Mar 6 14:00 IST)
        // Note: US/Europe spring-forward DST shifts occur in March, while Asia/Kolkata remains fixed UTC+05:30!
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 3, 6, 14, 0, 0);
        Clock utcClockAtExactIstDeadline = Clock.fixed(Instant.parse("2026-03-13T11:30:00Z"), ZoneOffset.UTC);
        BusinessCalendar calAtDeadline = new BusinessCalendar(utcClockAtExactIstDeadline);
        TicketStateMachine smAtDeadline = new TicketStateMachine(utcClockAtExactIstDeadline, calAtDeadline);

        assertEquals(LocalDateTime.of(2026, 3, 13, 17, 0, 0), calAtDeadline.nowInBusinessZone());
        assertTrue(calAtDeadline.isWithinReopenWindow(resolvedAt));
        assertDoesNotThrow(() -> smAtDeadline.validateTransition(
                resolvedTicketAt(resolvedAt), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));

        // 1 second later in UTC (11:30:01Z == 17:00:01 IST) -> EXPIRED
        Clock utcClockOneSecPast = Clock.fixed(Instant.parse("2026-03-13T11:30:01Z"), ZoneOffset.UTC);
        BusinessCalendar calPast = new BusinessCalendar(utcClockOneSecPast);
        TicketStateMachine smPast = new TicketStateMachine(utcClockOneSecPast, calPast);
        assertFalse(calPast.isWithinReopenWindow(resolvedAt));
        assertThrows(BusinessRuleViolationException.class, () -> smPast.validateTransition(
                resolvedTicketAt(resolvedAt), TicketStatus.IN_PROGRESS, VALID_REASON, "alice"));

        // 2. Year-end rollover across configured Christmas holiday (Fri 2026-12-25) into 2027:
        // Thu 2026-12-24 15:00 -> 3h on Thu; Fri Dec 25 (holiday) & Sat/Sun Dec 26-27 skipped;
        // Mon Dec 28..Thu Dec 31 = 36h; Fri 2027-01-01 = 9h -> Fri 2027-01-01 18:00 (3 + 36 + 9 = 48h)
        LocalDateTime dec24Resolved = LocalDateTime.of(2026, 12, 24, 15, 0, 0);
        assertEquals(LocalDateTime.of(2027, 1, 1, 18, 0, 0), calAtDeadline.calculateReopenDeadline(dec24Resolved));
    }
}
