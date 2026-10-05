package com.hyfecraft.hyfeutils.platform;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.ProtocolPipeline;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Simulates a 1.8 server running ViaVersion 5 with clients of several versions. */
class ViaBridgeTest {
    private static final int CLIENT_1_20 = 763;
    private static final int CLIENT_1_12 = 340;
    private static final int CLIENT_1_8 = 47;

    private final Map<UUID, Integer> versions = new HashMap<>();
    private ViaBridge bridge;
    private Dispatcher dispatcher;

    @BeforeEach
    void setUp() {
        PacketWrapper.SENT.clear();
        versions.clear();
        Via.api = new ViaAPI<Object>() {
            @Override
            public int getPlayerVersion(UUID uuid) {
                return versions.getOrDefault(uuid, -1);
            }

            @Override
            public UserConnection getConnection(UUID uuid) {
                int version = getPlayerVersion(uuid);
                ProtocolPipeline pipeline = protocol ->
                        (protocol == Protocol1_8To1_9.class && version >= ViaBridge.PROTOCOL_1_9)
                                || (protocol == Protocol1_15_2To1_16.class && version >= ViaBridge.PROTOCOL_1_16);
                ProtocolInfo info = () -> pipeline;
                return () -> info;
            }
        };
        bridge = ViaBridge.create(Logger.getAnonymousLogger());
        dispatcher = new Dispatcher(unusedAudiences(), bridge);
    }

    @Test
    void resolvesViaVersion5() {
        assertTrue(bridge.isPresent());
    }

    @Test
    void modernClientReceivesRgbChat() {
        Player player = player(CLIENT_1_20);
        dispatcher.message(player, Component.text("Hola", TextColor.color(0x12ABEF)));

        assertEquals(1, PacketWrapper.SENT.size());
        PacketWrapper.Recorded packet = PacketWrapper.SENT.get(0);
        assertEquals(ClientboundPackets1_16.CHAT, packet.type());
        assertEquals(Protocol1_15_2To1_16.class, packet.protocol()[0]);
        assertTrue(json(packet.values().get(0)).contains("#12ABEF"), json(packet.values().get(0)));
        assertEquals((byte) 1, packet.values().get(1));
        assertEquals(new UUID(0, 0), packet.values().get(2));
        assertTrue(bridge.supportsRgb(player));
    }

    @Test
    void titleSendsTimesSubtitleAndTitle() {
        Player player = player(CLIENT_1_20);
        dispatcher.title(player, Component.text("A"), Component.text("B"), 10, 40, 10);

        List<PacketWrapper.Recorded> sent = PacketWrapper.SENT;
        assertEquals(3, sent.size());
        assertEquals(List.of(3, 10, 40, 10), sent.get(0).values());
        assertEquals(1, sent.get(1).values().get(0));
        assertEquals(0, sent.get(2).values().get(0));
        assertTrue(json(sent.get(2).values().get(1)).contains("\"A\""));
    }

    @Test
    void oldClientsUseTheServerForText() {
        assertFalse(bridge.handlesText(player(CLIENT_1_12)));
        assertFalse(bridge.handlesText(player(CLIENT_1_8)));
        assertFalse(bridge.handlesBossBars(player(CLIENT_1_8)));
    }

    @Test
    void bossBarKeepsStyleOn1_9To1_15Clients() {
        Player player = player(CLIENT_1_12);
        BossBar bar = BossBar.bossBar(Component.text("Boss", TextColor.color(0xFF0000)), 0.5f,
                BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_12);
        dispatcher.showBossBar(player, bar);

        PacketWrapper.Recorded add = PacketWrapper.SENT.get(0);
        assertEquals(ClientboundPackets1_9.BOSS_EVENT, add.type());
        assertEquals(Protocol1_8To1_9.class, add.protocol()[0]);
        assertEquals(0, add.values().get(1));
        String title = json(add.values().get(2));
        assertTrue(title.contains("red") && !title.contains("#"), title);
        assertEquals(0.5f, add.values().get(3));
        assertEquals(3, add.values().get(4)); // GREEN
        assertEquals(3, add.values().get(5)); // NOTCHED_12
        assertEquals((byte) 0, add.values().get(6));
    }

    @Test
    void bossBarUpdatesAndHides() {
        Player player = player(CLIENT_1_20);
        BossBar bar = BossBar.bossBar(Component.text("Boss"), 1f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
        dispatcher.showBossBar(player, bar);
        bar.progress(0.25f);
        bar.color(BossBar.Color.WHITE);
        bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        dispatcher.hideBossBar(player, bar);
        bar.progress(0.1f); // No viewers left: nothing is sent.

        List<PacketWrapper.Recorded> sent = PacketWrapper.SENT;
        assertEquals(5, sent.size());
        UUID id = (UUID) sent.get(0).values().get(0);
        assertEquals(List.of(id, 2, 0.25f), sent.get(1).values());
        assertEquals(List.of(id, 4, 6, 0), sent.get(2).values());
        assertEquals(List.of(id, 5, (byte) 1), sent.get(3).values());
        assertEquals(List.of(id, 1), sent.get(4).values());
        assertEquals(ClientboundPackets1_16.BOSS_EVENT, sent.get(0).type());
    }

    @Test
    void enumOrderMatchesProtocolIds() {
        assertEquals(List.of("PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE"),
                java.util.Arrays.stream(BossBar.Color.values()).map(Enum::name).toList());
        assertEquals(List.of("PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20"),
                java.util.Arrays.stream(BossBar.Overlay.values()).map(Enum::name).toList());
    }

    private Player player(int version) {
        UUID id = UUID.randomUUID();
        versions.put(id, version);
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "isOnline" -> true;
                    case "hashCode" -> id.hashCode();
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Player(" + version + ")";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static BukkitAudiences unusedAudiences() {
        return (BukkitAudiences) Proxy.newProxyInstance(ViaBridgeTest.class.getClassLoader(),
                new Class<?>[]{BukkitAudiences.class}, (proxy, method, args) -> {
                    throw new AssertionError("Adventure path used: " + method.getName());
                });
    }

    private static String json(Object value) {
        return ((JsonElement) value).json();
    }
}
