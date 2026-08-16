package nu.miguel.persona.assetchannel;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParsingTest {
    @Test void playSoundDefaultsAndNormalizes() {
        Map<String, Object> parsed = AssetChannelExpansion.parsePlaySound(Map.of("id", "ui.click"));
        assertEquals("MASTER", parsed.get("category"));
        assertEquals("LISTENER", parsed.get("origin"));
        assertEquals(1.0f, parsed.get("volume"));
        assertEquals(1.0f, parsed.get("pitch"));
    }

    @Test void rejectsMissingAndInvalidAssetIds() {
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of("id", "Bad ID")));
    }

    @Test void rejectsInvalidCategoryAndOrigin() {
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of("id", "ok", "category", "music-ish")));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of("id", "ok", "category", "MASTER")));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of("id", "ok", "origin", "world")));
    }

    @Test void rejectsNegativeAndNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of("id", "ok", "volume", -0.1)));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parsePlaySound(Map.of("id", "ok", "pitch", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parseStartSoundtrack(Map.of("id", "ok", "volume", Double.POSITIVE_INFINITY)));
    }

    @Test void validatesSlotsStagesBooleansAndTransitions() {
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parseSlot(Map.of("slot", "bad slot")));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parseStartSoundtrack(Map.of("id", "ok", "stage", "Bad Stage")));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parseStartSoundtrack(Map.of("id", "ok", "loop", "sometimes")));
        assertThrows(IllegalArgumentException.class, () -> AssetChannelExpansion.parseSetStage(Map.of("stage", "combat", "transition", "later")));
        assertEquals("SESSION_BOUNDARY", AssetChannelExpansion.parseSetStage(
                Map.of("stage", "combat", "transition", "session-boundary")).get("transition"));
    }

    @Test void parsesGenericHudRequestsAndRejectsBadVariables() {
        Map<String,Object> parsed=AssetChannelExpansion.parseShowHud(Map.of("id","notification.banner","slot","quest","priority",50,
                "resume-interrupted",true,"variables",Map.of("heading","Quest Started","body","<quest-title>")));
        assertEquals(50,parsed.get("priority"));assertEquals("quest",parsed.get("slot"));
        assertEquals(Map.of(),AssetChannelExpansion.parseShowHud(Map.of("id","banner")).get("variables"));
        assertThrows(IllegalArgumentException.class,()->AssetChannelExpansion.parseUpdateHud(Map.of("variables",Map.of("bad name","x"))));
    }
}
