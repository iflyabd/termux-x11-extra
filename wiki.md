# Extra keys JSON reference

The extra keys bar is configured with a small JSON document:

**Preferences → Extra keys → Extra keys config**

The other layouts in **Extra keys → Extra keys layout** are ready-made grids.
When a preset (`9x4`, `10x4`, `11x4`, `12x4`, `10x5`, `11x5`) is selected it
replaces the JSON below. Select **Custom (JSON config)** to use your own JSON.

The repository default (also used by the *Reset* action in the JSON dialog) is
a PC-style **5×11** bar: volume and media keys on top, the function key row,
the letter/navigation rows and the modifiers:

```json
[
  [{"key": "VOLUME_MUTE", "popup": "X11_VOLUME_MUTE"},
   {"key": "VOLUME_DOWN", "popup": "X11_VOLUME_DOWN"},
   {"key": "VOLUME_UP", "popup": "X11_VOLUME_UP"},
   {"key": "MEDIA_PREVIOUS", "popup": "X11_MEDIA_PREVIOUS"},
   {"key": "MEDIA_PLAY_PAUSE", "popup": "X11_MEDIA_PLAY_PAUSE"},
   {"key": "MEDIA_NEXT", "popup": "X11_MEDIA_NEXT"},
   "F11", "F12",
   "MOUSE_SCROLL_LEFT", "MOUSE_SCROLL_RIGHT", "MOUSE_SCROLL_UP"],

  ["F1", "F2", "F3", "F4", "F5", "F6", "F7",
   {"key": "F8", "popup": "DRAWER"},
   {"key": "F9", "popup": "F11"},
   {"key": "F10", "popup": "F12"},
   "MOUSE_LEFT"],

  ["ESC", "A", {"key": "Z", "popup": "|"}, "R", "P",
   "HOME", "PGUP", "INS", "KEYBOARD", "MAPPER",
   "MOUSE_MIDDLE"],

  ["TAB", "C", "X", "V", {"key": "S", "popup": "_"}, "END", "PGDN", "UP",
   "PREFERENCES", {"key": "BACKSPACE", "popup": "DELETE"},
   "MOUSE_RIGHT"],

  ["SHIFT", "META", "CTRL", "ALT", "/",
   {"key": "SPACE", "popup": "MOUSE_HELPER"},
   "LEFT", "DOWN", "RIGHT", "ENTER",
   "MOUSE_SCROLL_DOWN"]
]
```

What the rows contain:

* **Row 1** – the phone's volume keys and the Android media keys (swipe up for the
  same key inside the X11 session), `F11`/`F12` and the horizontal/vertical
  wheel keys.
* **Row 2** – `F1`–`F10` (swipe up on `F9`/`F10` for `F11`/`F12`, on `F8` for the
  settings) and the left mouse button.
* **Row 3** – `ESC`, the letter keys, navigation and the app keys, plus the
  middle mouse button.
* **Row 4** – the editing keys, `S` (swipe up for `_`) and `PREFERENCES`, plus the
  right mouse button.
* **Row 5** – modifiers, space, the arrow keys and the wheel-up key.

Single-quoted strings are also accepted by the parser (the built-in default
uses them), but standard double-quoted JSON is recommended.

> **One document, but pasted arrays are merged.** The whole configuration is a
> list of rows. If you paste several arrays at once (for example a new row and
> then the old rows) the app merges them, so nothing gets lost:
>
> ```json
> [[new row]], [[old row 1], [old row 2]]   →  3 rows
> ```

## Format

The value is an **array of rows**. Each row is an **array of cells**. All rows
must have the same number of cells.

A cell is either:

* a **string** – a key name from the tables below; unknown strings are typed as
  literal text (so `"A"` simply types `A`);
* an **object** with any of these fields:

| Field     | Type   | Meaning |
|-----------|--------|---------|
| `key`     | string | Key name or literal text. |
| `popup`   | string or object | Secondary key shown when you **swipe up** on the button. Can be a full object, e.g. `{"key":"F11","display":"XI"}`. |
| `display` | string | Label shown on the button (the key itself is unaffected). |
| `macro`   | string | Space-separated sequence of keys, e.g. `"CTRL C"`. A cell must have either `key` or `macro`, not both. |

Popups are triggered by **swiping up** on a key, not by long-pressing it.

## Key names

### Function, editing and navigation keys

| Name | Key |
|------|-----|
| `F1` … `F12` | Function keys |
| `ESC` (`ESCAPE`) | Escape |
| `TAB` | Tab |
| `ENTER` (`RETURN`) | Enter |
| `SPACE` | Space |
| `BKSP` (`BACKSPACE`) | Backspace |
| `DEL` (`DELETE`) | Delete (forward delete) |
| `INS` | Insert |
| `HOME`, `END` | Home, End |
| `PGUP` (`PAGEUP`, `PAGE_UP`, `PAGE UP`, `PAGE-UP`) | Page Up |
| `PGDN` (`PAGEDOWN`, `PAGE_DOWN`, `PAGE-DOWN`) | Page Down |
| `UP`, `DOWN`, `LEFT`, `RIGHT` (`UP`, `DN`, `LT`, `RT`) | Arrow keys |

`UP`, `DOWN`, `LEFT` and `RIGHT` are **always arrow keys**. Mouse actions
never use those names, so existing layouts cannot break.

### Modifiers

`CTRL` (`CONTROL`), `ALT`, `SHIFT` (`SHFT`) and `META` are sticky modifier
buttons: tap to hold, tap again to release. They can also be used inside macros.

### Application keys

| Name | Action |
|------|--------|
| `KEYBOARD` | Show/hide the on-screen keyboard |
| `PREFERENCES`, `DRAWER` | Open the app preferences |
| `MAPPER` | Open the key mapper |
| `PASTE` | Type the current clipboard text as key events |
| `MOUSE_HELPER` | Show/hide the floating mouse buttons |
| `STYLUS_HELPER` | Show/hide the stylus click overlay |
| `EXIT` | Exit Termux:X11 |

`preset_*` keys are internal hooks used by the key-mapper layout presets and
are not normally needed in a custom bar.

### Mouse buttons

The click is sent at the **current pointer position** (move the pointer with
the touchpad first). Each press is a normal press/release pair.

| Name | Action |
|------|--------|
| `MOUSE_LEFT`, `MOUSE_BUTTON_LEFT` | Left click |
| `MOUSE_MIDDLE`, `MOUSE_BUTTON_MIDDLE` | Middle click |
| `MOUSE_RIGHT`, `MOUSE_BUTTON_RIGHT` | Right click |

### Mouse wheel / scroll

Each press sends exactly **one wheel notch** (120 units).

| Name | Action |
|------|--------|
| `MOUSE_SCROLL_UP`, `SCROLL_UP`, `MOUSEWHEEL_UP` | Wheel up |
| `MOUSE_SCROLL_DOWN`, `SCROLL_DOWN`, `MOUSEWHEEL_DOWN` | Wheel down |
| `MOUSE_SCROLL_LEFT`, `SCROLL_LEFT`, `MOUSEWHEEL_LEFT` | Wheel left |
| `MOUSE_SCROLL_RIGHT`, `SCROLL_RIGHT`, `MOUSEWHEEL_RIGHT` | Wheel right |

The mouse and scroll names are case-insensitive. The `MOUSE_` /
`MOUSE_SCROLL_` prefixes guarantee that they can never collide with the arrow
key names.

### Short labels

The long key names are too wide for a button, so the app shows a short label
instead. You can always override it with `"display"`:

| Key | Label |
|-----|-------|
| `MOUSE_LEFT`, `MOUSE_MIDDLE`, `MOUSE_RIGHT` | `🖱L`, `🖱M`, `🖱R` |
| `MOUSE_SCROLL_UP` / `DOWN` / `LEFT` / `RIGHT` | `🖱▲`, `🖱▼`, `🖱◀`, `🖱▶` |
| `MEDIA_PLAY` / `PAUSE` / `PLAY_PAUSE` | `▶`, `⏸`, `⏯` |
| `MEDIA_STOP` / `NEXT` / `PREVIOUS` | `⏹`, `⏭`, `⏮` |
| `MEDIA_REWIND` / `FAST_FORWARD` | `⏪`, `⏩` |
| `X11_MEDIA_*` | the same symbol prefixed with `X` (`X⏯`, `X⏭`, ...) |
| `VOLUME_UP` / `DOWN` / `MUTE` | `🔊`, `🔉`, `🔇` |
| `X11_VOLUME_UP` / `DOWN` / `MUTE` | `X🔊`, `X🔉`, `X🔇` |

### Media keys

There are two families:

* `MEDIA_*` controls the **Android media session** (a music app, the phone's
  browser audio, headphones remote, etc.).
* `X11_MEDIA_*` sends media keys into the **X11 session** as normal keyboard
  events (`XF86AudioPlay`, `XF86AudioNext`, ...), for players running on the
  Linux desktop such as mpv, VLC or a desktop browser.

| Android media key | X11 media key | Action |
|-------------------|---------------|--------|
| `MEDIA_PLAY_PAUSE` | `X11_MEDIA_PLAY_PAUSE` | Play / pause |
| `MEDIA_PLAY`, `MEDIA_PAUSE` | `X11_MEDIA_PLAY`, `X11_MEDIA_PAUSE` | Play, pause |
| `MEDIA_NEXT` | `X11_MEDIA_NEXT` | Next track |
| `MEDIA_PREV`, `MEDIA_PREVIOUS` | `X11_MEDIA_PREV`, `X11_MEDIA_PREVIOUS` | Previous track |
| `MEDIA_STOP` | `X11_MEDIA_STOP` | Stop |
| `MEDIA_REWIND`, `MEDIA_FAST_FORWARD` | `X11_MEDIA_REWIND`, `X11_MEDIA_FAST_FORWARD` | Rewind, fast-forward |

### Android device volume

These change the phone's media volume, not the volume inside X11. The system
volume panel is shown.

| Name | Label | Action |
|------|-------|--------|
| `VOLUME_UP` | 🔊 | Volume up |
| `VOLUME_DOWN` | 🔉 | Volume down |
| `VOLUME_MUTE` | 🔇 | Toggle mute |

### X11 session volume

These send the standard XF86 volume keysyms (`XF86AudioRaiseVolume`,
`XF86AudioLowerVolume`, `XF86AudioMute`) into the desktop, so the volume of the
player or of the desktop mixer running inside X11 changes.

| Name | Label | Action |
|------|-------|--------|
| `X11_VOLUME_UP`, `X11_VOLUME_RAISE` | X🔊 | Volume up in X11 |
| `X11_VOLUME_DOWN`, `X11_VOLUME_LOWER` | X🔉 | Volume down in X11 |
| `X11_VOLUME_MUTE` | X🔇 | Toggle mute in X11 |

A handy pattern is to use the X11 key as the popup of the phone key:

```json
{"key": "VOLUME_UP", "popup": "X11_VOLUME_UP"}
```

## Examples

### A compact bar with mouse, scroll, media and volume keys

```json
[
  ["F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10"],

  ["ESC", "TAB", "CTRL", "ALT", "SHIFT",
   "MOUSE_LEFT", "MOUSE_MIDDLE", "MOUSE_RIGHT",
   "SCROLL_UP", "SCROLL_DOWN"],

  ["HOME", "END", "PGUP", "PGDN", "INS", "DEL",
   "MEDIA_PREVIOUS", "MEDIA_PLAY_PAUSE", "MEDIA_NEXT", "VOLUME_MUTE"],

  ["A", "C", "X", "V", "Z",
   {"key": "SPACE", "popup": "MOUSE_HELPER"},
   "VOLUME_DOWN", "VOLUME_UP", "VOLUME_MUTE", "ENTER"]
]
```

### Popups, labels and macros

```json
[
  [
    "ESC",
    {"key": "/", "popup": "|"},
    {"key": "BACKSPACE", "popup": "DELETE", "display": "⌫"},
    {"macro": "CTRL C", "display": "COPY"},
    {"macro": "CTRL V", "display": "PASTE"},
    {"key": "F9", "popup": "F11"},
    {"key": "F10", "popup": {"key": "F12", "display": "XII"}}
  ]
]
```

Notes:

* `{"key": "BACKSPACE", "popup": "DELETE"}` gives Backspace the Delete key on
  swipe-up.
* `{"macro": "CTRL C"}` holds Ctrl, taps C and releases Ctrl.
* `display` only changes the label, never the key.

## Clipboard sync

Clipboard sharing is text-only and bidirectional.

* **X11 → Android**: copying in an X11 application updates the Android
  clipboard. Android only accepts clipboard writes from the app that is in the
  foreground, so a copy that is made while Termux:X11 is in the background is
  written as soon as the app comes to the front again.
* **Android → X11**: Android 10 and newer do **not** allow a background app to
  read the clipboard. Changes made in other apps while X11 is not focused are
  therefore announced as soon as Termux:X11 regains focus (or when the X
  server (re)connects). This is an Android privacy restriction, not an app
  limitation.
* Repeated clipboard events with the same text are ignored, and text that was
  just written by X11 is not echoed back.
* Rich text is provided to X11 as plain text. Multi-item clips are joined with
  newlines; non-text items fall back to their textual coercion (e.g. a file
  URI).
* Clips that only contain URIs (a copied link or file) are **not** read.
  Android tries to hand the URI permission to the app that reads the clipboard
  and clears the clipboard when that fails, which would throw away what was
  just copied on the phone. Copy the link as plain text to sync it to X11.

If copies made inside X11 are delayed while the app is in the background,
disable battery optimization for Termux:X11; some OEM battery managers freeze
background apps, which also pauses their clipboard bridge.

> **The phone clipboard is empty right after copying?** Android clears the
> clipboard when an app reads it and the URI permission of the copied item
> cannot be granted to that app. A **scrcpy server that runs as root** (via
> `adb shell`) does exactly that: it watches the clipboard and wipes it a few
> milliseconds after every change. Start scrcpy with
> `--no-clipboard-autosync` to avoid it. The same can happen with any other
> root process that reads the clipboard.

## Tips

* Keep rows the same length; a mismatch is reported as a JSON error.
* Use `popup` for shifted symbols (`{ "key": "-", "popup": "|" }`) to save
  space.
* If your custom JSON was accepted but the bar does not change, make sure
  **Extra keys layout** is set to **Custom (JSON config)**.
* JSON syntax errors are reported in a toast and the built-in default layout is
  used until the JSON is fixed.
