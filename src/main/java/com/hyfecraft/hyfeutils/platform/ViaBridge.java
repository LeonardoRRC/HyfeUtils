package com.hyfecraft.hyfeutils.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional bridge to ViaVersion 4.x and 5.x, resolved once by reflection.
 *
 * <p>An old server (for example 1.8.8) cannot carry RGB colors or boss bar styles in its own
 * packets: its chat components have no hex colors and it has no boss bar packet at all. When
 * ViaVersion runs on that server, this bridge writes those packets already in the newer format
 * and injects them into the ViaVersion pipeline right after the protocol that introduced the
 * feature, so the rest of the pipeline converts them for the actual client version.</p>
 *
 * <p>Adventure ships a similar bridge, but it only enables itself for ViaVersion 4 and therefore
 * silently falls back to downsampled colors and wither boss bars with ViaVersion 5.</p>
 */
public final class ViaBridge {
    public static final int PROTOCOL_1_9 = 107;
    public static final int PROTOCOL_1_16 = 735;

    private static final String BASE = "com.viaversion.viaversion.";
    private static final UUID NIL = new UUID(0L, 0L);
    private static final GsonComponentSerializer RGB_GSON = GsonComponentSerializer.gson();
    private static final GsonComponentSerializer LEGACY_GSON = GsonComponentSerializer.colorDownsamplingGson();

    private final Logger logger;
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private final Map<UUID, Target> targets = new ConcurrentHashMap<>();
    private final Handles handles;

    private ViaBridge(Logger logger, Handles handles) {
        this.logger = logger;
        this.handles = handles;
    }

    /** Creates the bridge. It is always safe to call: without ViaVersion the bridge is disabled. */
    public static ViaBridge create(Logger logger) {
        Handles handles;
        try {
            handles = Handles.resolve();
        } catch (Throwable ignored) {
            handles = null;
        }
        return new ViaBridge(logger, handles);
    }

    /** Whether ViaVersion is installed on this server and its API could be resolved. */
    public boolean isPresent() {
        return handles != null;
    }

    /** Client protocol version reported by ViaVersion, or {@code -1} when it is unknown. */
    public int protocol(Player player) {
        if (handles == null || player == null) {
            return -1;
        }
        try {
            return (int) handles.playerVersion.invoke(handles.api, player.getUniqueId());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /** Whether the client can display RGB colors (1.16+). */
    public boolean supportsRgb(Player player) {
        return protocol(player) >= PROTOCOL_1_16;
    }

    /** Whether this bridge will deliver chat, titles and tab lists to the player itself. */
    public boolean handlesText(Player player) {
        Target target = target(player);
        return target != null && target.modern;
    }

    /** Whether this bridge will deliver boss bars to the player itself. */
    public boolean handlesBossBars(Player player) {
        Target target = target(player);
        return target != null && (target.modern || target.bossBarOnly);
    }

    /** Forgets cached data about a player. Called when the player leaves. */
    public void forget(UUID playerId) {
        targets.remove(playerId);
    }

    // ------------------------------------------------------------------
    //  Packets. Every method returns false when the caller must use the normal path.
    // ------------------------------------------------------------------

    boolean sendChat(Player player, Component message, byte position) {
        Target target = target(player);
        if (target == null || !target.modern) {
            return false;
        }
        Route route = handles.modern;
        return send(target, route, route.chat, wrapper -> {
            write(wrapper, handles.componentType, json(RGB_GSON, message));
            write(wrapper, handles.byteType, position);
            write(wrapper, handles.uuidType, NIL);
        });
    }

    boolean sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Target target = target(player);
        if (target == null || !target.modern) {
            return false;
        }
        Route route = handles.modern;
        return send(target, route, route.title, wrapper -> {
            write(wrapper, handles.varIntType, 3);
            write(wrapper, handles.intType, fadeIn);
            write(wrapper, handles.intType, stay);
            write(wrapper, handles.intType, fadeOut);
        }) && send(target, route, route.title, wrapper -> {
            write(wrapper, handles.varIntType, 1);
            write(wrapper, handles.componentType, json(RGB_GSON, subtitle));
        }) && send(target, route, route.title, wrapper -> {
            write(wrapper, handles.varIntType, 0);
            write(wrapper, handles.componentType, json(RGB_GSON, title));
        });
    }

    boolean sendTitleAction(Player player, int action) {
        Target target = target(player);
        if (target == null || !target.modern) {
            return false;
        }
        Route route = handles.modern;
        return send(target, route, route.title, wrapper -> write(wrapper, handles.varIntType, action));
    }

    boolean sendTabList(Player player, Component header, Component footer) {
        Target target = target(player);
        if (target == null || !target.modern) {
            return false;
        }
        Route route = handles.modern;
        return send(target, route, route.tabList, wrapper -> {
            write(wrapper, handles.componentType, json(RGB_GSON, header));
            write(wrapper, handles.componentType, json(RGB_GSON, footer));
        });
    }

    /** Sends a boss bar packet in the 1.9-1.16 format. {@code fields} writes everything after the action. */
    boolean sendBossBar(Player player, UUID id, int action, BossBarFields fields) {
        Target target = target(player);
        if (target == null) {
            return false;
        }
        Route route = target.modern ? handles.modern : handles.legacy;
        GsonComponentSerializer serializer = target.modern ? RGB_GSON : LEGACY_GSON;
        return send(target, route, route.bossBar, wrapper -> {
            write(wrapper, handles.uuidType, id);
            write(wrapper, handles.varIntType, action);
            fields.write(new BossBarWriter(this, wrapper, serializer));
        });
    }

    // ------------------------------------------------------------------
    //  Internals
    // ------------------------------------------------------------------

    private Target target(Player player) {
        if (handles == null || player == null) {
            return null;
        }
        UUID id = player.getUniqueId();
        Target cached = targets.get(id);
        if (cached != null) {
            return cached.connection == null ? null : cached;
        }
        try {
            Object connection = handles.connection.invoke(handles.api, id);
            if (connection == null) {
                return null; // Not cached: the connection may not be registered yet.
            }
            int version = (int) handles.playerVersion.invoke(handles.api, id);
            Object pipeline = handles.pipeline.invoke(handles.protocolInfo.invoke(connection));
            boolean modern = handles.modern != null && version >= PROTOCOL_1_16
                    && (boolean) handles.contains.invoke(pipeline, handles.modern.protocol);
            boolean bossBars = !modern && handles.legacy != null && version >= PROTOCOL_1_9
                    && (boolean) handles.contains.invoke(pipeline, handles.legacy.protocol);
            Target target = modern || bossBars ? new Target(connection, modern, bossBars) : Target.NONE;
            targets.put(id, target);
            return target.connection == null ? null : target;
        } catch (Throwable error) {
            logFailure(error);
            return null;
        }
    }

    private boolean send(Target target, Route route, Object packetType, PacketBody body) {
        if (route == null || packetType == null) {
            return false;
        }
        try {
            Object wrapper = handles.create.invoke(packetType, target.connection);
            body.write(wrapper);
            handles.scheduleSend.invoke(wrapper, route.protocol);
            return true;
        } catch (Throwable error) {
            logFailure(error);
            return false;
        }
    }

    void write(Object wrapper, Object type, Object value) throws Throwable {
        handles.write.invoke(wrapper, type, value);
    }

    Object json(GsonComponentSerializer serializer, Component component) throws Throwable {
        String json = serializer.serialize(component == null ? Component.empty() : component);
        return handles.parseJson.invoke(json);
    }

    private void logFailure(Throwable error) {
        if (failureLogged.compareAndSet(false, true)) {
            logger.log(Level.WARNING, "[HyfeUtils] ViaVersion bridge failed, using the server path instead", error);
        }
    }

    @FunctionalInterface
    private interface PacketBody {
        void write(Object wrapper) throws Throwable;
    }

    /** Writes the fields of a boss bar packet after the UUID and action. */
    @FunctionalInterface
    interface BossBarFields {
        void write(BossBarWriter writer) throws Throwable;
    }

    static final class BossBarWriter {
        private final ViaBridge bridge;
        private final Object wrapper;
        private final GsonComponentSerializer serializer;

        private BossBarWriter(ViaBridge bridge, Object wrapper, GsonComponentSerializer serializer) {
            this.bridge = bridge;
            this.wrapper = wrapper;
            this.serializer = serializer;
        }

        void title(Component title) throws Throwable {
            bridge.write(wrapper, bridge.handles.componentType, bridge.json(serializer, title));
        }

        void progress(float progress) throws Throwable {
            bridge.write(wrapper, bridge.handles.floatType, progress);
        }

        void style(int color, int overlay) throws Throwable {
            bridge.write(wrapper, bridge.handles.varIntType, color);
            bridge.write(wrapper, bridge.handles.varIntType, overlay);
        }

        void flags(byte flags) throws Throwable {
            bridge.write(wrapper, bridge.handles.byteType, flags);
        }
    }

    private record Target(Object connection, boolean modern, boolean bossBarOnly) {
        static final Target NONE = new Target(null, false, false);
    }

    /** A protocol after which packets are written, plus the packet types of its output version. */
    private record Route(Class<?> protocol, Object chat, Object title, Object bossBar, Object tabList) { }

    private static final class Handles {
        Object api;
        MethodHandle playerVersion;
        MethodHandle connection;
        MethodHandle protocolInfo;
        MethodHandle pipeline;
        MethodHandle contains;
        MethodHandle create;
        MethodHandle write;
        MethodHandle scheduleSend;
        MethodHandle parseJson;
        Object componentType;
        Object byteType;
        Object uuidType;
        Object varIntType;
        Object intType;
        Object floatType;
        Route modern;
        Route legacy;

        static Handles resolve() throws Throwable {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Handles h = new Handles();

            Class<?> via = Class.forName(BASE + "api.Via");
            Class<?> viaApi = Class.forName(BASE + "api.ViaAPI");
            Class<?> userConnection = Class.forName(BASE + "api.connection.UserConnection");
            Class<?> protocolInfo = Class.forName(BASE + "api.connection.ProtocolInfo");
            Class<?> pipeline = Class.forName(BASE + "api.protocol.ProtocolPipeline");
            Class<?> wrapper = Class.forName(BASE + "api.protocol.packet.PacketWrapper");
            Class<?> packetType = Class.forName(BASE + "api.protocol.packet.PacketType");
            Class<?> type = Class.forName(BASE + "api.type.Type");

            h.api = via.getMethod("getAPI").invoke(null);
            h.playerVersion = lookup.findVirtual(viaApi, "getPlayerVersion", MethodType.methodType(int.class, UUID.class))
                    .asType(MethodType.methodType(int.class, Object.class, UUID.class));
            h.connection = lookup.findVirtual(viaApi, "getConnection", MethodType.methodType(userConnection, UUID.class))
                    .asType(MethodType.methodType(Object.class, Object.class, UUID.class));
            h.protocolInfo = lookup.findVirtual(userConnection, "getProtocolInfo", MethodType.methodType(protocolInfo))
                    .asType(MethodType.methodType(Object.class, Object.class));
            h.pipeline = lookup.findVirtual(protocolInfo, "getPipeline", MethodType.methodType(pipeline))
                    .asType(MethodType.methodType(Object.class, Object.class));
            h.contains = lookup.findVirtual(pipeline, "contains", MethodType.methodType(boolean.class, Class.class))
                    .asType(MethodType.methodType(boolean.class, Object.class, Class.class));
            h.create = lookup.findStatic(wrapper, "create", MethodType.methodType(wrapper, packetType, userConnection))
                    .asType(MethodType.methodType(Object.class, Object.class, Object.class));
            h.write = lookup.findVirtual(wrapper, "write", MethodType.methodType(void.class, type, Object.class))
                    .asType(MethodType.methodType(void.class, Object.class, Object.class, Object.class));
            h.scheduleSend = lookup.findVirtual(wrapper, "scheduleSend", MethodType.methodType(void.class, Class.class))
                    .asType(MethodType.methodType(void.class, Object.class, Class.class));

            // ViaVersion 5 moved the constants from Type to Types.
            Class<?> constants = firstClass(BASE + "api.type.Types", BASE + "api.type.Type");
            h.componentType = constants.getField("COMPONENT").get(null);
            h.byteType = constants.getField("BYTE").get(null);
            h.uuidType = constants.getField("UUID").get(null);
            h.varIntType = constants.getField("VAR_INT").get(null);
            h.intType = constants.getField("INT").get(null);
            h.floatType = constants.getField("FLOAT").get(null);
            h.parseJson = jsonParser(type, h.componentType);

            h.modern = route(
                    new String[]{BASE + "protocols.v1_15_2to1_16.Protocol1_15_2To1_16",
                            BASE + "protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2"},
                    new String[]{BASE + "protocols.v1_15_2to1_16.packet.ClientboundPackets1_16",
                            BASE + "protocols.protocol1_16to1_15_2.ClientboundPackets1_16"});
            h.legacy = route(
                    new String[]{BASE + "protocols.v1_8to1_9.Protocol1_8To1_9",
                            BASE + "protocols.protocol1_9to1_8.Protocol1_9To1_8"},
                    new String[]{BASE + "protocols.v1_8to1_9.packet.ClientboundPackets1_9",
                            BASE + "protocols.protocol1_9to1_8.ClientboundPackets1_9"});
            if (h.modern == null && h.legacy == null) {
                throw new ClassNotFoundException("No supported ViaVersion protocol classes");
            }
            return h;
        }

        private static MethodHandle jsonParser(Class<?> type, Object componentType) throws Throwable {
            // Use the Gson that ViaVersion itself uses (it is relocated in most builds).
            Class<?> element = (Class<?>) type.getMethod("getOutputClass").invoke(componentType);
            Class<?> parser = Class.forName(element.getPackageName() + ".JsonParser", true, element.getClassLoader());
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            try {
                return lookup.findStatic(parser, "parseString", MethodType.methodType(element, String.class))
                        .asType(MethodType.methodType(Object.class, String.class));
            } catch (NoSuchMethodException oldGson) {
                MethodHandle parse = lookup.findVirtual(parser, "parse", MethodType.methodType(element, String.class));
                return parse.bindTo(parser.getConstructor().newInstance())
                        .asType(MethodType.methodType(Object.class, String.class));
            }
        }

        private static Route route(String[] protocolNames, String[] packetNames) {
            try {
                Class<?> protocol = firstClass(protocolNames);
                Class<?> packets = firstClass(packetNames);
                return new Route(protocol,
                        constant(packets, "CHAT", "CHAT_MESSAGE"),
                        constant(packets, "SET_TITLES", "TITLE"),
                        constant(packets, "BOSS_EVENT", "BOSSBAR"),
                        constant(packets, "TAB_LIST"));
            } catch (ClassNotFoundException missing) {
                return null;
            }
        }

        private static Class<?> firstClass(String... names) throws ClassNotFoundException {
            for (String name : names) {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException ignored) {
                    // Try the next naming scheme.
                }
            }
            throw new ClassNotFoundException(String.join(", ", names));
        }

        private static Object constant(Class<?> enumType, String... names) {
            Object[] values = enumType.getEnumConstants();
            if (values == null) {
                return null;
            }
            for (String name : names) {
                for (Object value : values) {
                    if (((Enum<?>) value).name().equals(name)) {
                        return value;
                    }
                }
            }
            return null;
        }
    }
}
