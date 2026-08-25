package com.iris.hydration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record WaterEntryRequest(
        @NotNull @Positive Integer amountMl,
        @NotNull Instant consumedAt
) { }
