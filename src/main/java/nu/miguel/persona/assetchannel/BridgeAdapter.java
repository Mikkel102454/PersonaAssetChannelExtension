package nu.miguel.persona.assetchannel;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

interface BridgeAdapter {
    enum Origin { LISTENER, PLAYER, NPC }
    enum Transition { IMMEDIATE, SESSION_BOUNDARY, PLAYER_BOUNDARY }

    boolean available();
    void playSound(Player player, String id, Origin origin, Entity source, SoundCategory category, float volume, float pitch);
    boolean stopSound(Player player, String id, SoundCategory category);
    void stopAllSounds(Player player);
    UUID startSoundtrack(Player player, String id, Origin origin, Entity source, boolean loop,
                         boolean synchronizedPlayback, boolean retain, float volume, String stage);
    void stopSession(UUID id);
    boolean sessionActive(UUID id);
    Optional<String> sessionStage(UUID id);
    void setStage(UUID id, String stage, Transition transition);
    Optional<String> icon(String id);
}
