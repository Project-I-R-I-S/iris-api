package com.iris.weight;

import com.iris.common.exception.ResourceNotFoundException;
import com.iris.common.util.DayRange;
import com.iris.weight.dto.WeightEntryRequest;
import com.iris.weight.dto.WeightEntryResponse;
import com.iris.weight.model.WeightEntry;
import com.iris.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class WeightService {

    private static final int BMI_SCALE = 1;

    private final WeightRepository weightRepository;
    private final UserService userService;

    public WeightService(WeightRepository weightRepository, UserService userService) {
        this.weightRepository = weightRepository;
        this.userService = userService;
    }

    @Transactional
    public WeightEntryResponse create(UUID userId, WeightEntryRequest req) {
        WeightEntry entry = WeightEntry.builder()
                .userId(userId)
                .weightKg(req.weightKg())
                .recordedAt(req.recordedAt())
                .notes(req.notes())
                .build();
        entry = weightRepository.save(entry);
        return toResponse(entry, heightCm(userId));
    }

    @Transactional
    public WeightEntryResponse update(UUID userId, UUID entryId, WeightEntryRequest req) {
        WeightEntry entry = loadOwned(userId, entryId);
        entry.setWeightKg(req.weightKg());
        entry.setRecordedAt(req.recordedAt());
        entry.setNotes(req.notes());
        return toResponse(entry, heightCm(userId));
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        weightRepository.delete(loadOwned(userId, entryId));
    }

    @Transactional(readOnly = true)
    public List<WeightEntryResponse> listInRange(UUID userId, LocalDate from, LocalDate to, ZoneId zone) {
        Double heightCm = heightCm(userId);
        var fromInstant = DayRange.of(from, zone).from();
        var toInstant = DayRange.of(to, zone).to();
        return weightRepository
                .findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(userId, fromInstant, toInstant)
                .stream().map(e -> toResponse(e, heightCm)).toList();
    }

    @Transactional(readOnly = true)
    public WeightEntryResponse latest(UUID userId) {
        return weightRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)
                .map(entry -> toResponse(entry, heightCm(userId)))
                .orElse(null);
    }

    private Double heightCm(UUID userId) {
        return userService.getById(userId).heightCm();
    }

    /** BMI = weightKg / heightM^2. Null when the user hasn't set a height yet. */
    private WeightEntryResponse toResponse(WeightEntry entry, Double heightCm) {
        return WeightEntryResponse.from(entry, calculateBmi(entry.getWeightKg(), heightCm));
    }

    private BigDecimal calculateBmi(BigDecimal weightKg, Double heightCm) {
        if (heightCm == null || heightCm <= 0) {
            return null;
        }
        BigDecimal heightM = BigDecimal.valueOf(heightCm).divide(BigDecimal.valueOf(100));
        return weightKg.divide(heightM.multiply(heightM), BMI_SCALE, RoundingMode.HALF_UP);
    }

    private WeightEntry loadOwned(UUID userId, UUID entryId) {
        return weightRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Weight entry", entryId));
    }
}
