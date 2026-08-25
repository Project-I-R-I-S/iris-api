package com.iris.hydration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily hydration total — combines direct water logging with {@code fluid_ml}
 * from food/drink entries (coffee, juice, ...), per the hydration feature note.
 */
public record DailyHydrationResponse(
        LocalDate date,
        int waterMl,
        BigDecimal fluidFromFoodMl,
        BigDecimal totalMl,
        Integer goalMl,
        BigDecimal remainingMl
) {
    public static DailyHydrationResponse of(LocalDate date, int waterMl, BigDecimal fluidFromFoodMl, Integer goalMl) {
        BigDecimal total = BigDecimal.valueOf(waterMl).add(fluidFromFoodMl);
        BigDecimal remaining = goalMl == null
                ? null
                : BigDecimal.valueOf(goalMl).subtract(total).max(BigDecimal.ZERO);
        return new DailyHydrationResponse(date, waterMl, fluidFromFoodMl, total, goalMl, remaining);
    }
}
