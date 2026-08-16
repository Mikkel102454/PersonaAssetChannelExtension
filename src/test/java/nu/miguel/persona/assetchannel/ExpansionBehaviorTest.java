package nu.miguel.persona.assetchannel;

import nu.miguel.persona.api.*;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpansionBehaviorTest {
    @Test void iconPlaceholderReturnsMiniMessageFontTagAndMissingFallback() {
        StubAdapter adapter = new StubAdapter();
        adapter.icon = Optional.of("<font:assetchannel:icons>\uE000</font>");
        CapturingRegistrar registrar = register(adapter);
        assertEquals("<font:assetchannel:icons>\uE000</font>", registrar.placeholders.get("icon").resolve(context(), "quest"));
        adapter.icon = Optional.empty();
        assertEquals("", registrar.placeholders.get("icon").resolve(context(), "missing"));
        assertEquals("", registrar.placeholders.get("icon").resolve(context(), "Bad ID"));
    }

    @Test void unavailableConditionFailsClosed() {
        StubAdapter adapter = new StubAdapter();
        adapter.available = false;
        CapturingRegistrar registrar = register(adapter);
        assertFalse(registrar.conditions.get("available").test(context(), Map.of()));
        assertFalse(registrar.conditions.get("session-active").test(context(), Map.of("slot", "default")));
    }

    @Test void commandsExposeValidationSuccessAndUsefulFailureResults() throws Exception {
        StubAdapter adapter = new StubAdapter();
        CapturingRegistrar registrar = register(adapter);
        ExpansionTypes.Command play = registrar.commands.get("play-sound");
        Map<String,Object> playData = play.parse(Map.of("id", "ui.click"));
        assertNull(play.validate(context(), playData));
        assertEquals(ExpansionTypes.CommandResult.Kind.SUCCESS,
                play.execute(context(), playData).toCompletableFuture().get().kind());

        ExpansionTypes.Command stop = registrar.commands.get("stop-sound");
        ExpansionTypes.CommandResult failure = stop.execute(context(), stop.parse(Map.of("id", "missing")))
                .toCompletableFuture().get();
        assertEquals(ExpansionTypes.CommandResult.Kind.FAILURE, failure.kind());
        assertTrue(failure.message().contains("unknown AssetChannel sound"));

        adapter.available = false;
        assertTrue(play.validate(context(), playData).contains("not installed"));
    }

    @Test void allKnownPlaybackAndStageFailuresHaveClearMappings() {
        assertMessage("unknown AssetChannel sound 'missing'", "sound 'missing'", "UNKNOWN_ASSET");
        assertMessage("pack is not ready", "sound 'theme'", "PACK_NOT_READY");
        assertMessage("origin is unavailable", "sound 'theme'", "SOURCE_UNAVAILABLE");
        assertMessage("unknown AssetChannel stage 'boss'", "stage 'boss'", "UNKNOWN_STAGE");
        assertMessage("session is inactive", "stage 'boss'", "INACTIVE");
        assertMessage("unsupported result", "sound 'theme'", "SOMETHING_NEW");
    }

    private static void assertMessage(String expected, String target, String result) {
        assertTrue(ReflectiveAssetChannelAdapter.resultFailure(target, result).getMessage().contains(expected));
    }

    private static CapturingRegistrar register(StubAdapter adapter) {
        CapturingRegistrar registrar = new CapturingRegistrar();
        new AssetChannelExpansion(adapter).registerTypes(registrar);
        return registrar;
    }

    private static PersonaContext context() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return new PersonaContext(player, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), 0, 0, null, null, null, null);
    }

    static final class CapturingRegistrar implements ExpansionRegistrar {
        final Map<String, ExpansionTypes.Condition> conditions = new HashMap<>();
        final Map<String, ExpansionTypes.Placeholder> placeholders = new HashMap<>();
        final Map<String, ExpansionTypes.Command> commands = new HashMap<>();
        public void condition(String n, ExpansionTypes.Condition h) { conditions.put(n, h); }
        public void command(String n, ExpansionTypes.Command h) { commands.put(n, h); }
        public void placeholder(String n, ExpansionTypes.Placeholder h) { placeholders.put(n, h); }
        public void objective(String n, ExpansionTypes.Objective h) {}
    }

    static final class StubAdapter implements BridgeAdapter {
        boolean available = true;
        Optional<String> icon = Optional.empty();
        public boolean available() { return available; }
        public Optional<String> icon(String id) { return icon; }
        public void playSound(Player p, String i, Origin o, Entity e, SoundCategory c, float v, float pitch) {}
        public boolean stopSound(Player p, String i, SoundCategory c) { return false; }
        public void stopAllSounds(Player p) {}
        public UUID startSoundtrack(Player p, String i, Origin o, Entity e, boolean l, boolean s, boolean r, float v, String stage) { return UUID.randomUUID(); }
        public void stopSession(UUID id) {}
        public boolean sessionActive(UUID id) { return false; }
        public Optional<String> sessionStage(UUID id) { return Optional.empty(); }
        public void setStage(UUID id, String stage, Transition transition) {}
    }
}
