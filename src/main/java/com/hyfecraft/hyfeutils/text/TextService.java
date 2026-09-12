package com.hyfecraft.hyfeutils.text;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the public color syntax to client-compatible legacy text. */
public final class TextService {
    private static final int RGB_PROTOCOL = 735;
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})|(?<![A-Za-z0-9])#([0-9a-fA-F]{6})(?![A-Za-z0-9])");
    private static final String LEGACY_CODES = "0123456789abcdefklmnor";
    private static final LegacyColor[] COLORS = {
            new LegacyColor('0', 0x000000), new LegacyColor('1', 0x0000AA),
            new LegacyColor('2', 0x00AA00), new LegacyColor('3', 0x00AAAA),
            new LegacyColor('4', 0xAA0000), new LegacyColor('5', 0xAA00AA),
            new LegacyColor('6', 0xFFAA00), new LegacyColor('7', 0xAAAAAA),
            new LegacyColor('8', 0x555555), new LegacyColor('9', 0x5555FF),
            new LegacyColor('a', 0x55FF55), new LegacyColor('b', 0x55FFFF),
            new LegacyColor('c', 0xFF5555), new LegacyColor('d', 0xFF55FF),
            new LegacyColor('e', 0xFFFF55), new LegacyColor('f', 0xFFFFFF)
    };

    public String colorize(String input) {
        return colorize(input, -1);
    }

    public String colorize(String input, int clientProtocol) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = HEX.matcher(input);
        StringBuffer result = new StringBuffer(input.length());
        while (matcher.find()) {
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int rgb = Integer.parseInt(value, 16);
            String replacement = clientProtocol >= RGB_PROTOCOL
                    ? modernColor(value)
                    : legacyColor(rgb);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return legacyCodes(result.toString());
    }

    public boolean supportsRgb(int clientProtocol) {
        return clientProtocol >= RGB_PROTOCOL;
    }

    private String legacyCodes(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '&' && i + 1 < value.length()) {
                char next = Character.toLowerCase(value.charAt(i + 1));
                if (LEGACY_CODES.indexOf(next) >= 0) {
                    result.append('\u00a7').append(next);
                    i++;
                    continue;
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private String modernColor(String value) {
        StringBuilder result = new StringBuilder(14).append('\u00a7').append('x');
        for (char digit : value.toLowerCase(Locale.ROOT).toCharArray()) {
            result.append('\u00a7').append(digit);
        }
        return result.toString();
    }

    private String legacyColor(int rgb) {
        LegacyColor nearest = COLORS[0];
        int distance = Integer.MAX_VALUE;
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        for (LegacyColor color : COLORS) {
            int dr = red - ((color.rgb >> 16) & 0xff);
            int dg = green - ((color.rgb >> 8) & 0xff);
            int db = blue - (color.rgb & 0xff);
            int candidate = dr * dr + dg * dg + db * db;
            if (candidate <= distance) {
                distance = candidate;
                nearest = color;
            }
        }
        return "\u00a7" + nearest.code;
    }

    private record LegacyColor(char code, int rgb) { }
}
