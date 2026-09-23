package dev.darkblade.pirt.core.provider;

/**
 * Marker interface for all providers that supply data in response to queries.
 */
public interface QueryProvider {

    /**
     * @return the unique provider identifier (e.g. "player.health", "players.count")
     */
    String id();
}
