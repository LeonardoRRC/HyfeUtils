package com.hyfecraft.hyfeutils.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text formatting. {@link #parse(String)} accepts MiniMessage and legacy codes in the same string
 * and returns a {@link Component}; the color downsampling for old clients happens when the
 * component is sent, so a parsed component can be reused for every player.
 */
public final class TextService {
    private static final int RGB_PROTOCOL = 735;
    private static final int CACHE_SIZE = 256;
    private static final LegacyComponentSerializer LEGACY_RGB = LegacyComponentSerializer.builder()
            .character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
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

    private final MiniMessage miniMessage;
    private final Map<String, Component> cache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public TextService() {
        this(MiniMessage.miniMessage());
    }

    /** Uses a custom MiniMessage instance, for example one with extra global tags. */
    public TextService(MiniMessage miniMessage) {
        this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
    }

    // ------------------------------------------------------------------
    //  Components
    // ------------------------------------------------------------------

    /**
     * Parses MiniMessage and legacy formatting mixed together:
     * {@code "&aHola <gradient:#ff0000:#0000ff>Mundo</gradient> &#12ABEFRGB"}.
     * Results are cached, so parsing the same configuration message repeatedly is cheap.
     */
    public Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (!LegacyConverter.hasFormatting(input)) {
            return Component.text(input);
        }
        synchronized (cache) {
            Component cached = cache.get(input);
            if (cached != null) {
                return cached;
            }
        }
        Component parsed = miniMessage.deserialize(LegacyConverter.toMiniMessage(input));
        synchronized (cache) {
            cache.put(input, parsed);
        }
        return parsed;
    }

    /**
     * Parses like {@link #parse(String)} with placeholders, for example
     * {@code parse("&aHola <player>", Placeholder.unparsed("player", name))}.
     */
    public Component parse(String input, TagResolver... resolvers) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(LegacyConverter.toMiniMessage(input), TagResolver.resolver(resolvers));
    }

    /** Parses only MiniMessage, without legacy codes. */
    public Component miniMessage(String input, TagResolver... resolvers) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(input, TagResolver.resolver(resolvers));
    }

    /**
     * Parses only legacy codes ({@code &a}, {@code &#RRGGBB}, {@code &x&R&R...}). MiniMessage tags
     * are kept as plain text, which makes it safe for text written by players.
     */
    public Component legacy(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(LegacyConverter.toMiniMessage(miniMessage.escapeTags(input)));
    }

    /** Converts legacy codes into the equivalent MiniMessage tags. */
    public String toMiniMessage(String legacyInput) {
        return legacyInput == null ? null : LegacyConverter.toMiniMessage(legacyInput);
    }

    /** Serializes a component back to MiniMessage. */
    public String serialize(Component component) {
        return miniMessage.serialize(component);
    }

    /** Escapes MiniMessage tags in untrusted text, such as chat written by a player. */
    public String escape(String input) {
        return input == null ? null : miniMessage.escapeTags(input);
    }

    /** Returns the text without any formatting. */
    public String plain(String input) {
        return PlainTextComponentSerializer.plainText().serialize(parse(input));
    }

    /**
     * Returns {@code §} text for APIs that only accept strings (item names, lore, scoreboards),
     * with full MiniMessage support. RGB is kept only if the client protocol supports it.
     */
    public String toLegacy(String input, int clientProtocol) {
        if (input == null) {
            return null;
        }
        return toLegacy(parse(input), clientProtocol);
    }

    public String toLegacy(Component component, int clientProtocol) {
        return (clientProtocol >= RGB_PROTOCOL ? LEGACY_RGB : LEGACY).serialize(component);
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    /** Empties the parse cache, for example after reloading the configuration. */
    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    // ------------------------------------------------------------------
    //  Legacy strings (fast path, no MiniMessage)
    // ------------------------------------------------------------------

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
