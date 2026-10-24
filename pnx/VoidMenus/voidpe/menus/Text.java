package voidpe.menus;

import java.util.Locale;

/**
 * Цвета: &-коды, &-hex (&#RRGGBB) и Java-hex (&x&1&C&A&7&6&9).
 * Bedrock понимает 16 обычных цветов, поэтому настоящий hex заменяется на ближайший.
 */
public final class Text {

    private static final char[] CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final int[][] RGB = {
            {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
            {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
            {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
            {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255}
    };

    private Text() {
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String s = hexJava(input);
        s = hexAmp(s);
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < s.length() && isCode(s.charAt(i + 1))) {
                out.append('\u00a7').append(Character.toLowerCase(s.charAt(i + 1)));
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < input.length() && isCode(input.charAt(i + 1))) {
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** &x&1&C&A&7&6&9 -> ближайший обычный цвет */
    private static String hexJava(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < s.length()
                    && Character.toLowerCase(s.charAt(i + 1)) == 'x') {
                StringBuilder hex = new StringBuilder();
                int j = i + 2;
                while (j + 1 < s.length() && hex.length() < 6
                        && (s.charAt(j) == '&' || s.charAt(j) == '\u00a7')) {
                    hex.append(s.charAt(j + 1));
                    j += 2;
                }
                if (hex.length() == 6 && isHex(hex.toString())) {
                    out.append('\u00a7').append(nearest(hex.toString()));
                    i = j;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** &#RRGGBB -> ближайший обычный цвет */
    private static String hexAmp(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < s.length() && s.charAt(i + 1) == '#'
                    && i + 8 <= s.length()) {
                String hex = s.substring(i + 2, i + 8);
                if (hex.length() == 6 && isHex(hex)) {
                    out.append('\u00a7').append(nearest(hex));
                    i += 8;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isHex(String hex) {
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static char nearest(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < RGB.length; i++) {
            long dr = r - RGB[i][0];
            long dg = g - RGB[i][1];
            long db = b - RGB[i][2];
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return CODES[best];
    }

    private static boolean isCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')
                || lower == 'k' || lower == 'l' || lower == 'm' || lower == 'n'
                || lower == 'o' || lower == 'r' || lower == 'x';
    }

    public static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
