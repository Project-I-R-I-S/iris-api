package com.iris.nutrition;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.nutrition.dto.FoodEntryRequest;
import com.iris.nutrition.dto.FoodEntryResponse;
import com.iris.nutrition.model.FoodEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
class NutritionServiceTest {

    @Mock
    private NutritionRepository nutritionRepository;

    @InjectMocks
    private NutritionService nutritionService;

    @Captor
    private ArgumentCaptor<FoodEntry> entryCaptor;

    private final UUID userId = UUID.randomUUID();

    private FoodEntryRequest fullRequest() {
        return new FoodEntryRequest(
                "Banana", "Chiquita", "snack",
                new BigDecimal("120"), "g",
                new BigDecimal("105"),
                new BigDecimal("1.3"), new BigDecimal("27"), new BigDecimal("0.4"), new BigDecimal("3.1"),
                new BigDecimal("14"), new BigDecimal("1"), new BigDecimal("0"),
                null, "ripe", Instant.parse("2026-08-25T08:00:00Z"));
    }

    @Test
    void create_savesEntryScopedToTheGivenUser() {
        when(nutritionRepository.save(any(FoodEntry.class))).thenAnswer(inv -> {
            FoodEntry e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        FoodEntryResponse response = nutritionService.create(userId, fullRequest());

        verify(nutritionRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(entryCaptor.getValue().getName()).isEqualTo("Banana");
        assertThat(response.name()).isEqualTo("Banana");
        assertThat(response.calories()).isEqualByComparingTo("105");
    }

    @Test
    void create_defaultsOptionalMacrosToZeroWhenOmitted() {
        FoodEntryRequest req = new FoodEntryRequest(
                "Water crackers", null, "snack",
                new BigDecimal("30"), "g", new BigDecimal("120"),
                null, null, null, null,
                null, null, null,
                null, null, Instant.now());
        when(nutritionRepository.save(any(FoodEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        nutritionService.create(userId, req);

        verify(nutritionRepository).save(entryCaptor.capture());
        FoodEntry saved = entryCaptor.getValue();
        assertThat(saved.getProteinG()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCarbsG()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getFatG()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getFiberG()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCaffeineMg()).isEqualByComparingTo(BigDecimal.ZERO);
        // sugar/sodium/fluid have no non-null default per the reference implementation
        assertThat(saved.getSugarG()).isNull();
        assertThat(saved.getSodiumMg()).isNull();
        assertThat(saved.getFluidMl()).isNull();
    }

    @Test
    void update_throwsNotFoundWhenEntryDoesNotBelongToUser() {
        UUID entryId = UUID.randomUUID();
        when(nutritionRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nutritionService.update(userId, entryId, fullRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(entryId.toString());

        verify(nutritionRepository, never()).save(any());
    }

    @Test
    void update_mutatesTheOwnedManagedEntity() {
        UUID entryId = UUID.randomUUID();
        FoodEntry existing = FoodEntry.builder().id(entryId).userId(userId).name("Old").build();
        when(nutritionRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(existing));

        FoodEntryResponse response = nutritionService.update(userId, entryId, fullRequest());

        assertThat(existing.getName()).isEqualTo("Banana");
        assertThat(response.name()).isEqualTo("Banana");
    }

    @Test
    void delete_removesOnlyWhenOwnedByUser() {
        UUID entryId = UUID.randomUUID();
        FoodEntry existing = FoodEntry.builder().id(entryId).userId(userId).build();
        when(nutritionRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(existing));

        nutritionService.delete(userId, entryId);

        verify(nutritionRepository).delete(existing);
    }

    @Test
    void delete_throwsNotFoundForAnotherUsersEntry() {
        UUID entryId = UUID.randomUUID();
        when(nutritionRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nutritionService.delete(userId, entryId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(nutritionRepository, never()).delete(any());
    }

    @Test
    void listForDay_queriesTheHalfOpenDayRangeInTheGivenZone() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        when(nutritionRepository.findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(any(), any(), any()))
                .thenReturn(List.of());

        nutritionService.listForDay(userId, date, ZoneOffset.UTC);

        verify(nutritionRepository).findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
                eq(userId),
                eq(Instant.parse("2026-08-25T00:00:00Z")),
                eq(Instant.parse("2026-08-26T00:00:00Z")));
    }

    @Test
    void totalFluidMlForDay_delegatesToTheSumQueryForTheDayRange() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        when(nutritionRepository.sumFluidMlForUserAndRange(any(), any(), any()))
                .thenReturn(new BigDecimal("350"));

        BigDecimal total = nutritionService.totalFluidMlForDay(userId, date, ZoneOffset.UTC);

        assertThat(total).isEqualByComparingTo("350");
        verify(nutritionRepository).sumFluidMlForUserAndRange(
                userId, Instant.parse("2026-08-25T00:00:00Z"), Instant.parse("2026-08-26T00:00:00Z"));
    }
}
