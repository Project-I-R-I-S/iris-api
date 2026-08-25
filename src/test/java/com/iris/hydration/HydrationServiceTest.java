package com.iris.hydration;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.hydration.dto.DailyHydrationResponse;
import com.iris.hydration.dto.WaterEntryRequest;
import com.iris.hydration.dto.WaterEntryResponse;
import com.iris.hydration.model.WaterEntry;
import com.iris.nutrition.NutritionService;
import com.iris.user.UserService;
import com.iris.user.dto.UserResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HydrationServiceTest {

    @Mock
    private HydrationRepository hydrationRepository;
    @Mock
    private NutritionService nutritionService;
    @Mock
    private UserService userService;

    @InjectMocks
    private HydrationService hydrationService;

    private final UUID userId = UUID.randomUUID();

    private UserResponse userWithGoal(Integer goalMl) {
        return new UserResponse(userId, "u@example.com", "User", null, null, null,
                null, null, "UTC", goalMl, null, 400, true);
    }

    @Test
    void create_savesEntryScopedToTheGivenUser() {
        WaterEntryRequest req = new WaterEntryRequest(250, Instant.parse("2026-08-25T08:00:00Z"));
        when(hydrationRepository.save(any(WaterEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        WaterEntryResponse response = hydrationService.create(userId, req);

        assertThat(response.amountMl()).isEqualTo(250);
    }

    @Test
    void update_throwsNotFoundForAnotherUsersEntry() {
        UUID entryId = UUID.randomUUID();
        when(hydrationRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hydrationService.update(
                userId, entryId, new WaterEntryRequest(200, Instant.now())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesOnlyWhenOwnedByUser() {
        UUID entryId = UUID.randomUUID();
        WaterEntry existing = WaterEntry.builder().id(entryId).userId(userId).build();
        when(hydrationRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(existing));

        hydrationService.delete(userId, entryId);

        verify(hydrationRepository).delete(existing);
    }

    @Test
    void listForDay_queriesTheHalfOpenDayRangeInTheGivenZone() {
        when(hydrationRepository.findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(any(), any(), any()))
                .thenReturn(List.of());

        hydrationService.listForDay(userId, LocalDate.of(2026, 8, 25), ZoneOffset.UTC);

        verify(hydrationRepository).findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
                eq(userId),
                eq(Instant.parse("2026-08-25T00:00:00Z")),
                eq(Instant.parse("2026-08-26T00:00:00Z")));
    }

    @Test
    void dailyTotal_combinesDirectWaterLoggingWithFluidFromNutrition() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        when(hydrationRepository.sumAmountMlForUserAndRange(any(), any(), any())).thenReturn(1000);
        when(nutritionService.totalFluidMlForDay(userId, date, ZoneOffset.UTC))
                .thenReturn(new BigDecimal("350"));
        when(userService.getById(userId)).thenReturn(userWithGoal(2500));

        DailyHydrationResponse response = hydrationService.dailyTotal(userId, date, ZoneOffset.UTC);

        assertThat(response.waterMl()).isEqualTo(1000);
        assertThat(response.fluidFromFoodMl()).isEqualByComparingTo("350");
        assertThat(response.totalMl()).isEqualByComparingTo("1350");
        assertThat(response.remainingMl()).isEqualByComparingTo("1150");
    }

    @Test
    void dailyTotal_remainingNeverGoesNegativeWhenGoalIsExceeded() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        when(hydrationRepository.sumAmountMlForUserAndRange(any(), any(), any())).thenReturn(3000);
        when(nutritionService.totalFluidMlForDay(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(userService.getById(userId)).thenReturn(userWithGoal(2500));

        DailyHydrationResponse response = hydrationService.dailyTotal(userId, date, ZoneOffset.UTC);

        assertThat(response.remainingMl()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dailyTotal_remainingIsNullWhenUserHasNoGoalSet() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        when(hydrationRepository.sumAmountMlForUserAndRange(any(), any(), any())).thenReturn(500);
        when(nutritionService.totalFluidMlForDay(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(userService.getById(userId)).thenReturn(userWithGoal(null));

        DailyHydrationResponse response = hydrationService.dailyTotal(userId, date, ZoneOffset.UTC);

        assertThat(response.remainingMl()).isNull();
    }
}
