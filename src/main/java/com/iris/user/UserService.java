package com.iris.user;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.user.dto.UpdateUserRequest;
import com.iris.user.dto.UserResponse;
import com.iris.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        return UserResponse.from(loadOrThrow(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest req) {
        User u = loadOrThrow(userId);

        if (req.displayName() != null)          u.setDisplayName(req.displayName());
        if (req.dateOfBirth() != null)          u.setDateOfBirth(req.dateOfBirth());
        if (req.sex() != null)                  u.setSex(req.sex());
        if (req.heightCm() != null)             u.setHeightCm(req.heightCm());
        if (req.dayStartTime() != null)         u.setDayStartTime(req.dayStartTime());
        if (req.dayEndTime() != null)           u.setDayEndTime(req.dayEndTime());
        if (req.timezone() != null)             u.setTimezone(req.timezone());
        if (req.dailyWaterGoalMl() != null)     u.setDailyWaterGoalMl(req.dailyWaterGoalMl());
        if (req.dailyCalorieGoal() != null)     u.setDailyCalorieGoal(req.dailyCalorieGoal());
        if (req.dailyCaffeineLimitMg() != null) u.setDailyCaffeineLimitMg(req.dailyCaffeineLimitMg());

        return UserResponse.from(u);
    }

    private User loadOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
