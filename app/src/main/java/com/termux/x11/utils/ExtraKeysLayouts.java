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
 *
 * The presets are organised like a compact PC keyboard: a function-key strip on
 * top, the number row with shifted symbols in the five-row presets, a modifier
 * and punctuation row, a clipboard/utility row and a navigation/edit cluster
 * with Space/Enter at the bottom right. Long-press popups stay attached to the
 * key they belong to (F7-F9 carry F10-F12, Backspace carries Delete, and so on).
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
                rows.put(row("ESC", "TAB", "CTRL", "ALT", "SHIFT", "META",
                        popup("/", "\\"), popup("-", "|"), popup("BACKSPACE", "DELETE")));
                rows.put(row("PREFERENCES", "MAPPER", "KEYBOARD", "C", "X",
                        popup("V", "PASTE"), "Z", "P", "ENTER"));
                rows.put(row(popup("HOME", "INS"), "END", "PGUP", "PGDN",
                        "LEFT", "UP", "DOWN", "RIGHT", popup("SPACE", "A")));
                break;
            case "10x4":
                rows.put(row(escFnRow()));
                rows.put(row("TAB", "CTRL", "ALT", "SHIFT", "META",
                        popup("/", "\\"), popup("-", "|"),
                        popup("BACKSPACE", "DELETE"), "PASTE", "INS"));
                rows.put(row("PREFERENCES", "MAPPER", "KEYBOARD",
                        "C", "X", "V", "Z", "P", "B", "ENTER"));
                rows.put(row("HOME", "END", "PGUP", "PGDN",
                        "LEFT", "UP", "DOWN", "RIGHT", popup("SPACE", "A"), "DEL"));
                break;
            case "11x4":
                rows.put(row(escFnRow("INS")));
                rows.put(row("TAB", "CTRL", "ALT", "SHIFT", "META",
                        popup("/", "\\"), popup("-", "|"), popup(";", ":"),
                        popup("'", "\""), popup("`", "~"),
                        popup("BACKSPACE", "DELETE")));
                rows.put(row("PREFERENCES", "MAPPER", "KEYBOARD", "PASTE",
                        "C", "X", "V", "Z", "P", "B", "N"));
                rows.put(row("HOME", "END", "PGUP", "PGDN",
                        "LEFT", "UP", "DOWN", "RIGHT", popup("SPACE", "A"),
                        "DEL", "ENTER"));
                break;
            case "12x4":
                rows.put(row(escFnRow("INS", "DEL")));
                rows.put(row("TAB", "CTRL", "ALT", "SHIFT", "META",
                        popup("/", "\\"), popup("-", "|"), popup(";", ":"),
                        popup("'", "\""), popup("`", "~"),
                        popup("BACKSPACE", "DELETE"), "PASTE"));
                rows.put(row("PREFERENCES", "MAPPER", "KEYBOARD",
                        "C", "X", "V", "Z", "P", "B", "N",
                        popup("SPACE", "A"), "ENTER"));
                rows.put(row("HOME", "END", "PGUP", "PGDN",
                        "LEFT", "UP", "DOWN", "RIGHT",
                        popup("[", "{"), popup("]", "}"),
                        popup("(", ")"), popup("=", "+")));
                break;
            case "10x5":
                rows.put(row(escFnRow()));
                rows.put(row(digitRow()));
                rows.put(row("TAB", "CTRL", "ALT", "SHIFT", "META",
                        popup("/", "\\"), popup("-", "|"),
                        popup("BACKSPACE", "DELETE"), "PASTE", "INS"));
                rows.put(row("PREFERENCES", "MAPPER", "KEYBOARD",
                        "C", "X", "V", "Z", "P", "B", "ENTER"));
                rows.put(row("HOME", "END", "PGUP", "PGDN",
                        "LEFT", "UP", "DOWN", "RIGHT", popup("SPACE", "A"), "DEL"));
                break;
            case "11x5":
                rows.put(row(escFnRow("INS")));
                rows.put(row(digitRow(popup("`", "~"))));
                rows.put(row("TAB", "CTRL", "ALT", "SHIFT", "META",
                        popup("/", "\\"), popup("-", "|"), popup(";", ":"),
                        popup("'", "\""), popup("BACKSPACE", "DELETE"), "PASTE"));
                rows.put(row("PREFERENCES", "MAPPER", "KEYBOARD",
                        "C", "X", "V", "Z", "P", "B", "N", "ENTER"));
                rows.put(row("HOME", "END", "PGUP", "PGDN", "DEL",
                        "LEFT", "UP", "DOWN", "RIGHT", popup("SPACE", "A"),
                        popup("=", "+")));
                break;
            default:
                return null;
        }
        return rows.toString();
    }

    /** Function row without Esc: F1-F9, with F10-F12 on the long-press popups. */
    private static Object[] fnRow() {
        return new Object[]{
                "F1", "F2", "F3", "F4", "F5", "F6",
                popup("F7", "F10"), popup("F8", "F11"), popup("F9", "F12")
        };
    }

    /** PC-style top row: Esc on the left, then F1-F9 and optional Ins/Del keys. */
    private static Object[] escFnRow(Object... trailing) {
        Object[] result = new Object[1 + 9 + trailing.length];
        result[0] = "ESC";
        System.arraycopy(fnRow(), 0, result, 1, 9);
        System.arraycopy(trailing, 0, result, 10, trailing.length);
        return result;
    }

    /** Number row with the shifted symbol on long press, like a real PC number row. */
    private static Object[] digitRow(Object... trailing) {
        Object[] result = new Object[10 + trailing.length];
        result[0] = popup("1", "!");
        result[1] = popup("2", "@");
        result[2] = popup("3", "#");
        result[3] = popup("4", "$");
        result[4] = popup("5", "%");
        result[5] = popup("6", "^");
        result[6] = popup("7", "&");
        result[7] = popup("8", "*");
        result[8] = popup("9", "(");
        result[9] = popup("0", ")");
        System.arraycopy(trailing, 0, result, 10, trailing.length);
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
