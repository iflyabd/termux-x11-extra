package com.termux.x11.utils;

import static com.termux.shared.termux.extrakeys.ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS;
import static com.termux.x11.MainActivity.ACTION_CUSTOM;
import static com.termux.x11.MainActivity.getInstance;
import static com.termux.x11.MainActivity.handler;
import static com.termux.x11.MainActivity.toggleKeyboardVisibility;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.termux.shared.termux.extrakeys.*;
import com.termux.x11.LoriePreferences;
import com.termux.x11.LorieView;
import com.termux.x11.MainActivity;
import com.termux.x11.Prefs;
import com.termux.x11.R;
import com.termux.x11.VirtualKeyMapperActivity;
import com.termux.x11.MainActivity;
import com.termux.x11.input.InputStub;
import com.termux.x11.input.TouchInputHandler;
import com.termux.x11.input.VirtualKeyHandler;

import org.json.JSONException;

import java.util.Locale;

public class TermuxX11ExtraKeys implements ExtraKeysView.IExtraKeysView {
    @SuppressWarnings("FieldCanBeLocal")
    private static final String LOG_TAG = "TermuxX11ExtraKeys";
    private final View.OnKeyListener mEventListener;
    private final MainActivity mActivity;
    private final ExtraKeysView mExtraKeysView;
    private final ClipboardManager mClipboardManager;
    static private ExtraKeysInfo mExtraKeysInfo;

    private boolean ctrlDown;
    private boolean altDown;
    private boolean shiftDown;
    private boolean metaDown;

    /**
     * Default extra-keys JSON: a PC/hacker-style 4x10 keyboard.
     * F1-F12 on the first row (F11/F12 as popups of F9/F10, settings as F8 popup),
     * letters/navigation on the middle rows and modifiers/punctuation on the last row.
     * See {@code wiki.md} for the full key reference (including MOUSE_*, MEDIA_*, VOLUME_*).
     */
    public static final String DEFAULT_IVALUE_EXTRA_KEYS = "[['F1','F2','F3','F4','F5','F6','F7',{key:'F8',popup:'DRAWER'},{key:'F9',popup:'F11'},{key:'F10',popup:'F12'}],['ESC','A',{key:'Z',popup:'|'},'R','P','HOME','PGUP','INS','KEYBOARD','MAPPER'],['TAB','C','X','V','-','END','PGDN','UP','PREFERENCES',{key:'BACKSPACE',popup:'DELETE'}],['SHIFT','META','CTRL','ALT','/',{key:'SPACE',popup:'MOUSE_HELPER'},'LEFT','DOWN','RIGHT','ENTER']]";

    /** One wheel notch in the smooth-scroll units used by {@link LorieView#sendMouseWheelEvent}. */
    private static final float SCROLL_NOTCH = 120f;

    public TermuxX11ExtraKeys(@NonNull View.OnKeyListener eventlistener, MainActivity activity, ExtraKeysView extrakeysview) {
        mEventListener = eventlistener;
        mActivity = activity;
        mExtraKeysView = extrakeysview;
        applyAppearanceFromPreferences();
        mClipboardManager = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /** Applies the user-selected colors, rounded borders, font and text size to the extra keys bar. */
    private void applyAppearanceFromPreferences() {
        Prefs prefs = MainActivity.getPrefs();
        if (prefs == null) {
            // Fallback used only if the view is inflated before preferences are ready.
            mExtraKeysView.setButtonColors(
                    ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR,
                    ExtraKeysView.DEFAULT_BUTTON_ACTIVE_TEXT_COLOR,
                    ExtraKeysView.DEFAULT_BUTTON_BACKGROUND_COLOR,
                    ExtraKeysView.DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR);
            return;
        }

        int textColor = ExtraKeysThemes.parseColor(prefs.extraKeysTextColor.get(),
                ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR);
        int activeTextColor = ExtraKeysThemes.parseColor(prefs.extraKeysActiveTextColor.get(),
                ExtraKeysView.DEFAULT_BUTTON_ACTIVE_TEXT_COLOR);
        int backgroundColor = ExtraKeysThemes.parseColor(prefs.extraKeysBackgroundColor.get(),
                ExtraKeysView.DEFAULT_BUTTON_BACKGROUND_COLOR);
        int activeBackgroundColor = ExtraKeysThemes.parseColor(prefs.extraKeysActiveBackgroundColor.get(),
                ExtraKeysView.DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR);
        int borderColor = ExtraKeysThemes.parseColor(prefs.extraKeysBorderColor.get(),
                ExtraKeysView.DEFAULT_BUTTON_BORDER_COLOR);

        mExtraKeysView.setButtonColors(textColor, activeTextColor, backgroundColor, activeBackgroundColor);
        mExtraKeysView.setButtonAppearance(
                borderColor,
                dp(prefs.extraKeysBorderWidthTenths.get() / 10f),
                dp(prefs.extraKeysBorderRadius.get()),
                prefs.extraKeysTextSize.get(),
                ExtraKeysThemes.typeface(prefs.extraKeysFont.get()),
                Math.round(dp(prefs.extraKeysKeyGap.get())),
                dp(prefs.extraKeysGlowRadius.get()));
        mExtraKeysView.setButtonBorderImage(prefs.extraKeysBorderImageEnabled.get()
                ? prefs.extraKeysBorderImage.get() : null);
    }

    private float dp(int value) {
        return dp((float) value);
    }

    private float dp(float value) {
        return value * mActivity.getResources().getDisplayMetrics().density;
    }

    private final KeyCharacterMap mVirtualKeyboardKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
    static final String ACTION_START_PREFERENCES_ACTIVITY = "com.termux.x11.start_preferences_activity";
    static final String ACTION_START_MAPPER_ACTIVITY = "com.termux.x11.start_virtualkeymapperactivity";

    @Override
    public void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, Button button) {
        if (buttonInfo.macro) {
            String[] keys = buttonInfo.key.split(" ");
            boolean ctrlDown = false, altDown = false, shiftDown = false, metaDown = false, fnDown = false;
            for (String key : keys) {
                if (SpecialButton.CTRL.getKey().equals(key))
                    ctrlDown = true;
                else if (SpecialButton.ALT.getKey().equals(key))
                    altDown = true;
                else if (SpecialButton.SHIFT.getKey().equals(key))
                    shiftDown = true;
                else if (SpecialButton.META.getKey().equals(key))
                    metaDown = true;
                else if (SpecialButton.FN.getKey().equals(key))
                    fnDown = true;
            }

            for (String key : keys) {
                if (!SpecialButton.CTRL.getKey().equals(key)
                        && !SpecialButton.ALT.getKey().equals(key)
                        && !SpecialButton.SHIFT.getKey().equals(key)
                        && !SpecialButton.META.getKey().equals(key)
                        && !SpecialButton.FN.getKey().equals(key))
                    onLorieExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, metaDown, fnDown);
            }

            if (ctrlDown || altDown || shiftDown || metaDown || fnDown) {
                onLorieExtraKeyButtonClick(view, null, false, false, false, false, false);
                unsetSpecialKeys();
            }
        } else {
            onLorieExtraKeyButtonClick(view, buttonInfo.key, false, false, false, false, false);
        }
    }

    protected void onTerminalExtraKeyButtonClick(@SuppressWarnings("unused") View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean metaDown, @SuppressWarnings("unused") boolean fnDown) {
        if (this.ctrlDown != ctrlDown) {
            this.ctrlDown = ctrlDown;
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, ctrlDown);
        }

        if (this.altDown != altDown) {
            this.altDown = altDown;
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_ALT_LEFT, altDown);
        }

        if (this.shiftDown != shiftDown) {
            this.shiftDown = shiftDown;
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_SHIFT_LEFT, shiftDown);
        }

        if (this.metaDown != metaDown) {
            this.metaDown = metaDown;
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_META_LEFT, metaDown);
        }

        if (PRIMARY_KEY_CODES_FOR_STRINGS.containsKey(key)) {
            Integer keyCode = PRIMARY_KEY_CODES_FOR_STRINGS.get(key);
            if (keyCode == null) return;

            int scanCode = 0;

            // Workaround for SPECIAL keys (evdev scancodes)
            switch (key) {
                // Control/navigation
                case "SPACE": scanCode = 57;  break;
                case "ESC":   scanCode = 1;   break;
                case "TAB":   scanCode = 15;  break;
                case "ENTER": scanCode = 28;  break;
                case "BKSP":  scanCode = 14;  break;
                case "DEL":   scanCode = 111; break;
                case "INS":   scanCode = 110; break;
                case "HOME":  scanCode = 102; break;
                case "END":   scanCode = 107; break;
                case "PGUP":  scanCode = 104; break;
                case "PGDN":  scanCode = 109; break;

                case "UP":    scanCode = 103; break;
                case "DOWN":  scanCode = 108; break;
                case "LEFT":  scanCode = 105; break;
                case "RIGHT": scanCode = 106; break;

                case "F1":  scanCode = 59; break;
                case "F2":  scanCode = 60; break;
                case "F3":  scanCode = 61; break;
                case "F4":  scanCode = 62; break;
                case "F5":  scanCode = 63; break;
                case "F6":  scanCode = 64; break;
                case "F7":  scanCode = 65; break;
                case "F8":  scanCode = 66; break;
                case "F9":  scanCode = 67; break;
                case "F10": scanCode = 68; break;
                case "F11": scanCode = 87; break;
                case "F12": scanCode = 88; break;
            }

            // Pass the Android keycode as fallback: native uses scanCode when non-zero and
            // otherwise maps keyCode (this is what makes SPACE work — it has no scanCode entry).
            mActivity.getLorieView().sendKeyEvent(scanCode, keyCode, true);
            mActivity.getLorieView().sendKeyEvent(scanCode, keyCode, false);

        }
        else if (key != null) {
            // not a control char
            mActivity.getLorieView().sendTextEvent(key.getBytes(UTF_8));
        }
    }

    public void unsetSpecialKeys() {
        if (mExtraKeysView == null)
            return;

        if (Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.CTRL, true)))
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, false);
        if (Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.ALT, true)))
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_ALT_LEFT, false);
        if (Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.SHIFT, true)))
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_SHIFT_LEFT, false);
        if (Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.META, true)))
            mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_META_LEFT, false);
    }

    @Override
    public boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, Button button) {
        handler.postDelayed(() -> {
            boolean pressed;
            switch (buttonInfo.key) {
                case "CTRL":
                    pressed = Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.CTRL, false));
                    mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, pressed);
                    break;
                case "ALT":
                    pressed = Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.ALT, false));
                    mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_ALT_LEFT, pressed);
                    break;
                case "SHIFT":
                    pressed = Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.SHIFT, false));
                    mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_SHIFT_LEFT, pressed);
                    break;
                case "META":
                    pressed = Boolean.TRUE.equals(mExtraKeysView.readSpecialButton(SpecialButton.META, false));
                    mActivity.getLorieView().sendKeyEvent(0, KeyEvent.KEYCODE_META_LEFT, pressed);
                    break;
            }
        }, 100);

        return false;
    }

    @SuppressLint("RtlHardcoded")
    public void onLorieExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean metaDown, boolean fnDown) {
        if (key == null) {
            onTerminalExtraKeyButtonClick(view, null, ctrlDown, altDown, shiftDown, metaDown, fnDown);
            return;
        }
        if ("KEYBOARD".equals(key))
            toggleKeyboardVisibility(mActivity);
        else if ("MAPPER".equals(key))
            mActivity.startActivity(new Intent(mActivity, VirtualKeyMapperActivity.class) {{ setAction(ACTION_START_MAPPER_ACTIVITY); }});
        else if (key.startsWith("preset_")) {
            Context context = getInstance();
            Activity activity = (Activity) context;
            FrameLayout container = activity.findViewById(R.id.top);
            MainActivity act = (MainActivity) activity;
            VirtualKeyHandler handler = new VirtualKeyHandler(
                    context,
                    act.getLorieView(),                          // sau null dacă nu vrei deloc Lorie
                    act.getGamepadIpc(),
                    act.getGamepadState(),
                    act.getGamepadHandler());

            PresetManager.loadPresetAndAddToUI(context, key, container, handler);
        }
        else if ("DRAWER".equals(key) || "PREFERENCES".equals(key))
            mActivity.startActivity(new Intent(mActivity, LoriePreferences.class) {{ setAction(ACTION_START_PREFERENCES_ACTIVITY); }});
        else if ("EXIT".equals(key))
            mActivity.finish();
        else if ("PASTE".equals(key)) {
            ClipData clipData = mClipboardManager.getPrimaryClip();
            if (clipData != null) {
                CharSequence pasted = clipData.getItemAt(0).coerceToText(mActivity);
                if (!TextUtils.isEmpty(pasted)) {
                    KeyEvent[] events = mVirtualKeyboardKeyCharacterMap.getEvents(pasted.toString().toCharArray());
                    if (events != null)
                        for (KeyEvent event : events)
                            mEventListener.onKey(mActivity.getLorieView(), event.getKeyCode(), event);
                }
            }
        } else if ("MOUSE_HELPER".equals(key))
            mActivity.toggleMouseAuxButtons();
        else if ("STYLUS_HELPER".equals(key))
            mActivity.toggleStylusAuxButtons();
        else if (key.startsWith("MOUSE_") || key.startsWith("SCROLL_") || key.startsWith("MOUSEWHEEL_"))
            onMouseExtraKey(key);
        else if (key.startsWith("X11_MEDIA_"))
            onX11MediaExtraKey(key);
        else if (key.startsWith("MEDIA_"))
            onMediaExtraKey(key);
        else if (key.startsWith("VOLUME_"))
            onVolumeExtraKey(key);
        else
            onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, metaDown, fnDown);
    }

    /**
     * Mouse button and wheel keys for the extra-keys JSON.
     *
     * <p>All names are prefixed with {@code MOUSE_} / {@code MOUSE_SCROLL_} (or the
     * {@code SCROLL_} / {@code MOUSEWHEEL_} aliases) on purpose: the bare names
     * {@code LEFT}, {@code RIGHT}, {@code UP} and {@code DOWN} stay reserved for the
     * arrow keys and can never be turned into mouse actions.</p>
     *
     * <p>Supported: {@code MOUSE_LEFT}, {@code MOUSE_MIDDLE}, {@code MOUSE_RIGHT}
     * (plus {@code MOUSE_BUTTON_LEFT/MIDDLE/RIGHT} aliases) and the four wheel
     * directions {@code MOUSE_SCROLL_UP/DOWN/LEFT/RIGHT}.</p>
     */
    private void onMouseExtraKey(@NonNull String rawKey) {
        LorieView lorieView = mActivity.getLorieView();
        if (lorieView == null)
            return;

        String key = rawKey.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (key.startsWith("MOUSEWHEEL_"))
            key = "MOUSE_SCROLL_" + key.substring("MOUSEWHEEL_".length());

        switch (key) {
            case "MOUSE_LEFT":
            case "MOUSE_BUTTON_LEFT":
                clickMouseButton(lorieView, InputStub.BUTTON_LEFT);
                break;
            case "MOUSE_MIDDLE":
            case "MOUSE_BUTTON_MIDDLE":
                clickMouseButton(lorieView, InputStub.BUTTON_MIDDLE);
                break;
            case "MOUSE_RIGHT":
            case "MOUSE_BUTTON_RIGHT":
                clickMouseButton(lorieView, InputStub.BUTTON_RIGHT);
                break;
            case "MOUSE_SCROLL_UP":
            case "SCROLL_UP":
                lorieView.sendMouseWheelEvent(0f, -SCROLL_NOTCH);
                break;
            case "MOUSE_SCROLL_DOWN":
            case "SCROLL_DOWN":
                lorieView.sendMouseWheelEvent(0f, SCROLL_NOTCH);
                break;
            case "MOUSE_SCROLL_LEFT":
            case "SCROLL_LEFT":
                lorieView.sendMouseWheelEvent(-SCROLL_NOTCH, 0f);
                break;
            case "MOUSE_SCROLL_RIGHT":
            case "SCROLL_RIGHT":
                lorieView.sendMouseWheelEvent(SCROLL_NOTCH, 0f);
                break;
        }
    }

    /** Presses and releases an X11 mouse button at the current pointer position. */
    private void clickMouseButton(LorieView lorieView, int button) {
        lorieView.sendMouseEvent(0f, 0f, button, true, true);
        handler.postDelayed(() -> {
            LorieView view = mActivity.getLorieView();
            if (view != null)
                view.sendMouseEvent(0f, 0f, button, false, true);
        }, 60);
    }

    /**
     * Media keys handled by Android's media session (whatever is playing on the
     * device: a music app, a browser, etc.).
     */
    private void onMediaExtraKey(@NonNull String rawKey) {
        int keyCode;
        switch (rawKey.toUpperCase(Locale.ROOT)) {
            case "MEDIA_PLAY_PAUSE": keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE; break;
            case "MEDIA_PLAY":       keyCode = KeyEvent.KEYCODE_MEDIA_PLAY; break;
            case "MEDIA_PAUSE":      keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE; break;
            case "MEDIA_NEXT":       keyCode = KeyEvent.KEYCODE_MEDIA_NEXT; break;
            case "MEDIA_PREV":
            case "MEDIA_PREVIOUS":   keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS; break;
            case "MEDIA_STOP":       keyCode = KeyEvent.KEYCODE_MEDIA_STOP; break;
            case "MEDIA_REWIND":     keyCode = KeyEvent.KEYCODE_MEDIA_REWIND; break;
            case "MEDIA_FAST_FORWARD": keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD; break;
            default: return;
        }

        AudioManager audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null)
            return;

        long now = SystemClock.uptimeMillis();
        audioManager.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
    }

    /**
     * Media keys sent to X11 as regular keyboard events (XF86AudioPlay, XF86AudioNext,
     * ...). Use these when the media player runs inside the X11 session (mpv, VLC,
     * a desktop browser, ...) instead of as an Android media session.
     */
    private void onX11MediaExtraKey(@NonNull String rawKey) {
        int keyCode;
        switch (rawKey.toUpperCase(Locale.ROOT)) {
            case "X11_MEDIA_PLAY_PAUSE": keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE; break;
            case "X11_MEDIA_PLAY":       keyCode = KeyEvent.KEYCODE_MEDIA_PLAY; break;
            case "X11_MEDIA_PAUSE":      keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE; break;
            case "X11_MEDIA_NEXT":       keyCode = KeyEvent.KEYCODE_MEDIA_NEXT; break;
            case "X11_MEDIA_PREV":
            case "X11_MEDIA_PREVIOUS":   keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS; break;
            case "X11_MEDIA_STOP":       keyCode = KeyEvent.KEYCODE_MEDIA_STOP; break;
            case "X11_MEDIA_REWIND":     keyCode = KeyEvent.KEYCODE_MEDIA_REWIND; break;
            case "X11_MEDIA_FAST_FORWARD": keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD; break;
            default: return;
        }

        LorieView lorieView = mActivity.getLorieView();
        if (lorieView == null)
            return;

        lorieView.sendKeyEvent(0, keyCode, true);
        lorieView.sendKeyEvent(0, keyCode, false);
    }

    /**
     * Android device volume keys (media stream), independent of the X11 session.
     */
    private void onVolumeExtraKey(@NonNull String rawKey) {
        AudioManager audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null)
            return;

        switch (rawKey.toUpperCase(Locale.ROOT)) {
            case "VOLUME_UP":
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            case "VOLUME_DOWN":
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                break;
            case "VOLUME_MUTE":
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                break;
        }
    }

    /**
     * Set the terminal extra keys and style.
     */
    public static void setExtraKeys() {
        mExtraKeysInfo = null;

        Prefs prefs = MainActivity.getPrefs();
        // A ready-made layout overrides the JSON config; "custom" keeps the user's JSON.
        String layoutJson = prefs == null ? null : ExtraKeysLayouts.build(prefs.extraKeysLayout.get());
        String extrakeys = layoutJson != null ? layoutJson
                : (prefs != null ? prefs.extra_keys_config.get() : DEFAULT_IVALUE_EXTRA_KEYS);

        try {
            // The mMap stores the extra key and style string values while loading properties
            // Check {@link #getExtraKeysInternalPropertyValueFromValue(String)} and
            // {@link #getExtraKeysStyleInternalPropertyValueFromValue(String)}
            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, "extra-keys-style", ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            Toast.makeText(getInstance(), "Could not load and set the \"extra-keys\" property from the properties file: " + e, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Could not load and set the \"extra-keys\" property from the properties file: ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(TermuxX11ExtraKeys.DEFAULT_IVALUE_EXTRA_KEYS, "default", ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (JSONException e2) {
                Toast.makeText(getInstance(), "Can't create default extra keys", Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    public static ExtraKeysInfo getExtraKeysInfo() {
        if (mExtraKeysInfo == null)
            setExtraKeys();
        return mExtraKeysInfo;
    }
}
