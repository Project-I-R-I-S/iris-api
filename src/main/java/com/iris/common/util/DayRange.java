package com.iris.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Half-open [from, to) instant range covering one calendar day in a given
 * timezone. Every feature that logs timestamped, user-owned events (food,
 * water, sleep, ...) needs to answer "what happened on this day, in this
 * user's timezone" — this is that conversion, written once.
 */
public record DayRange(Instant from, Instant to) {

    public static DayRange of(LocalDate date, ZoneId zone) {
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        return new DayRange(from, to);
    }
}
