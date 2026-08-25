package com.iris.weight;

import com.iris.common.repository.UserScopedRepository;
import com.iris.weight.model.WeightEntry;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeightRepository extends UserScopedRepository<WeightEntry, UUID> {

    List<WeightEntry> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            UUID userId, Instant from, Instant to);

    Optional<WeightEntry> findFirstByUserIdOrderByRecordedAtDesc(UUID userId);
}
