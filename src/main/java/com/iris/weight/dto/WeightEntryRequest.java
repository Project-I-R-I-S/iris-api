package com.iris.weight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record WeightEntryRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
        @NotNull Instant recordedAt,
        @Size(max = 500) String notes
) { }
