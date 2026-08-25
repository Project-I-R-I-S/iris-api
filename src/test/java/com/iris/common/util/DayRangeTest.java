package com.iris.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DayRangeTest {

    @Test
    void spansExactlyOneCalendarDayInTheGivenZone() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");

        DayRange range = DayRange.of(date, kolkata);

        assertThat(range.from()).isEqualTo(Instant.parse("2026-08-24T18:30:00Z"));
        assertThat(range.to()).isEqualTo(Instant.parse("2026-08-25T18:30:00Z"));
    }

    @Test
    void isHalfOpen_toIsExclusive() {
        LocalDate date = LocalDate.of(2026, 8, 25);

        DayRange range = DayRange.of(date, ZoneOffset.UTC);

        assertThat(range.to()).isEqualTo(range.from().plusSeconds(24 * 3600));
    }

    @Test
    void sameCalendarDateInDifferentZonesProducesDifferentInstants() {
        LocalDate date = LocalDate.of(2026, 8, 25);

        DayRange utc = DayRange.of(date, ZoneOffset.UTC);
        DayRange kolkata = DayRange.of(date, ZoneId.of("Asia/Kolkata"));

        assertThat(utc.from()).isNotEqualTo(kolkata.from());
    }
}
