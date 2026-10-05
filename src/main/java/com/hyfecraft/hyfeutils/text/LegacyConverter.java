package com.hyfecraft.hyfeutils.text;

/**
 * Single-pass conversion of legacy formatting to MiniMessage tags, so both syntaxes can be mixed
 * in one string: {@code "&aHola <gradient:#ff0000:#0000ff>Mundo</gradient> &#12ABEFRGB"}.
 *
 * <p>Supported legacy input, with {@code &} or {@code §}: {@code &a}, {@code &l}, {@code &r},
 * {@code &#RRGGBB}, Spigot's {@code &x&R&R&G&G&B&B} and a bare {@code #RRGGBB}.
 * Like in vanilla, a legacy color turns off the decorations opened by previous legacy codes.</p>
 */
final class LegacyConverter {
    private static final String[] COLOR_TAGS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    };
    private static final String DECORATION_CODES = "klmno";
    private static final String[] DECORATION_TAGS = {"obfuscated", "bold", "strikethrough", "underlined", "italic"};

    private LegacyConverter() { }

    /** Whether the input may contain anything that needs parsing. */
    static boolean hasFormatting(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' || c == '§' || c == '<' || c == '#' || c == '\\') {
                return true;
            }
        }
        return false;
    }

    static String toMiniMessage(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        convert(input, 0, input.length(), out);
        return out.toString();
    }

    private static void convert(String in, int start, int end, StringBuilder out) {
        int openDecorations = 0; // bit mask of decorations opened by legacy codes
        int i = start;
        while (i < end) {
            char c = in.charAt(i);
            if (c == '\\' && i + 1 < end) {
                // Keep MiniMessage escapes untouched.
                out.append(c).append(in.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '<') {
                int close = tagEnd(in, i, end);
                if (close > 0) {
                    copyTag(in, i, close, out);
                    i = close + 1;
                    continue;
                }
            }
            if ((c == '&' || c == '§') && i + 1 < end) {
                char code = Character.toLowerCase(in.charAt(i + 1));
                if (code == '#' && isHex(in, i + 2, 6, end)) {
                    openDecorations = color(out, openDecorations);
                    out.append("<#").append(in, i + 2, i + 8).append('>');
                    i += 8;
                    continue;
                }
                if (code == 'x' && isSpigotHex(in, i + 2, end)) {
                    openDecorations = color(out, openDecorations);
                    out.append("<#");
                    for (int digit = 0; digit < 6; digit++) {
                        out.append(in.charAt(i + 3 + digit * 2));
                    }
                    out.append('>');
                    i += 14;
                    continue;
                }
                int color = Character.digit(code, 16);
                if (color >= 0) {
                    openDecorations = color(out, openDecorations);
                    out.append('<').append(COLOR_TAGS[color]).append('>');
                    i += 2;
                    continue;
                }
                int decoration = DECORATION_CODES.indexOf(code);
                if (decoration >= 0) {
                    out.append('<').append(DECORATION_TAGS[decoration]).append('>');
                    openDecorations |= 1 << decoration;
                    i += 2;
                    continue;
                }
                if (code == 'r') {
                    out.append("<reset>");
                    openDecorations = 0;
                    i += 2;
                    continue;
                }
            }
            if (c == '#' && isHex(in, i + 1, 6, end) && standaloneHex(in, i, start, end)) {
                openDecorations = color(out, openDecorations);
                out.append("<#").append(in, i + 1, i + 7).append('>');
                i += 7;
                continue;
            }
            out.append(c);
            i++;
        }
    }

    /** Emits negations for the decorations opened by legacy codes, as a legacy color would reset them. */
    private static int color(StringBuilder out, int openDecorations) {
        for (int bit = 0; bit < DECORATION_TAGS.length; bit++) {
            if ((openDecorations & (1 << bit)) != 0) {
                out.append("<!").append(DECORATION_TAGS[bit]).append('>');
            }
        }
        return 0;
    }

    /**
     * Returns the index of the {@code >} closing a MiniMessage tag that starts at {@code start},
     * or -1 when the {@code <} is plain text (for example {@code "a < b"}).
     */
    private static int tagEnd(String in, int start, int end) {
        if (start + 1 >= end) {
            return -1;
        }
        char first = in.charAt(start + 1);
        if (!(Character.isLetter(first) || first == '#' || first == '/' || first == '!' || first == '_')) {
            return -1;
        }
        char quote = 0;
        for (int i = start + 1; i < end; i++) {
            char c = in.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '>') {
                return i;
            } else if (c == '<' || c == '\n') {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Copies a tag verbatim. The quoted arguments of a hover tag are MiniMessage text themselves,
     * so legacy codes inside them are converted too; other arguments (commands, URLs) are not.
     */
    private static void copyTag(String in, int start, int close, StringBuilder out) {
        if (!in.regionMatches(true, start + 1, "hover", 0, 5)) {
            out.append(in, start, close + 1);
            return;
        }
        int i = start;
        while (i <= close) {
            char c = in.charAt(i);
            if (c == '\'' || c == '"') {
                int endQuote = i + 1;
                while (endQuote < close && in.charAt(endQuote) != c) {
                    if (in.charAt(endQuote) == '\\') endQuote++;
                    endQuote++;
                }
                out.append(c);
                convert(in, i + 1, Math.min(endQuote, close), out);
                if (endQuote < close) {
                    out.append(c);
                }
                i = endQuote + 1;
                continue;
            }
            out.append(c);
            i++;
        }
    }

    private static boolean standaloneHex(String in, int hash, int start, int end) {
        if (hash > start) {
            char before = in.charAt(hash - 1);
            if (Character.isLetterOrDigit(before) || before == '<' || before == ':' || before == '&'
                    || before == '§' || before == '#' || before == '/') {
                return false;
            }
        }
        int after = hash + 7;
        return after >= end || !Character.isLetterOrDigit(in.charAt(after));
    }

    private static boolean isSpigotHex(String in, int start, int end) {
        if (start + 12 > end) {
            return false;
        }
        for (int digit = 0; digit < 6; digit++) {
            int index = start + digit * 2;
            char marker = in.charAt(index);
            if ((marker != '&' && marker != '§') || Character.digit(in.charAt(index + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(String in, int start, int length, int end) {
        if (start + length > end) {
            return false;
        }
        for (int i = start; i < start + length; i++) {
            if (Character.digit(in.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
