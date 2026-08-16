package nu.miguel.persona.assetchannel;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class SessionSlots {
    private final BridgeAdapter adapter;
    private final Map<UUID, Map<String, UUID>> players = new HashMap<>();

    SessionSlots(BridgeAdapter adapter) { this.adapter = adapter; }

    UUID start(Player player, String slot, String asset, BridgeAdapter.Origin origin, Entity source,
               boolean loop, boolean synchronizedPlayback, boolean retain, float volume, String stage) {
        Map<String, UUID> slots = players.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        UUID old = slots.remove(slot);
        if (old != null && adapter.sessionActive(old)) adapter.stopSession(old);
        UUID created = adapter.startSoundtrack(player, asset, origin, source, loop, synchronizedPlayback, retain, volume, stage);
        slots.put(slot, created);
        return created;
    }

    boolean stop(Player player, String slot) {
        UUID id = remove(player.getUniqueId(), slot);
        if (id == null || !adapter.sessionActive(id)) return false;
        adapter.stopSession(id);
        return true;
    }

    boolean active(Player player, String slot) {
        UUID id = get(player.getUniqueId(), slot);
        if (id == null) return false;
        if (adapter.sessionActive(id)) return true;
        remove(player.getUniqueId(), slot);
        return false;
    }

    Optional<String> stage(Player player, String slot) {
        UUID id = get(player.getUniqueId(), slot);
        if (id == null) return Optional.empty();
        if (!adapter.sessionActive(id)) {
            remove(player.getUniqueId(), slot);
            return Optional.empty();
        }
        return adapter.sessionStage(id);
    }

    void setStage(Player player, String slot, String stage, BridgeAdapter.Transition transition) {
        UUID id = get(player.getUniqueId(), slot);
        if (id == null || !adapter.sessionActive(id)) {
            remove(player.getUniqueId(), slot);
            throw new BridgeException("no active soundtrack in slot '" + slot + "'");
        }
        adapter.setStage(id, stage, transition);
    }

    private UUID get(UUID player, String slot) {
        Map<String, UUID> slots = players.get(player);
        return slots == null ? null : slots.get(slot);
    }

    private UUID remove(UUID player, String slot) {
        Map<String, UUID> slots = players.get(player);
        if (slots == null) return null;
        UUID removed = slots.remove(slot);
        if (slots.isEmpty()) players.remove(player);
        return removed;
    }
}
