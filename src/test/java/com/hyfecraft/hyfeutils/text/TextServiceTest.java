package com.hyfecraft.hyfeutils.text;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextServiceTest {
    private final TextService text = new TextService();

    @Test
    void translatesLegacyColors() {
        assertEquals("\u00a7aHola \u00a7lMundo", text.colorize("&aHola &lMundo"));
    }

    @Test
    void usesExpandedHexForModernClients() {
        assertEquals("\u00a7x\u00a71\u00a72\u00a7a\u00a7b\u00a7e\u00a7fTexto", text.colorize("&#12ABEFTexto", 735));
    }

    @Test
    void selectsNearestLegacyColorForOldClients() {
        assertEquals("\u00a74Texto", text.colorize("&#FF0000Texto", 47));
    }

    @Test
    void adventureReadsModernHexSequenceAsRgb() {
        TextComponent component = (TextComponent) LegacyComponentSerializer.legacySection()
                .deserialize(text.colorize("&#12ABEFTexto", 735));
        assertEquals(TextColor.color(0x12ABEF), component.color());
    }
}
