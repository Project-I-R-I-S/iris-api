package com.iris.weight.dto;

import com.iris.weight.model.WeightEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeightEntryResponse(
        UUID id,
        BigDecimal weightKg,
        Instant recordedAt,
        String notes,
        BigDecimal bmi,
        Instant createdAt
) {
    public static WeightEntryResponse from(WeightEntry e, BigDecimal bmi) {
        return new WeightEntryResponse(
                e.getId(), e.getWeightKg(), e.getRecordedAt(), e.getNotes(), bmi, e.getCreatedAt());
    }
}
