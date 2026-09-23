package dev.darkblade.pirt.platform.paper.player;

import dev.darkblade.pirt.core.player.PlayerSubject;
import dev.darkblade.pirt.core.player.PlayerSubjectLookup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves Bukkit Players as PlayerSubjects.
 */
public class PaperPlayerLookup implements PlayerSubjectLookup {

    @Override
    public Optional<PlayerSubject> find(UUID id) {
        if (id == null) return Optional.empty();
        Player player = Bukkit.getPlayer(id);
        if (player != null && player.isOnline()) {
            return Optional.of(new PaperPlayerSubject(player));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlayerSubject> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        Player player = Bukkit.getPlayerExact(name);
        if (player != null && player.isOnline()) {
            return Optional.of(new PaperPlayerSubject(player));
        }
        return Optional.empty();
    }
}
