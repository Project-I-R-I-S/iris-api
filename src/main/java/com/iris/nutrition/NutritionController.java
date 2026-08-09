package com.iris.nutrition;

import com.iris.common.security.AuthenticatedUser;
import com.iris.nutrition.dto.FoodEntryRequest;
import com.iris.nutrition.dto.FoodEntryResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nutrition/entries")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @PostMapping
    public ResponseEntity<FoodEntryResponse> create(@RequestBody @Valid FoodEntryRequest req) {
        var created = nutritionService.create(AuthenticatedUser.currentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<FoodEntryResponse> listForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "Asia/Kolkata") String timezone
    ) {
        return nutritionService.listForDay(
                AuthenticatedUser.currentUserId(), date, ZoneId.of(timezone));
    }

    @PutMapping("/{id}")
    public FoodEntryResponse update(@PathVariable UUID id, @RequestBody @Valid FoodEntryRequest req) {
        return nutritionService.update(AuthenticatedUser.currentUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        nutritionService.delete(AuthenticatedUser.currentUserId(), id);
    }
}
