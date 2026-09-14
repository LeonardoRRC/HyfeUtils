package com.hyfecraft.hyfeutils.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.Test;

import static com.hyfecraft.hyfeutils.text.ClickableTextService.clickable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClickableTextServiceTest {

    @Test
    void buildLegacyColorizesText() {
        String result = clickable("&aHola &lMundo").buildLegacy();
        assertEquals("\u00a7aHola \u00a7lMundo", result);
    }

    @Test
    void buildReturnsComponent() {
        Component component = clickable("&aClick me").build();
        assertNotNull(component);
    }

    @Test
    void runCommandAddsClickEvent() {
        Component component = clickable("&aTest")
                .runCommand("/test")
                .build();
        assertNotNull(component.clickEvent());
        assertEquals(ClickEvent.Action.RUN_COMMAND, component.clickEvent().action());
        assertEquals("/test", component.clickEvent().value());
    }

    @Test
    void suggestCommandAddsClickEvent() {
        Component component = clickable("&aTest")
                .suggestCommand("/test ")
                .build();
        assertNotNull(component.clickEvent());
        assertEquals(ClickEvent.Action.SUGGEST_COMMAND, component.clickEvent().action());
    }

    @Test
    void openUrlAddsClickEvent() {
        Component component = clickable("&aLink")
                .openUrl("https://example.com")
                .build();
        assertNotNull(component.clickEvent());
        assertEquals(ClickEvent.Action.OPEN_URL, component.clickEvent().action());
        assertEquals("https://example.com", component.clickEvent().value());
    }

    @Test
    void copyToClipboardAddsClickEvent() {
        Component component = clickable("&aCopy")
                .copyToClipboard("secret")
                .build();
        assertNotNull(component.clickEvent());
        assertEquals(ClickEvent.Action.COPY_TO_CLIPBOARD, component.clickEvent().action());
    }

    @Test
    void buildWithoutActionsHasNoEvents() {
        Component component = clickable("&aPlain").build();
        assertNull(component.clickEvent());
        assertNull(component.hoverEvent());
    }
}
