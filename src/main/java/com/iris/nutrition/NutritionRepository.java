package com.iris.nutrition;

import com.iris.nutrition.model.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NutritionRepository extends JpaRepository<FoodEntry, UUID> {

    List<FoodEntry> findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
            UUID userId, Instant from, Instant to);

    Optional<FoodEntry> findByIdAndUserId(UUID id, UUID userId);
}
