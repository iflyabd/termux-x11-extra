package com.termux.x11.utils;

import android.graphics.Color;
import android.graphics.Typeface;

import androidx.annotation.Nullable;

/**
 * Ready-made palettes for the additional (extra) keys bar plus helpers shared by the
 * preferences screen and the runtime styling code.
 *
 * The palette values are stored in the regular color preferences when a theme is selected,
 * so every individual color remains freely editable afterwards.
 */
public final class ExtraKeysThemes {

    public static final String CUSTOM = "custom";

    public static final String[] VALUES = {
            CUSTOM, "termux", "dracula", "nord", "solarized",
            "mono", "amber", "ocean", "matrix"
    };

    public static final class Palette {
        public final String text;
        public final String activeText;
        public final String background;
        public final String activeBackground;
        public final String border;
        public final int radiusDp;
        public final int borderWidthDp;
        public final int textSizeSp;
        public final int keyGapDp;

        Palette(String text, String activeText, String background, String activeBackground,
                String border, int radiusDp, int borderWidthDp, int textSizeSp, int keyGapDp) {
            this.text = text;
            this.activeText = activeText;
            this.background = background;
            this.activeBackground = activeBackground;
            this.border = border;
            this.radiusDp = radiusDp;
            this.borderWidthDp = borderWidthDp;
            this.textSizeSp = textSizeSp;
            this.keyGapDp = keyGapDp;
        }
    }

    private ExtraKeysThemes() { }

    /** Returns the palette for a theme value, or {@code null} for the custom theme. */
    @Nullable
    public static Palette get(String theme) {
        if (theme == null)
            return null;
        switch (theme) {
            case "termux":
                return new Palette("#FFFFFFFF", "#FFFF0000", "#00000000", "#FF7F7F7F",
                        "#33FFFFFF", 6, 1, 14, 2);
            case "dracula":
                return new Palette("#FFF8F8F2", "#FFFF79C6", "#FF282A36", "#FF44475A",
                        "#FF6272A4", 8, 1, 14, 2);
            case "nord":
                return new Palette("#FFECEFF4", "#FF88C0D0", "#FF2E3440", "#FF4C566A",
                        "#FF5E81AC", 8, 1, 14, 2);
            case "solarized":
                return new Palette("#FF93A1A1", "#FFB58900", "#FF002B36", "#FF073642",
                        "#FF586E75", 6, 1, 14, 2);
            case "mono":
                return new Palette("#FFFFFFFF", "#FF000000", "#FF000000", "#FFFFFFFF",
                        "#FF808080", 0, 1, 14, 1);
            case "amber":
                return new Palette("#FFFFC107", "#FF000000", "#FF1A1A1A", "#FFFFC107",
                        "#FFFF9800", 10, 1, 15, 3);
            case "ocean":
                return new Palette("#FFE0F7FA", "#FF00E5FF", "#FF0B2530", "#FF01579B",
                        "#FF0288D1", 8, 1, 14, 2);
            case "matrix":
                return new Palette("#FF00FF41", "#FF000000", "#FF000000", "#FF00FF41",
                        "#FF00FF41", 4, 1, 14, 1);
            case CUSTOM:
            default:
                return null;
        }
    }

    /** Parses a color stored as text ({@code #RRGGBB}, {@code #AARRGGBB} or without {@code #}). */
    public static int parseColor(@Nullable String value, int fallback) {
        if (value == null)
            return fallback;
        String text = value.trim();
        if (text.isEmpty())
            return fallback;
        if (!text.startsWith("#"))
            text = "#" + text;
        try {
            return Color.parseColor(text);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Resolves the font preference to a typeface.
     *
     * @return the typeface, or {@code null} to keep the platform default.
     */
    @Nullable
    public static Typeface typeface(@Nullable String name) {
        if (name == null || name.isEmpty() || "default".equals(name))
            return null;
        switch (name) {
            case "monospace":
                return Typeface.MONOSPACE;
            case "serif":
                return Typeface.SERIF;
            default:
                return Typeface.create(name, Typeface.NORMAL);
        }
    }
}
