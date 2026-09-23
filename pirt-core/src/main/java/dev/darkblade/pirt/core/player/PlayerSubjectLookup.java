package dev.darkblade.pirt.core.player;

import java.util.Optional;
import java.util.UUID;

/**
 * Functional interface for resolving a PlayerSubject by UUID.
 */
@FunctionalInterface
public interface PlayerSubjectLookup {

    /**
     * Finds a player subject by unique identifier.
     *
     * @param id the player UUID
     * @return an Optional containing the PlayerSubject if online, or empty
     */
    Optional<PlayerSubject> find(UUID id);

    /**
     * Resolves a player subject by username if supported by the platform.
     *
     * @param name player name (case-insensitive)
     * @return an Optional containing the PlayerSubject if online, or empty
     */
    default Optional<PlayerSubject> findByName(String name) {
        return Optional.empty();
    }
}
