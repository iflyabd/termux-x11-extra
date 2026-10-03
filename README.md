# termux-x11-extra-0.11.1-modified — Source Patch Files

## What this zip contains

All files that were **modified or created** to build `termux-x11-extra-0.11.1-modified.zip`
on top of the official [termux-x11](https://github.com/termux/termux-x11) base repo.

Drop these files over a fresh clone of termux-x11 and build with `./gradlew assembleDebug`.

---

## Files included

### Build configuration
| File | Change |
|------|--------|
| `app/build.gradle` | Added `ndkVersion`, `recyclerview:1.3.2`, `appcompat:1.7.0` deps |
| `gradle.properties` | JVM heap 3072 m, daemon/parallel off |
| `local.properties` | `sdk.dir=/home/user/android-sdk` (adjust to your SDK path) |

### Android configuration
| File | Change |
|------|--------|
| `app/src/main/AndroidManifest.xml` | Added `VirtualKeyMapperActivity` declaration |

### Java source (from original modified zip)
| File | Description |
|------|-------------|
| `MainActivity.java` | Added command/support buttons + virtual key overlay |
| `LoriePreferences.java` | Added gamepad prefs, virtual key export/import |
| `VirtualKeyMapperActivity.java` | New — full virtual key mapper screen |
| `VirtualKeyAdapter.java` | New — RecyclerView adapter for virtual keys |
| `utils/PresetManager.java` | New — preset save/load for virtual key layouts |
| `utils/*.java` | Unchanged utility classes (included for completeness) |

### Resources
| File | Change |
|------|--------|
| `res/xml/preferences.xml` | **Complete rewrite** — added `app:title` on every preference; added Gamepad section |
| `res/values/strings.xml` | Added `pref_<key>` aliases (required by `LoriePreferences.findId()` to set titles at runtime); added `pref_summary_*` aliases; added `notification_content_text`, `command_button_text`, `support_button_text` |
| `res/values/arrays.xml` | Added 6 string-arrays for Gamepad ListPreference options |
| `res/values/ids.xml` | New — `slideable_flag`, `toggleable_flag` IDs for VirtualKeyMapperActivity |
| `res/layout/main_activity.xml` | Added `command_button`, `support_button`, `top` overlay FrameLayout |
| `res/layout/activity_virtual_key_mapper.xml` | New — layout for VirtualKeyMapperActivity |
| `res/layout/list_item_virtual_key.xml` | New — RecyclerView item for VirtualKeyAdapter |
| `res/menu/button_options_menu.xml` | New — context menu for virtual key buttons |

---

## Key bug fix: Settings labels were blank

`LoriePreferences.java` sets preference titles **programmatically** at runtime using:

```java
int findId(String name) {
    return getResources().getIdentifier("pref_" + name, "string", packageName);
}
if ((id = findId(p.getKey())) != 0)
    p.setTitle(getResources().getString(id));
```

It looks for string resources named `pref_<key>` (e.g. `pref_touchMode`).  
The original strings were all named `lorie_pref_*`, so `findId()` always returned 0
and **every preference showed a blank label**.

**Fix**: `strings.xml` now contains `pref_<key>` aliases for every preference key,
so `findId()` resolves correctly and titles render.

---

## How to rebuild from scratch

```bash
# 1. Clone the base repo
git clone https://github.com/termux/termux-x11.git
cd termux-x11
git submodule update --init --recursive

# 2. Overlay these patch files (adjust paths as needed)
cp -r /path/to/this/zip/* .

# 3. Set your SDK path
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# 4. Build
./gradlew assembleDebug --no-daemon
```
