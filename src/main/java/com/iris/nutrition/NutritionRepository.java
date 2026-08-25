package com.iris.nutrition;

import com.iris.common.repository.UserScopedRepository;
import com.iris.nutrition.model.FoodEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NutritionRepository extends UserScopedRepository<FoodEntry, UUID> {

    List<FoodEntry> findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
            UUID userId, Instant from, Instant to);

    @Query("""
            select coalesce(sum(f.fluidMl), 0) from FoodEntry f
            where f.userId = :userId and f.fluidMl is not null
              and f.consumedAt >= :from and f.consumedAt < :to
            """)
    BigDecimal sumFluidMlForUserAndRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);
}
