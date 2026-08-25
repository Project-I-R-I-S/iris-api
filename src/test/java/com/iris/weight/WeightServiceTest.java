package com.iris.weight;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.user.UserService;
import com.iris.user.dto.UserResponse;
import com.iris.weight.dto.WeightEntryRequest;
import com.iris.weight.dto.WeightEntryResponse;
import com.iris.weight.model.WeightEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeightServiceTest {

    @Mock
    private WeightRepository weightRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private WeightService weightService;

    private final UUID userId = UUID.randomUUID();

    private UserResponse userWithHeight(Double heightCm) {
        return new UserResponse(userId, "u@example.com", "User", null, null, heightCm,
                null, null, "UTC", 2500, null, 400, true);
    }

    @Test
    void create_computesBmiFromWeightAndUsersHeight() {
        when(weightRepository.save(any(WeightEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userService.getById(userId)).thenReturn(userWithHeight(180.0));

        WeightEntryResponse response = weightService.create(
                userId, new WeightEntryRequest(new BigDecimal("81.0"), Instant.now(), null));

        // BMI = 81 / 1.8^2 = 25.0
        assertThat(response.bmi()).isEqualByComparingTo("25.0");
    }

    @Test
    void create_bmiIsNullWhenUserHasNoHeightSet() {
        when(weightRepository.save(any(WeightEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userService.getById(userId)).thenReturn(userWithHeight(null));

        WeightEntryResponse response = weightService.create(
                userId, new WeightEntryRequest(new BigDecimal("70"), Instant.now(), null));

        assertThat(response.bmi()).isNull();
    }

    @Test
    void create_bmiIsNullWhenHeightIsZeroOrNegative() {
        when(weightRepository.save(any(WeightEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userService.getById(userId)).thenReturn(userWithHeight(0.0));

        WeightEntryResponse response = weightService.create(
                userId, new WeightEntryRequest(new BigDecimal("70"), Instant.now(), null));

        assertThat(response.bmi()).isNull();
    }

    @Test
    void update_throwsNotFoundForAnotherUsersEntry() {
        UUID entryId = UUID.randomUUID();
        when(weightRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weightService.update(
                userId, entryId, new WeightEntryRequest(new BigDecimal("70"), Instant.now(), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_mutatesTheOwnedManagedEntityAndRecalculatesBmi() {
        UUID entryId = UUID.randomUUID();
        WeightEntry existing = WeightEntry.builder()
                .id(entryId).userId(userId).weightKg(new BigDecimal("90")).build();
        when(weightRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(existing));
        when(userService.getById(userId)).thenReturn(userWithHeight(180.0));

        WeightEntryResponse response = weightService.update(
                userId, entryId, new WeightEntryRequest(new BigDecimal("81.0"), Instant.now(), "lost some"));

        assertThat(existing.getWeightKg()).isEqualByComparingTo("81.0");
        assertThat(existing.getNotes()).isEqualTo("lost some");
        assertThat(response.bmi()).isEqualByComparingTo("25.0");
    }

    @Test
    void delete_removesOnlyWhenOwnedByUser() {
        UUID entryId = UUID.randomUUID();
        WeightEntry existing = WeightEntry.builder().id(entryId).userId(userId).build();
        when(weightRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(existing));

        weightService.delete(userId, entryId);

        verify(weightRepository).delete(existing);
    }

    @Test
    void listInRange_queriesTheHalfOpenRangeCoveringFromThroughTo() {
        when(weightRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(userService.getById(userId)).thenReturn(userWithHeight(180.0));

        weightService.listInRange(userId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 25), ZoneOffset.UTC);

        verify(weightRepository).findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                userId,
                Instant.parse("2026-08-20T00:00:00Z"),
                Instant.parse("2026-08-26T00:00:00Z"));
    }

    @Test
    void latest_throwsNotFoundWhenUserHasNoEntries() {
        when(weightRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weightService.latest(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void latest_returnsMostRecentEntryWithBmi() {
        WeightEntry entry = WeightEntry.builder()
                .id(UUID.randomUUID()).userId(userId).weightKg(new BigDecimal("81.0")).build();
        when(weightRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)).thenReturn(Optional.of(entry));
        when(userService.getById(userId)).thenReturn(userWithHeight(180.0));

        WeightEntryResponse response = weightService.latest(userId);

        assertThat(response.bmi()).isEqualByComparingTo("25.0");
    }
}
