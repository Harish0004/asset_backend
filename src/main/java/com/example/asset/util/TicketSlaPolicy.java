package com.example.asset.util;

import com.example.asset.entity.Ticket;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * SLA deadlines by priority (business minutes from ticket creation).
 */
public final class TicketSlaPolicy {

    private static final LocalTime BUSINESS_OPEN = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_CLOSE = LocalTime.of(18, 0);

    private TicketSlaPolicy() {
    }

    public static int slaMinutesFor(Ticket.Priority priority) {
        if (priority == null) {
            return 10080; // 7 business days * 24*60
        }
        return switch (priority) {
            case CRITICAL -> 4;
            case HIGH -> 1440;
            case MEDIUM -> 4320;
            case LOW -> 10080;      
        };
    }

    @Deprecated(since = "minutes-migration")
    public static int slaHoursFor(Ticket.Priority priority) {
        return slaMinutesFor(priority) / 60;
    }

    public static LocalDateTime deadlineFrom(LocalDateTime createdAt, Ticket.Priority priority) {
        LocalDateTime current = normalizeToBusinessHours(
                createdAt != null ? createdAt : LocalDateTime.now());
        int remainingMinutes = slaMinutesFor(priority);

        while (remainingMinutes > 0) {
            LocalDateTime endOfBusinessDay = LocalDateTime.of(current.toLocalDate(), BUSINESS_CLOSE);
            long availableMinutes = Duration.between(current, endOfBusinessDay).toMinutes();
            if (availableMinutes >= remainingMinutes) {
                return current.plusMinutes(remainingMinutes);
            }
            remainingMinutes -= (int) availableMinutes;
            current = nextBusinessStart(current.plusDays(1));
        }
        return current;
    }

    private static LocalDateTime normalizeToBusinessHours(LocalDateTime dateTime) {
        LocalDateTime normalized = dateTime;
        if (isWeekend(normalized)) {
            normalized = nextBusinessStart(normalized.plusDays(1));
        }
        if (normalized.toLocalTime().isBefore(BUSINESS_OPEN)) {
            normalized = normalized.with(BUSINESS_OPEN);
        } else if (!normalized.toLocalTime().isBefore(BUSINESS_CLOSE)) {
            normalized = nextBusinessStart(normalized.plusDays(1));
        }
        if (isWeekend(normalized)) {
            normalized = nextBusinessStart(normalized.plusDays(1));
        }
        return normalized;
    }

    private static LocalDateTime nextBusinessStart(LocalDateTime dateTime) {
        LocalDateTime next = dateTime.with(BUSINESS_OPEN);
        while (isWeekend(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    private static boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static boolean isBreached(Ticket ticket) {
        if (ticket == null || ticket.getDeadlineAt() == null) {
            return false;
        }
        LocalDateTime comparisonPoint = ticket.getResolvedAt() != null
                ? ticket.getResolvedAt()
                : LocalDateTime.now();
        return comparisonPoint.isAfter(ticket.getDeadlineAt());
    }
}