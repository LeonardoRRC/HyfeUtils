package com.hyfecraft.hyfeutils.message;

import com.hyfecraft.hyfeutils.text.TextService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {

    @Test
    void centerAddsPaddingBeforeMessage() {
        String centered = ChatService.center("&aHello");
        assertNotNull(centered);
        assertTrue(centered.length() > "&aHello".length());
        assertTrue(centered.endsWith("&aHello"));
    }

    @Test
    void centerReturnsOriginalIfTooLong() {
        String longMessage = "&a" + "A".repeat(200);
        assertEquals(longMessage, ChatService.center(longMessage));
    }

    @Test
    void centerReturnsNullForNull() {
        assertEquals(null, ChatService.center(null));
    }

    @Test
    void centerReturnsEmptyForEmptyString() {
        assertEquals("", ChatService.center(""));
    }

    @Test
    void centerReturnsPrefixForCodeOnly() {
        String centered = ChatService.center("&a");
        assertNotNull(centered);
        assertTrue(centered.startsWith("\u00a7r"));
    }

    @Test
    void boldTextIsWiderThanNormalText() {
        TextService text = new TextService();
        int normal = ChatService.width(text.parse("&aHola"));
        int bold = ChatService.width(text.parse("&a&lHola"));
        assertEquals(normal + 4, bold);
    }

    @Test
    void centerComponentPrependsSpaces() {
        Component centered = ChatService.centerComponent(Component.text("Hola"));
        assertTrue(centered instanceof net.kyori.adventure.text.TextComponent);
        String spaces = ((net.kyori.adventure.text.TextComponent) centered).content();
        assertTrue(spaces.length() > 10 && spaces.isBlank());
    }

    @Test
    void mergingAStyleKeepsUnsetDecorations() {
        Style base = Style.style().decoration(TextDecoration.BOLD, true).build();
        Style wave = Style.style().decoration(TextDecoration.ITALIC, true).build();
        Style merged = base.merge(wave);
        assertEquals(TextDecoration.State.TRUE, merged.decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.TRUE, merged.decoration(TextDecoration.ITALIC));
    }

    @Test
    void parsesBossBarNames() {
        assertEquals(BossBar.Color.RED, BossBarService.parseColor("red", BossBar.Color.PINK));
        assertEquals(BossBar.Color.PINK, BossBarService.parseColor("rainbow", BossBar.Color.PINK));
        assertEquals(BossBar.Overlay.NOTCHED_12, BossBarService.parseOverlay("12", BossBar.Overlay.PROGRESS));
        assertEquals(BossBar.Overlay.NOTCHED_6, BossBarService.parseOverlay("notched 6", BossBar.Overlay.PROGRESS));
    }
}
