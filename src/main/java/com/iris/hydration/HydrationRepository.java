package com.iris.hydration;

import com.iris.common.repository.UserScopedRepository;
import com.iris.hydration.model.WaterEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HydrationRepository extends UserScopedRepository<WaterEntry, UUID> {

    List<WaterEntry> findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
            UUID userId, Instant from, Instant to);

    @Query("""
            select coalesce(sum(w.amountMl), 0) from WaterEntry w
            where w.userId = :userId and w.consumedAt >= :from and w.consumedAt < :to
            """)
    int sumAmountMlForUserAndRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);
}
