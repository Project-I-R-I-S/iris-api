package com.iris.nutrition;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.nutrition.dto.FoodEntryRequest;
import com.iris.nutrition.dto.FoodEntryResponse;
import com.iris.nutrition.model.FoodEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
                .proteinG(nz(req.proteinG()))
                .carbsG(nz(req.carbsG()))
                .fatG(nz(req.fatG()))
                .fiberG(nz(req.fiberG()))
                .sugarG(req.sugarG())
                .sodiumMg(req.sodiumMg())
                .caffeineMg(nz(req.caffeineMg()))
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
        entry.setProteinG(nz(req.proteinG()));
        entry.setCarbsG(nz(req.carbsG()));
        entry.setFatG(nz(req.fatG()));
        entry.setFiberG(nz(req.fiberG()));
        entry.setSugarG(req.sugarG());
        entry.setSodiumMg(req.sodiumMg());
        entry.setCaffeineMg(nz(req.caffeineMg()));
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
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        return nutritionRepository
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(userId, from, to)
                .stream().map(FoodEntryResponse::from).toList();
    }

    private FoodEntry loadOwned(UUID userId, UUID entryId) {
        return nutritionRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Food entry not found: " + entryId));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
