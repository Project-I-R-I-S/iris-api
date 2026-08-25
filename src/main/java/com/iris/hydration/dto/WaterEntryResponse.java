package com.iris.hydration.dto;

import com.iris.hydration.model.WaterEntry;

import java.time.Instant;
import java.util.UUID;

public record WaterEntryResponse(
        UUID id,
        Integer amountMl,
        Instant consumedAt,
        Instant createdAt
) {
    public static WaterEntryResponse from(WaterEntry e) {
        return new WaterEntryResponse(e.getId(), e.getAmountMl(), e.getConsumedAt(), e.getCreatedAt());
    }
}
