package com.hyfecraft.hyfeutils.message;

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
}
