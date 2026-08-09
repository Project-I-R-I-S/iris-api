package com.iris.user.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateUserRequest(
        @Size(max = 120) String displayName,
        LocalDate dateOfBirth,
        String sex,
        @Positive Double heightCm,
        LocalTime dayStartTime,
        LocalTime dayEndTime,
        String timezone,
        @Positive Integer dailyWaterGoalMl,
        @Positive Integer dailyCalorieGoal,
        @Positive Integer dailyCaffeineLimitMg
) { }
