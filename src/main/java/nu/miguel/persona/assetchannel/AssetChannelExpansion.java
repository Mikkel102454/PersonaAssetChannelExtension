package nu.miguel.persona.assetchannel;

import net.citizensnpcs.api.npc.NPC;
import nu.miguel.persona.api.ExpansionRegistrar;
import nu.miguel.persona.api.ExpansionTypes;
import nu.miguel.persona.api.PersonaContext;
import nu.miguel.persona.api.PersonaExpansion;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class AssetChannelExpansion extends PersonaExpansion {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9][a-z0-9._-]*");
    private final BridgeAdapter adapter;
    private final SessionSlots sessions;

    public AssetChannelExpansion() { this(new ReflectiveAssetChannelAdapter()); }
    AssetChannelExpansion(BridgeAdapter adapter) {
        this.adapter = adapter;
        this.sessions = new SessionSlots(adapter);
    }

    @Override public String identifier() { return "assetchannel"; }
    @Override public String author() { return "Miguel"; }
    @Override public String version() { return "2.0.0"; }

    @Override protected void registerTypes(ExpansionRegistrar registrar) {
        registrar.command("play-sound", command(AssetChannelExpansion::parsePlaySound, this::playSound));
        registrar.command("stop-sound", command(AssetChannelExpansion::parseStopSound, this::stopSound));
        registrar.command("stop-all-sounds", command(AssetChannelExpansion::copy, this::stopAllSounds));
        registrar.command("start-soundtrack", command(AssetChannelExpansion::parseStartSoundtrack, this::startSoundtrack));
        registrar.command("stop-soundtrack", command(AssetChannelExpansion::parseSlot, this::stopSoundtrack));
        registrar.command("set-stage", command(AssetChannelExpansion::parseSetStage, this::setStage));
        registrar.command("show-hud", command(AssetChannelExpansion::parseShowHud, this::showHud));
        registrar.command("update-hud", command(AssetChannelExpansion::parseUpdateHud, this::updateHud));
        registrar.command("hide-hud", command(AssetChannelExpansion::parseSlot, this::hideHud));

        registrar.condition("available", new ExpansionTypes.Condition() {
            @Override public boolean test(PersonaContext context, Map<String, Object> data) { return adapter.available(); }
        });
        registrar.condition("session-active", new ExpansionTypes.Condition() {
            @Override public Map<String, Object> parse(Map<String, Object> yaml) { return parseSlot(yaml); }
            @Override public boolean test(PersonaContext context, Map<String, Object> data) {
                try { return adapter.available() && sessions.active(context.player(), string(data, "slot")); }
                catch (RuntimeException ignored) { return false; }
            }
        });
        registrar.condition("hud-active", new ExpansionTypes.Condition() {
            @Override public Map<String,Object> parse(Map<String,Object> yaml){return parseSlot(yaml);}
            @Override public boolean test(PersonaContext context,Map<String,Object> data){try{return adapter.available()&&adapter.hudActive(context.player(),string(data,"slot"));}catch(RuntimeException ignored){return false;}}
        });

        registrar.placeholder("icon", (context, argument) -> {
            if (!validIdentifier(argument)) return "";
            try { return adapter.icon(argument).orElse(""); }
            catch (RuntimeException ignored) { return ""; }
        });
        registrar.placeholder("session-active", (context, argument) -> {
            String slot = placeholderSlot(argument);
            try { return String.valueOf(adapter.available() && sessions.active(context.player(), slot)); }
            catch (RuntimeException ignored) { return "false"; }
        });
        registrar.placeholder("session-stage", (context, argument) -> {
            String slot = placeholderSlot(argument);
            try { return adapter.available() ? sessions.stage(context.player(), slot).orElse("") : ""; }
            catch (RuntimeException ignored) { return ""; }
        });
        registrar.placeholder("hud-active",(context,argument)->{try{return String.valueOf(adapter.available()&&adapter.hudActive(context.player(),placeholderSlot(argument)));}catch(RuntimeException ignored){return "false";}});
    }

    private void playSound(PersonaContext context, Map<String, Object> data) {
        BridgeAdapter.Origin origin = origin(data);
        adapter.playSound(context.player(), string(data, "id"), origin, source(context, origin),
                SoundCategory.valueOf(string(data, "category")), number(data, "volume"), number(data, "pitch"));
    }

    private void stopSound(PersonaContext context, Map<String, Object> data) {
        SoundCategory category = data.containsKey("category")
                ? SoundCategory.valueOf(string(data, "category")) : null;
        if (!adapter.stopSound(context.player(), string(data, "id"), category))
            throw new BridgeException("unknown AssetChannel sound '" + string(data, "id") + "'");
    }

    private void stopAllSounds(PersonaContext context, Map<String, Object> data) {
        adapter.stopAllSounds(context.player());
    }

    private void startSoundtrack(PersonaContext context, Map<String, Object> data) {
        BridgeAdapter.Origin origin = origin(data);
        sessions.start(context.player(), string(data, "slot"), string(data, "id"), origin, source(context, origin),
                bool(data, "loop"), bool(data, "synchronized"), bool(data, "retain"), number(data, "volume"),
                data.containsKey("stage") ? string(data, "stage") : null);
    }

    private void stopSoundtrack(PersonaContext context, Map<String, Object> data) {
        if (!sessions.stop(context.player(), string(data, "slot")))
            throw new BridgeException("no active soundtrack in slot '" + string(data, "slot") + "'");
    }

    private void setStage(PersonaContext context, Map<String, Object> data) {
        sessions.setStage(context.player(), string(data, "slot"), string(data, "stage"),
                BridgeAdapter.Transition.valueOf(string(data, "transition")));
    }

    private void showHud(PersonaContext context,Map<String,Object> data){
        adapter.showHud(context.player(),string(data,"id"),(String)data.get("slot"),(Integer)data.get("priority"),(Boolean)data.get("resume-interrupted"),components(context,data));
    }
    private void updateHud(PersonaContext context,Map<String,Object> data){adapter.updateHud(context.player(),string(data,"slot"),components(context,data));}
    private void hideHud(PersonaContext context,Map<String,Object> data){if(!adapter.hideHud(context.player(),string(data,"slot")))throw new BridgeException("no active HUD in slot '"+string(data,"slot")+"'");}
    @SuppressWarnings("unchecked") private static Map<String,Component> components(PersonaContext context,Map<String,Object> data){
        Map<String,String> raw=(Map<String,String>)data.get("variables");Map<String,Component> out=new LinkedHashMap<>();MiniMessage mini=MiniMessage.miniMessage();
        raw.forEach((name,value)->out.put(name,mini.deserialize(context.api().resolvePlaceholders(context,value))));return Map.copyOf(out);
    }

    private static Entity source(PersonaContext context, BridgeAdapter.Origin origin) {
        if (origin == BridgeAdapter.Origin.LISTENER) return null;
        if (origin == BridgeAdapter.Origin.PLAYER) return context.player();
        NPC npc = context.npc().orElseThrow(() -> new BridgeException("NPC origin requires an active Citizens NPC"));
        if (!npc.isSpawned() || npc.getEntity() == null || !npc.getEntity().isValid())
            throw new BridgeException("NPC origin requires a spawned Citizens NPC");
        return npc.getEntity();
    }

    private ExpansionTypes.Command command(Parser parser, Executor executor) {
        return new ExpansionTypes.Command() {
            @Override public Map<String, Object> parse(Map<String, Object> yaml) { return parser.parse(yaml); }
            @Override public String validate(PersonaContext context, Map<String, Object> data) {
                if (!adapter.available()) return "AssetChannel is not installed, enabled, or API-compatible.";
                try {
                    if (data.containsKey("origin") && origin(data) == BridgeAdapter.Origin.NPC) source(context, BridgeAdapter.Origin.NPC);
                    return null;
                } catch (BridgeException error) { return error.getMessage(); }
            }
            @Override public CompletionStage<ExpansionTypes.CommandResult> execute(PersonaContext context, Map<String, Object> data) {
                try { executor.execute(context, data); return CompletableFuture.completedFuture(ExpansionTypes.CommandResult.success()); }
                catch (BridgeException error) { return CompletableFuture.completedFuture(ExpansionTypes.CommandResult.failure(error.getMessage())); }
                catch (RuntimeException error) { return CompletableFuture.failedFuture(error); }
            }
        };
    }

    static Map<String, Object> parsePlaySound(Map<String, Object> yaml) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", identifier(yaml, "id"));
        out.put("category", category(yaml.getOrDefault("category", "master")));
        out.put("volume", finiteNonNegative(yaml.getOrDefault("volume", 1.0), "volume"));
        out.put("pitch", finiteNonNegative(yaml.getOrDefault("pitch", 1.0), "pitch"));
        out.put("origin", origin(yaml.getOrDefault("origin", "listener")));
        return Map.copyOf(out);
    }

    static Map<String, Object> parseStopSound(Map<String, Object> yaml) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", identifier(yaml, "id"));
        if (yaml.containsKey("category")) out.put("category", category(yaml.get("category")));
        return Map.copyOf(out);
    }

    static Map<String, Object> parseStartSoundtrack(Map<String, Object> yaml) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", identifier(yaml, "id"));
        out.put("slot", slot(yaml.getOrDefault("slot", "default")));
        out.put("loop", booleanValue(yaml.getOrDefault("loop", false), "loop"));
        out.put("synchronized", booleanValue(yaml.getOrDefault("synchronized", false), "synchronized"));
        out.put("retain", booleanValue(yaml.getOrDefault("retain", false), "retain"));
        out.put("volume", finiteNonNegative(yaml.getOrDefault("volume", 1.0), "volume"));
        out.put("origin", origin(yaml.getOrDefault("origin", "listener")));
        if (yaml.containsKey("stage")) out.put("stage", identifier(yaml, "stage"));
        return Map.copyOf(out);
    }

    static Map<String, Object> parseSetStage(Map<String, Object> yaml) {
        Map<String, Object> out = new HashMap<>();
        out.put("stage", identifier(yaml, "stage"));
        out.put("slot", slot(yaml.getOrDefault("slot", "default")));
        String transition = String.valueOf(yaml.getOrDefault("transition", "immediate")).toLowerCase(Locale.ROOT);
        out.put("transition", switch (transition) {
            case "immediate" -> "IMMEDIATE";
            case "session-boundary" -> "SESSION_BOUNDARY";
            case "player-boundary" -> "PLAYER_BOUNDARY";
            default -> throw new IllegalArgumentException("transition must be immediate, session-boundary, or player-boundary");
        });
        return Map.copyOf(out);
    }

    static Map<String,Object> parseShowHud(Map<String,Object> yaml){
        Map<String,Object> out=new HashMap<>();out.put("id",identifier(yaml,"id"));out.put("variables",variables(yaml,false));
        if(yaml.containsKey("slot"))out.put("slot",slot(yaml.get("slot")));
        if(yaml.containsKey("priority")){Object value=yaml.get("priority");try{out.put("priority",value instanceof Number n?n.intValue():Integer.parseInt(String.valueOf(value)));}catch(NumberFormatException e){throw new IllegalArgumentException("priority must be an integer");}}
        if(yaml.containsKey("resume-interrupted"))out.put("resume-interrupted",booleanValue(yaml.get("resume-interrupted"),"resume-interrupted"));
        return Map.copyOf(out);
    }
    static Map<String,Object> parseUpdateHud(Map<String,Object> yaml){return Map.of("slot",slot(yaml.getOrDefault("slot","default")),"variables",variables(yaml,true));}
    private static Map<String,String> variables(Map<String,Object> yaml,boolean required){
        Object raw=yaml.get("variables");if(raw==null&&!required)return Map.of();
        if(!(raw instanceof Map<?,?> values)||required&&values.isEmpty())throw new IllegalArgumentException("variables must be a non-empty map");
        Map<String,String> out=new LinkedHashMap<>();for(var entry:values.entrySet()){String name=String.valueOf(entry.getKey());if(!name.matches("[a-zA-Z][a-zA-Z0-9_.-]*"))throw new IllegalArgumentException("invalid variable name '"+name+"'");out.put(name,String.valueOf(entry.getValue()));}return Map.copyOf(out);
    }

    static Map<String, Object> parseSlot(Map<String, Object> yaml) {
        return Map.of("slot", slot(yaml.getOrDefault("slot", "default")));
    }

    private static Map<String, Object> copy(Map<String, Object> yaml) { return Map.copyOf(yaml); }
    private static String identifier(Map<String, Object> yaml, String field) {
        Object value = yaml.get(field);
        if (value == null || String.valueOf(value).isBlank()) throw new IllegalArgumentException("missing " + field);
        String id = String.valueOf(value);
        if (!validIdentifier(id)) throw new IllegalArgumentException(field + " must match " + IDENTIFIER.pattern());
        return id;
    }
    private static boolean validIdentifier(String value) { return value != null && IDENTIFIER.matcher(value).matches(); }
    private static String slot(Object value) {
        String slot = String.valueOf(value);
        if (!validIdentifier(slot)) throw new IllegalArgumentException("slot must match " + IDENTIFIER.pattern());
        return slot;
    }
    private static String category(Object value) {
        String raw = String.valueOf(value);
        if (!raw.equals(raw.toLowerCase(Locale.ROOT)) || raw.contains("_"))
            throw new IllegalArgumentException("sound category must be lowercase kebab-case");
        String category = raw.toUpperCase(Locale.ROOT).replace('-', '_');
        try { return SoundCategory.valueOf(category).name(); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("invalid sound category '" + value + "'"); }
    }
    private static String origin(Object value) {
        String origin = String.valueOf(value).toLowerCase(Locale.ROOT);
        if (!Set.of("listener", "player", "npc").contains(origin))
            throw new IllegalArgumentException("origin must be listener, player, or npc");
        return origin.toUpperCase(Locale.ROOT);
    }
    private static BridgeAdapter.Origin origin(Map<String, Object> data) {
        return BridgeAdapter.Origin.valueOf(string(data, "origin"));
    }
    private static float finiteNonNegative(Object value, String field) {
        final float parsed;
        try { parsed = value instanceof Number number ? number.floatValue() : Float.parseFloat(String.valueOf(value)); }
        catch (NumberFormatException error) { throw new IllegalArgumentException(field + " must be a number"); }
        if (!Float.isFinite(parsed) || parsed < 0) throw new IllegalArgumentException(field + " must be finite and non-negative");
        return parsed;
    }
    private static boolean booleanValue(Object value, String field) {
        if (value instanceof Boolean bool) return bool;
        String text = String.valueOf(value).toLowerCase(Locale.ROOT);
        if (text.equals("true") || text.equals("false")) return Boolean.parseBoolean(text);
        throw new IllegalArgumentException(field + " must be true or false");
    }
    private static String placeholderSlot(String argument) {
        return argument == null || argument.isBlank() ? "default" : slot(argument);
    }
    private static String string(Map<String, Object> data, String key) { return String.valueOf(data.get(key)); }
    private static float number(Map<String, Object> data, String key) { return ((Number) data.get(key)).floatValue(); }
    private static boolean bool(Map<String, Object> data, String key) { return (Boolean) data.get(key); }

    @FunctionalInterface private interface Parser { Map<String, Object> parse(Map<String, Object> yaml); }
    @FunctionalInterface private interface Executor { void execute(PersonaContext context, Map<String, Object> data); }
}
