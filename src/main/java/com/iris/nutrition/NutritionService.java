package com.iris.nutrition;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.common.util.DayRange;
import com.iris.common.util.Numbers;
import com.iris.nutrition.dto.FoodEntryRequest;
import com.iris.nutrition.dto.FoodEntryResponse;
import com.iris.nutrition.model.FoodEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class NutritionService {

    private final NutritionRepository nutritionRepository;

    public NutritionService(NutritionRepository nutritionRepository) {
        this.nutritionRepository = nutritionRepository;
    }

    @Transactional
    public FoodEntryResponse create(UUID userId, FoodEntryRequest req) {
        FoodEntry entry = FoodEntry.builder()
                .userId(userId)
                .name(req.name())
                .brand(req.brand())
                .mealType(req.mealType())
                .servingSize(req.servingSize())
                .servingUnit(req.servingUnit())
                .calories(req.calories())
                .proteinG(Numbers.nz(req.proteinG()))
                .carbsG(Numbers.nz(req.carbsG()))
                .fatG(Numbers.nz(req.fatG()))
                .fiberG(Numbers.nz(req.fiberG()))
                .sugarG(req.sugarG())
                .sodiumMg(req.sodiumMg())
                .caffeineMg(Numbers.nz(req.caffeineMg()))
                .fluidMl(req.fluidMl())
                .notes(req.notes())
                .consumedAt(req.consumedAt())
                .build();

        return FoodEntryResponse.from(nutritionRepository.save(entry));
    }

    @Transactional
    public FoodEntryResponse update(UUID userId, UUID entryId, FoodEntryRequest req) {
        FoodEntry entry = loadOwned(userId, entryId);
        entry.setName(req.name());
        entry.setBrand(req.brand());
        entry.setMealType(req.mealType());
        entry.setServingSize(req.servingSize());
        entry.setServingUnit(req.servingUnit());
        entry.setCalories(req.calories());
        entry.setProteinG(Numbers.nz(req.proteinG()));
        entry.setCarbsG(Numbers.nz(req.carbsG()));
        entry.setFatG(Numbers.nz(req.fatG()));
        entry.setFiberG(Numbers.nz(req.fiberG()));
        entry.setSugarG(req.sugarG());
        entry.setSodiumMg(req.sodiumMg());
        entry.setCaffeineMg(Numbers.nz(req.caffeineMg()));
        entry.setFluidMl(req.fluidMl());
        entry.setNotes(req.notes());
        entry.setConsumedAt(req.consumedAt());
        return FoodEntryResponse.from(entry);
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        FoodEntry entry = loadOwned(userId, entryId);
        nutritionRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<FoodEntryResponse> listForDay(UUID userId, LocalDate date, ZoneId zone) {
        DayRange range = DayRange.of(date, zone);
        return nutritionRepository
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(userId, range.from(), range.to())
                .stream().map(FoodEntryResponse::from).toList();
    }

    /**
     * Total {@code fluid_ml} logged as food/drink entries for one day, in the user's
     * timezone. Called by {@code hydration} (via this service, never the repository —
     * see CLAUDE.md rule 1) so a coffee or juice logged under nutrition also counts
     * toward the day's hydration total.
     */
    @Transactional(readOnly = true)
    public BigDecimal totalFluidMlForDay(UUID userId, LocalDate date, ZoneId zone) {
        DayRange range = DayRange.of(date, zone);
        return nutritionRepository.sumFluidMlForUserAndRange(userId, range.from(), range.to());
    }

    private FoodEntry loadOwned(UUID userId, UUID entryId) {
        return nutritionRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Food entry", entryId));
    }
}
