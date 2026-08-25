package com.iris.hydration;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.common.util.DayRange;
import com.iris.hydration.dto.DailyHydrationResponse;
import com.iris.hydration.dto.WaterEntryRequest;
import com.iris.hydration.dto.WaterEntryResponse;
import com.iris.hydration.model.WaterEntry;
import com.iris.nutrition.NutritionService;
import com.iris.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class HydrationService {

    private final HydrationRepository hydrationRepository;
    private final NutritionService nutritionService;
    private final UserService userService;

    public HydrationService(HydrationRepository hydrationRepository,
                             NutritionService nutritionService,
                             UserService userService) {
        this.hydrationRepository = hydrationRepository;
        this.nutritionService = nutritionService;
        this.userService = userService;
    }

    @Transactional
    public WaterEntryResponse create(UUID userId, WaterEntryRequest req) {
        WaterEntry entry = WaterEntry.builder()
                .userId(userId)
                .amountMl(req.amountMl())
                .consumedAt(req.consumedAt())
                .build();
        return WaterEntryResponse.from(hydrationRepository.save(entry));
    }

    @Transactional
    public WaterEntryResponse update(UUID userId, UUID entryId, WaterEntryRequest req) {
        WaterEntry entry = loadOwned(userId, entryId);
        entry.setAmountMl(req.amountMl());
        entry.setConsumedAt(req.consumedAt());
        return WaterEntryResponse.from(entry);
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        hydrationRepository.delete(loadOwned(userId, entryId));
    }

    @Transactional(readOnly = true)
    public List<WaterEntryResponse> listForDay(UUID userId, LocalDate date, ZoneId zone) {
        DayRange range = DayRange.of(date, zone);
        return hydrationRepository
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(userId, range.from(), range.to())
                .stream().map(WaterEntryResponse::from).toList();
    }

    /**
     * Combines direct water logging with {@code fluid_ml} from nutrition entries
     * (coffee, juice, ...) — nutrition is reached through its service, never its
     * repository, per the cross-feature access rule in CLAUDE.md.
     */
    @Transactional(readOnly = true)
    public DailyHydrationResponse dailyTotal(UUID userId, LocalDate date, ZoneId zone) {
        DayRange range = DayRange.of(date, zone);
        int waterMl = hydrationRepository.sumAmountMlForUserAndRange(userId, range.from(), range.to());
        var fluidFromFood = nutritionService.totalFluidMlForDay(userId, date, zone);
        Integer goalMl = userService.getById(userId).dailyWaterGoalMl();
        return DailyHydrationResponse.of(date, waterMl, fluidFromFood, goalMl);
    }

    private WaterEntry loadOwned(UUID userId, UUID entryId) {
        return hydrationRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Water entry", entryId));
    }
}
