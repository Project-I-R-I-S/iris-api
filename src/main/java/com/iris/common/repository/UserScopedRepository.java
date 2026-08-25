package com.iris.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository for entities owned by a single user (a {@code user_id} column).
 * Extend this instead of {@link JpaRepository} directly to get ownership-scoped
 * lookup for free — every feature that stores per-user data (nutrition, hydration,
 * weight, sleep, ...) needs the "load this row only if it belongs to this user"
 * query, so it lives here once rather than being redeclared per feature.
 */
@NoRepositoryBean
public interface UserScopedRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    /**
     * Loads a row by id, scoped to the owning user. Use this (never plain
     * {@code findById}) whenever a user-supplied id is being resolved, so a user
     * can never read or mutate another user's row by guessing an id.
     */
    Optional<T> findByIdAndUserId(ID id, UUID userId);
}
