package com.iris.user;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.user.dto.UpdateUserRequest;
import com.iris.user.dto.UserResponse;
import com.iris.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final UUID userId = UUID.randomUUID();

    private User existingUser() {
        return User.builder()
                .id(userId)
                .email("user@example.com")
                .displayName("Original Name")
                .heightCm(175.0)
                .dayStartTime(LocalTime.of(7, 0))
                .dayEndTime(LocalTime.of(23, 0))
                .timezone("Asia/Kolkata")
                .dailyWaterGoalMl(2500)
                .dailyCaffeineLimitMg(400)
                .emailVerified(true)
                .build();
    }

    @Test
    void getById_throwsNotFoundForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_returnsMappedResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser()));

        UserResponse response = userService.getById(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.displayName()).isEqualTo("Original Name");
    }

    @Test
    void update_onlyOverwritesFieldsProvidedInTheRequest() {
        User user = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateUserRequest req = new UpdateUserRequest(
                "New Name", null, null, null, null, null, null, null, null, null);

        UserResponse response = userService.update(userId, req);

        assertThat(response.displayName()).isEqualTo("New Name");
        // Untouched fields keep their original values.
        assertThat(user.getHeightCm()).isEqualTo(175.0);
        assertThat(user.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(user.getDailyWaterGoalMl()).isEqualTo(2500);
    }

    @Test
    void update_appliesAllProvidedFields() {
        User user = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateUserRequest req = new UpdateUserRequest(
                "New Name", LocalDate.of(1995, 1, 1), "female", 168.0,
                LocalTime.of(6, 30), LocalTime.of(22, 0), "UTC", 3000, 2200, 300);

        UserResponse response = userService.update(userId, req);

        assertThat(response.displayName()).isEqualTo("New Name");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1995, 1, 1));
        assertThat(response.sex()).isEqualTo("female");
        assertThat(response.heightCm()).isEqualTo(168.0);
        assertThat(response.dayStartTime()).isEqualTo(LocalTime.of(6, 30));
        assertThat(response.dayEndTime()).isEqualTo(LocalTime.of(22, 0));
        assertThat(response.timezone()).isEqualTo("UTC");
        assertThat(response.dailyWaterGoalMl()).isEqualTo(3000);
        assertThat(response.dailyCalorieGoal()).isEqualTo(2200);
        assertThat(response.dailyCaffeineLimitMg()).isEqualTo(300);
    }

    @Test
    void update_throwsNotFoundForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        UpdateUserRequest req = new UpdateUserRequest(
                "New Name", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> userService.update(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
