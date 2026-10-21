package com.termux.x11.utils;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Ready-made grid layouts for the additional (extra) keys bar.
 *
 * A layout is a columns x rows grid generated as an extra-keys JSON config. The
 * "custom" layout means "use the JSON from the extra keys config preference as is".
 */
public final class ExtraKeysLayouts {

    public static final String CUSTOM = "custom";

    private ExtraKeysLayouts() { }

    /**
     * Builds the extra-keys JSON for a layout.
     *
     * @return the JSON string, or {@code null} when the custom JSON should be used.
     */
    @Nullable
    public static String build(String layout) {
        if (layout == null || layout.isEmpty() || CUSTOM.equals(layout))
            return null;

        JSONArray rows = new JSONArray();
        switch (layout) {
            case "9x4":
                rows.put(row(fnRow()));
                rows.put(row(navRow()));
                rows.put(row(ctrlRow()));
                rows.put(row(bottomRow()));
                break;
            case "10x4":
                rows.put(row(extend(fnRow(), "INS")));
                rows.put(row(extend(navRow(), "PASTE")));
                rows.put(row(extend(ctrlRow(), "DEL")));
                rows.put(row(extend(bottomRow(), "B")));
                break;
            case "11x4":
                rows.put(row(extend(fnRow(), "INS", "DEL")));
                rows.put(row(extend(navRow(), "PASTE", "`")));
                rows.put(row(extend(ctrlRow(), ";", "'")));
                rows.put(row(extend(bottomRow(), "B", "N")));
                break;
            case "10x5":
                rows.put(row(digitRow()));
                rows.put(row(extend(fnRow(), "INS")));
                rows.put(row(extend(navRow(), "PASTE")));
                rows.put(row(extend(ctrlRow(), "DEL")));
                rows.put(row(extend(bottomRow(), "B")));
                break;
            case "11x5":
                rows.put(row(extend(digitRow(), "`")));
                rows.put(row(extend(fnRow(), "INS", "DEL")));
                rows.put(row(extend(navRow(), "PASTE", "`")));
                rows.put(row(extend(ctrlRow(), ";", "'")));
                rows.put(row(extend(bottomRow(), "B", "N")));
                break;
            default:
                return null;
        }
        return rows.toString();
    }

    private static Object[] fnRow() {
        return new Object[]{
                "F1", "F2", "F3", "F4", "F5", "F6",
                popup("F7", "F10"), popup("F8", "F11"), popup("F9", "F12")
        };
    }

    private static Object[] navRow() {
        return new Object[]{
                "ESC", "/", popup("-", "|"), "HOME", "UP", "END", "PGUP",
                "PREFERENCES", "MAPPER"
        };
    }

    private static Object[] ctrlRow() {
        return new Object[]{
                "TAB", "CTRL", "ALT", "LEFT", "DOWN", "RIGHT", "PGDN",
                "KEYBOARD", popup("BACKSPACE", "DELETE")
        };
    }

    private static Object[] bottomRow() {
        return new Object[]{
                "SHIFT", "META", "C", "X", "V", popup("SPACE", "A"), "Z", "P", "ENTER"
        };
    }

    private static Object[] digitRow() {
        return new Object[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    }

    private static Object[] extend(Object[] base, Object... extra) {
        Object[] result = new Object[base.length + extra.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(extra, 0, result, base.length, extra.length);
        return result;
    }

    private static JSONArray row(Object... cells) {
        JSONArray array = new JSONArray();
        for (Object cell : cells)
            array.put(cell);
        return array;
    }

    private static JSONObject popup(String key, String popup) {
        JSONObject object = new JSONObject();
        try {
            object.put("key", key);
            object.put("popup", popup);
        } catch (JSONException ignored) {
            // Keys are compile-time constants, this cannot happen.
        }
        return object;
    }
}
