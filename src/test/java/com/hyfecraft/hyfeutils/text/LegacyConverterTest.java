package com.hyfecraft.hyfeutils.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyConverterTest {
    private final TextService text = new TextService();

    @Test
    void convertsLegacyCodes() {
        assertEquals("<green>Hola <bold>Mundo", text.toMiniMessage("&aHola &lMundo"));
        assertEquals("<red>A<reset>B", text.toMiniMessage("§cA§rB"));
    }

    @Test
    void convertsEveryHexFormat() {
        assertEquals("<#12ABEF>A", text.toMiniMessage("&#12ABEFA"));
        assertEquals("<#12abef>A", text.toMiniMessage("&x&1&2&a&b&e&fA"));
        assertEquals("<#12abef>A", text.toMiniMessage("§x§1§2§a§b§e§fA"));
        assertEquals("Color <#FF0000> rojo", text.toMiniMessage("Color #FF0000 rojo"));
        assertEquals("id#FF0000", text.toMiniMessage("id#FF0000"));
    }

    @Test
    void legacyColorTurnsOffPreviousLegacyDecorations() {
        assertEquals("<bold>A<!bold><green>B", text.toMiniMessage("&lA&aB"));
        Component component = text.parse("&lA&aB");
        assertEquals(TextDecoration.State.FALSE, styleOf(component, "B").decoration(TextDecoration.BOLD));
    }

    @Test
    void leavesMiniMessageTagsUntouched() {
        assertEquals("<#ff0000>A <color:#00ff00>B", text.toMiniMessage("<#ff0000>A <color:#00ff00>B"));
        assertEquals("<gradient:#ff0000:#0000ff>A</gradient>", text.toMiniMessage("<gradient:#ff0000:#0000ff>A</gradient>"));
        assertEquals("<click:run_command:'/say &aHi'>X", text.toMiniMessage("<click:run_command:'/say &aHi'>X"));
        assertEquals("a < b & c", text.toMiniMessage("a < b & c"));
    }

    @Test
    void convertsLegacyInsideHoverText() {
        assertEquals("<hover:show_text:'<green>Info'>X", text.toMiniMessage("<hover:show_text:'&aInfo'>X"));
    }

    @Test
    void parsesMixedSyntax() {
        Component component = text.parse("&aVerde <bold>negrita</bold> &#12ABEFrgb <gradient:red:blue>degradado</gradient>");
        assertEquals("Verde negrita rgb degradado", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(NamedTextColor.GREEN, styleOf(component, "Verde ").color());
        assertEquals(TextColor.color(0x12ABEF), styleOf(component, "rgb ").color());
    }

    @Test
    void parsesClickAndHoverTags() {
        Component component = text.parse("<click:run_command:/spawn><hover:show_text:'&eIr'>&aSpawn</hover></click>");
        Component leaf = leaf(component, "Spawn");
        assertNotNull(leaf);
        assertEquals(ClickEvent.Action.RUN_COMMAND, leaf.style().clickEvent() != null
                ? leaf.style().clickEvent().action() : inheritedClick(component));
    }

    @Test
    void legacyModeIgnoresTags() {
        Component component = text.legacy("&a<red>hola</red>");
        assertEquals("<red>hola</red>", PlainTextComponentSerializer.plainText().serialize(component));
    }

    @Test
    void plainTextIsReturnedDirectly() {
        assertEquals(Component.text("hola"), text.parse("hola"));
        assertEquals(Component.empty(), text.parse(null));
    }

    @Test
    void cachesParsedComponents() {
        assertTrue(text.parse("&aCache") == text.parse("&aCache"));
    }

    @Test
    void toLegacyKeepsRgbOnlyForModernClients() {
        assertEquals("§x§1§2§a§b§e§fA", text.toLegacy("<#12abef>A", 735));
        assertFalse(text.toLegacy("<#12abef>A", 47).contains("§x"));
    }

    @Test
    void hoverEventIsBuiltByClickableBuilder() {
        Component component = ClickableTextService.clickable("&aX").hoverText("<#ff0000>Y").build();
        assertNotNull(component.hoverEvent());
        assertEquals(HoverEvent.Action.SHOW_TEXT, component.hoverEvent().action());
    }

    /** Resolved style (with inherited values) of the first text leaf with the given content. */
    private static net.kyori.adventure.text.format.Style styleOf(Component component, String content) {
        return find(component, content, net.kyori.adventure.text.format.Style.empty());
    }

    private static net.kyori.adventure.text.format.Style find(Component component, String content,
                                                              net.kyori.adventure.text.format.Style parent) {
        net.kyori.adventure.text.format.Style style = component.style().merge(parent, net.kyori.adventure.text.format.Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (component instanceof net.kyori.adventure.text.TextComponent text && text.content().equals(content)) {
            return style;
        }
        for (Component child : component.children()) {
            net.kyori.adventure.text.format.Style found = find(child, content, style);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Component leaf(Component component, String content) {
        if (component instanceof net.kyori.adventure.text.TextComponent text && text.content().equals(content)) {
            return component;
        }
        for (Component child : component.children()) {
            Component found = leaf(child, content);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static ClickEvent.Action inheritedClick(Component component) {
        if (component.clickEvent() != null) {
            return component.clickEvent().action();
        }
        for (Component child : component.children()) {
            ClickEvent.Action action = inheritedClick(child);
            if (action != null) {
                return action;
            }
        }
        return null;
    }
}
