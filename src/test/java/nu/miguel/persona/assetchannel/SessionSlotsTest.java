package nu.miguel.persona.assetchannel;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionSlotsTest {
    private FakeAdapter adapter;
    private SessionSlots slots;
    private Player first;
    private Player second;

    @BeforeEach void setUp() {
        adapter = new FakeAdapter();
        slots = new SessionSlots(adapter);
        first = player(UUID.randomUUID());
        second = player(UUID.randomUUID());
    }

    @Test void createsAndStopsNamedSlot() {
        UUID id = start(first, "music");
        assertTrue(slots.active(first, "music"));
        assertTrue(slots.stop(first, "music"));
        assertFalse(adapter.active.contains(id));
        assertFalse(slots.active(first, "music"));
    }

    @Test void replacingSlotStopsOldSession() {
        UUID old = start(first, "default");
        UUID replacement = start(first, "default");
        assertFalse(adapter.active.contains(old));
        assertTrue(adapter.active.contains(replacement));
        assertEquals(List.of(old), adapter.stopped);
    }

    @Test void staleSessionsAreRemovedLazily() {
        UUID old = start(first, "default");
        adapter.active.remove(old);
        assertFalse(slots.active(first, "default"));
        assertFalse(slots.stop(first, "default"));
    }

    @Test void stagesChangeAndPlayersRemainIsolated() {
        UUID one = start(first, "music");
        UUID two = start(second, "music");
        adapter.stages.put(one, "calm");
        adapter.stages.put(two, "combat");
        slots.setStage(first, "music", "boss", BridgeAdapter.Transition.PLAYER_BOUNDARY);
        assertEquals("boss", slots.stage(first, "music").orElseThrow());
        assertEquals("combat", slots.stage(second, "music").orElseThrow());
        assertEquals(BridgeAdapter.Transition.PLAYER_BOUNDARY, adapter.transition);
    }

    private UUID start(Player player, String slot) {
        return slots.start(player, slot, "theme", BridgeAdapter.Origin.LISTENER, null,
                false, false, false, 1.0f, null);
    }

    private static Player player(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    static final class FakeAdapter implements BridgeAdapter {
        final Set<UUID> active = new HashSet<>();
        final Map<UUID, String> stages = new HashMap<>();
        final List<UUID> stopped = new ArrayList<>();
        Transition transition;
        public boolean available() { return true; }
        public void playSound(Player p, String i, Origin o, Entity e, SoundCategory c, float v, float pitch) {}
        public boolean stopSound(Player p, String i, SoundCategory c) { return true; }
        public void stopAllSounds(Player p) {}
        public UUID startSoundtrack(Player p, String i, Origin o, Entity e, boolean l, boolean s, boolean r, float v, String stage) {
            UUID id = UUID.randomUUID(); active.add(id); stages.put(id, stage == null ? "default" : stage); return id;
        }
        public void stopSession(UUID id) { active.remove(id); stopped.add(id); }
        public boolean sessionActive(UUID id) { return active.contains(id); }
        public Optional<String> sessionStage(UUID id) { return Optional.ofNullable(stages.get(id)); }
        public void setStage(UUID id, String stage, Transition transition) { stages.put(id, stage); this.transition = transition; }
        public Optional<String> icon(String id) { return Optional.empty(); }
    }
}
