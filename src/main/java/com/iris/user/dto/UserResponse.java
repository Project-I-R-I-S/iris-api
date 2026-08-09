package com.iris.user.dto;

import com.iris.user.model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        LocalDate dateOfBirth,
        String sex,
        Double heightCm,
        LocalTime dayStartTime,
        LocalTime dayEndTime,
        String timezone,
        Integer dailyWaterGoalMl,
        Integer dailyCalorieGoal,
        Integer dailyCaffeineLimitMg,
        boolean emailVerified
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getDateOfBirth(),
                u.getSex(),
                u.getHeightCm(),
                u.getDayStartTime(),
                u.getDayEndTime(),
                u.getTimezone(),
                u.getDailyWaterGoalMl(),
                u.getDailyCalorieGoal(),
                u.getDailyCaffeineLimitMg(),
                u.isEmailVerified()
        );
    }
}
