package nu.miguel.persona.assetchannel;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class ReflectiveAssetChannelAdapter implements BridgeAdapter {
    private static final String PLUGIN_NAME = "AssetChannel";
    private volatile Binding cached;
    private volatile String discoveryError;

    @Override public boolean available() {
        try { binding(); return true; }
        catch (BridgeException ignored) { return false; }
    }

    @Override public void playSound(Player player, String id, Origin origin, Entity source,
                                    SoundCategory category, float volume, float pitch) {
        Binding b = binding();
        Object result = call(b.playSound, b.api, player, id, b.origin(origin, source), category, volume, pitch);
        String name = enumName(result);
        if (!name.equals("PLAYED")) throw resultFailure("sound '" + id + "'", name);
    }

    @Override public boolean stopSound(Player player, String id, SoundCategory category) {
        Binding b = binding();
        Object result = category == null
                ? call(b.stopSoundAllCategories, b.api, player, id)
                : call(b.stopSoundCategory, b.api, player, id, category);
        return Boolean.TRUE.equals(result);
    }

    @Override public void stopAllSounds(Player player) {
        Binding b = binding();
        call(b.stopAllSounds, b.api, player);
    }

    @Override public UUID startSoundtrack(Player player, String id, Origin origin, Entity source,
                                           boolean loop, boolean synchronizedPlayback, boolean retain,
                                           float volume, String stage) {
        Binding b = binding();
        Object settings = construct(b.sessionSettings, loop, synchronizedPlayback, retain, volume, stage,
                b.origin(origin, source));
        Object session = call(b.startSoundtrack, b.api, id, List.of(player), settings);
        return (UUID) call(b.sessionId, session);
    }

    @Override public void stopSession(UUID id) {
        Binding b = binding();
        Object session = b.session(id).orElseThrow(() -> new BridgeException("soundtrack session is inactive"));
        call(b.sessionStop, session);
    }

    @Override public boolean sessionActive(UUID id) {
        Binding b = binding();
        Optional<?> session = b.session(id);
        return session.isPresent() && Boolean.TRUE.equals(call(b.sessionActive, session.get()));
    }

    @Override public Optional<String> sessionStage(UUID id) {
        Binding b = binding();
        Optional<?> session = b.session(id);
        if (session.isEmpty() || !Boolean.TRUE.equals(call(b.sessionActive, session.get()))) return Optional.empty();
        return Optional.ofNullable((String) call(b.sessionStage, session.get()));
    }

    @Override public void setStage(UUID id, String stage, Transition transition) {
        Binding b = binding();
        Object session = b.session(id).orElseThrow(() -> new BridgeException("soundtrack session is inactive"));
        Object settings = call(switch (transition) {
            case IMMEDIATE -> b.transitionImmediate;
            case SESSION_BOUNDARY -> b.transitionSessionBoundary;
            case PLAYER_BOUNDARY -> b.transitionPlayerBoundary;
        }, null);
        String result = enumName(call(b.sessionSetStage, session, stage, settings));
        if (!result.equals("APPLIED") && !result.equals("SCHEDULED") && !result.equals("ALREADY_ACTIVE"))
            throw resultFailure("stage '" + stage + "'", result);
    }

    @Override public Optional<String> icon(String id) {
        Binding b = binding();
        Optional<?> icon = optional(call(b.icon, b.api, id));
        if (icon.isEmpty()) return Optional.empty();
        Object value = icon.get();
        String character = String.valueOf(call(b.iconCharacter, value));
        Object font = call(b.iconFont, value);
        String key;
        try { key = String.valueOf(font.getClass().getMethod("asString").invoke(font)); }
        catch (ReflectiveOperationException e) { key = font.toString(); }
        return Optional.of("<font:" + key + ">" + character + "</font>");
    }

    private Binding binding() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        Binding current = cached;
        if (plugin == null || !plugin.isEnabled()) {
            cached = null;
            discoveryError = "AssetChannel is not installed or enabled";
            throw new BridgeException(discoveryError);
        }
        if (current != null && current.plugin == plugin && current.plugin.isEnabled()) return current;
        try {
            Binding created = new Binding(plugin);
            cached = created;
            discoveryError = null;
            return created;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            cached = null;
            discoveryError = "AssetChannel API is incompatible with this extension";
            throw new BridgeException(discoveryError + ": " + concise(e), e);
        }
    }

    static BridgeException resultFailure(String target, String result) {
        return switch (result) {
            case "UNKNOWN_ASSET" -> new BridgeException("unknown AssetChannel " + target);
            case "PACK_NOT_READY" -> new BridgeException("AssetChannel pack is not ready for " + target);
            case "SOURCE_UNAVAILABLE" -> new BridgeException("playback origin is unavailable for " + target);
            case "UNKNOWN_STAGE" -> new BridgeException("unknown AssetChannel " + target);
            case "INACTIVE" -> new BridgeException("soundtrack session is inactive");
            default -> new BridgeException("AssetChannel returned unsupported result " + result + " for " + target);
        };
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> e ? e.name() : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Optional<?> optional(Object value) {
        if (value instanceof Optional<?> optional) return optional;
        throw new BridgeException("AssetChannel API is incompatible: expected Optional result");
    }

    private static Object call(Method method, Object target, Object... arguments) {
        try { return method.invoke(target, arguments); }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof BridgeException bridge) throw bridge;
            throw new BridgeException("AssetChannel call failed: " + concise(cause), cause);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new BridgeException("AssetChannel API is incompatible: " + concise(e), e);
        }
    }

    private static Object construct(Constructor<?> constructor, Object... arguments) {
        try { return constructor.newInstance(arguments); }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new BridgeException("AssetChannel settings were rejected: " + concise(cause), cause);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new BridgeException("AssetChannel API is incompatible: " + concise(e), e);
        }
    }

    private static String concise(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private static final class Binding {
        final Plugin plugin;
        final Object api;
        final Class<?> originClass;
        final Method originListener, originEntity;
        final Method playSound, stopSoundAllCategories, stopSoundCategory, stopAllSounds, startSoundtrack, session, icon;
        final Constructor<?> sessionSettings;
        final Method sessionId, sessionActive, sessionStage, sessionStop, sessionSetStage;
        final Method transitionImmediate, transitionSessionBoundary, transitionPlayerBoundary;
        final Method iconCharacter, iconFont;

        @SuppressWarnings({"unchecked", "rawtypes"})
        Binding(Plugin plugin) throws ReflectiveOperationException {
            this.plugin = plugin;
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("nu.miguel.assetChannel.api.AssetChannelApi", false, loader);
            originClass = Class.forName("nu.miguel.assetChannel.api.SoundOrigin", false, loader);
            Class<?> settingsClass = Class.forName("nu.miguel.assetChannel.api.SessionSettings", false, loader);
            Class<?> musicSessionClass = Class.forName("nu.miguel.assetChannel.api.MusicSession", false, loader);
            Class<?> transitionClass = Class.forName("nu.miguel.assetChannel.api.StageTransitionSettings", false, loader);
            Class<?> iconClass = Class.forName("nu.miguel.assetChannel.api.IconAsset", false, loader);
            api = Bukkit.getServicesManager().load((Class) apiClass);
            if (api == null) throw new IllegalStateException("AssetChannel API service is not registered");

            originListener = originClass.getMethod("listenerRelative");
            originEntity = originClass.getMethod("entity", Entity.class);
            playSound = apiClass.getMethod("playSound", Player.class, String.class, originClass,
                    SoundCategory.class, float.class, float.class);
            stopSoundAllCategories = apiClass.getMethod("stopSound", Player.class, String.class);
            stopSoundCategory = apiClass.getMethod("stopSound", Player.class, String.class, SoundCategory.class);
            stopAllSounds = apiClass.getMethod("stopAllSounds", Player.class);
            startSoundtrack = apiClass.getMethod("startSoundtrack", String.class, Collection.class, settingsClass);
            session = apiClass.getMethod("session", UUID.class);
            icon = apiClass.getMethod("icon", String.class);
            sessionSettings = settingsClass.getConstructor(boolean.class, boolean.class, boolean.class,
                    float.class, String.class, originClass);
            sessionId = musicSessionClass.getMethod("id");
            sessionActive = musicSessionClass.getMethod("active");
            sessionStage = musicSessionClass.getMethod("stage");
            sessionStop = musicSessionClass.getMethod("stop");
            sessionSetStage = musicSessionClass.getMethod("setStage", String.class, transitionClass);
            transitionImmediate = transitionClass.getMethod("immediate");
            transitionSessionBoundary = transitionClass.getMethod("nextSessionBoundary");
            transitionPlayerBoundary = transitionClass.getMethod("nextPlayerBoundary");
            iconCharacter = iconClass.getMethod("character");
            iconFont = iconClass.getMethod("font");
        }

        Object origin(Origin origin, Entity source) {
            return switch (origin) {
                case LISTENER -> call(originListener, null);
                case PLAYER, NPC -> {
                    if (source == null || !source.isValid()) throw new BridgeException("playback origin entity is unavailable");
                    yield call(originEntity, null, source);
                }
            };
        }

        Optional<?> session(UUID id) { return optional(call(session, api, id)); }
    }
}
