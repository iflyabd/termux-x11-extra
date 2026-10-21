package com.termux.shared.termux.extrakeys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

import java.util.Map;
import java.util.stream.Collectors;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.x11.utils.TermuxX11ExtraKeys;

/**
 * A {@link View} showing extra keys (such as Escape, Ctrl, Alt) not normally available on an Android soft
 * keyboards.
 * <p>
 * To use it, add following to a layout file and import it in your activity layout file or inflate
 * it with a {@link androidx.viewpager.widget.ViewPager}.:
 * {@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <com.termux.shared.termux.extrakeys.ExtraKeysView xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:id="@+id/extra_keys"
 *     style="?android:attr/buttonBarStyle"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     android:layout_alignParentBottom="true"
 *     android:orientation="horizontal" />
 * }
 *
 * Then in your activity, get its reference by a call to {@link android.app.Activity#findViewById(int)}
 * or {@link LayoutInflater#inflate(int, ViewGroup)} if using {@link androidx.viewpager.widget.ViewPager}.
 * Then call {@link #setExtraKeysViewClient(IExtraKeysView)} and pass it the implementation of
 * {@link IExtraKeysView} so that you can receive callbacks. You can also override other values set
 * in {@link ExtraKeysView#ExtraKeysView(Context, AttributeSet)} by calling the respective functions.
 * If you extend {@link ExtraKeysView}, you can also set them in the constructor, but do call super().
 * <p>
 * After this you will have to make a call to {@link ExtraKeysView#reload() and pass
 * it the {@link ExtraKeysInfo} to load and display the extra keys. Read its class javadocs for more
 * info on how to create it.
 * <p>
 * Termux app defines the view in res/layout/view_terminal_toolbar_extra_keys and
 * inflates it in TerminalToolbarViewPager.instantiateItem() and sets the {@link ExtraKeysView} client
 * and calls {@link ExtraKeysView#reload(ExtraKeysInfo).
 * The {@link ExtraKeysInfo} is created by TermuxAppSharedProperties.setExtraKeys().
 * Then its got and the view height is adjusted in TermuxActivity.setTerminalToolbarHeight().
 * The client used is TermuxTerminalExtraKeys, which extends
 * {@link com.termux.x11.utils.TermuxX11ExtraKeys } to handle Termux app specific logic and
 * leave the rest to the super class.
 */
public final class ExtraKeysView extends GridLayout {
    /** The client for the {@link ExtraKeysView}. */
    public interface IExtraKeysView {
        /**
         * This is called by {@link ExtraKeysView} when a button is clicked. This is also called
         * for {@link #mRepetitiveKeys} and {@link ExtraKeyButton} that have a popup set.
         * However, this is not called for {@link #mSpecialButtons}, whose state can instead be read
         * via a call to {@link #readSpecialButton(SpecialButton, boolean)}.
         *
         * @param view The view that was clicked.
         * @param buttonInfo The {@link ExtraKeyButton} for the button that was clicked.
         *                   The button may be a {@link ExtraKeyButton#KEY_MACRO} set which can be
         *                   checked with a call to {@link ExtraKeyButton#macro}.
         * @param button The {@link Button} that was clicked.
         */
        void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, Button button);

        /**
         * This is called by {@link ExtraKeysView} when a button is clicked so that the client
         * can perform any hepatic feedback. This is only called in the {@link Button.OnClickListener}
         * and not for every repeat. Its also called for {@link #mSpecialButtons}.
         *
         * @param view The view that was clicked.
         * @param buttonInfo The {@link ExtraKeyButton} for the button that was clicked.
         * @param button The {@link Button} that was clicked.
         * @return Return {@code true} if the client handled the feedback, otherwise {@code false}
         * so that {@link ExtraKeysView#performExtraKeyButtonHapticFeedback(View, ExtraKeyButton, Button)}
         * can handle it depending on system settings.
         */
        boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, Button button);
    }

    /** Defines the default value for {@link #mButtonTextColor} */
    public static final int DEFAULT_BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    /** Defines the default value for {@link #mButtonActiveTextColor} - RED like Termux FDroid (was cyan 0xFF80DEEA) */
    public static final int DEFAULT_BUTTON_ACTIVE_TEXT_COLOR = 0xFFFF0000;
    /** Defines the default value for {@link #mButtonBackgroundColor} */
    public static final int DEFAULT_BUTTON_BACKGROUND_COLOR = 0x00000000;
    /** Defines the default value for {@link #mButtonActiveBackgroundColor} */
    public static final int DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR = 0xFF7F7F7F;
    /** Defines the default value for {@link #mButtonBorderColor}. */
    public static final int DEFAULT_BUTTON_BORDER_COLOR = 0x33FFFFFF;
    /** Defines the default corner radius of the extra keys buttons, in dp. */
    public static final float DEFAULT_BUTTON_CORNER_RADIUS_DP = 6f;
    /** Defines the default border width of the extra keys buttons, in dp. */
    public static final float DEFAULT_BUTTON_BORDER_WIDTH_DP = 1f;
    /** Defines the default gap between extra keys buttons, in dp. */
    public static final float DEFAULT_BUTTON_GAP_DP = 2f;

    /** Defines the minimum allowed duration in milliseconds for {@link #mLongPressTimeout}. */
    public static final int MIN_LONG_PRESS_DURATION = 200;
    /** Defines the maximum allowed duration in milliseconds for {@link #mLongPressTimeout}. */
    public static final int MAX_LONG_PRESS_DURATION = 3000;
    /** Defines the fallback duration in milliseconds for {@link #mLongPressTimeout}. */
    public static final int FALLBACK_LONG_PRESS_DURATION = 400;

    /** Defines the minimum allowed duration in milliseconds for {@link #mLongPressRepeatDelay}. */
    public static final int MIN_LONG_PRESS__REPEAT_DELAY = 5;
    /** Defines the maximum allowed duration in milliseconds for {@link #mLongPressRepeatDelay}. */
    public static final int MAX_LONG_PRESS__REPEAT_DELAY = 2000;
    /** Defines the default duration in milliseconds for {@link #mLongPressRepeatDelay}. */
    public static final int DEFAULT_LONG_PRESS_REPEAT_DELAY = 80;



    /** The implementation of the {@link IExtraKeysView} that acts as a client for the {@link ExtraKeysView}. */
    private IExtraKeysView mExtraKeysViewClient;

    /** The map for the {@link SpecialButton} and their {@link SpecialButtonState}. Defaults to
     * the one returned by {@link #getDefaultSpecialButtons(ExtraKeysView)}. */
    private Map<SpecialButton, SpecialButtonState> mSpecialButtons;

    /** The keys for the {@link SpecialButton} added to {@link #mSpecialButtons}. This is automatically
     * set when the call to {@link #setSpecialButtons(Map)} is made. */
    private Set<String> mSpecialButtonsKeys;


    /**
     * The list of keys for which auto repeat of key should be triggered if its extra keys button
     * is long pressed. This is done by calling {@link IExtraKeysView#onExtraKeyButtonClick(View, ExtraKeyButton, Button)}
     * every {@link #mLongPressRepeatDelay} seconds after {@link #mLongPressTimeout} has passed.
     * The default keys are defined by {@link ExtraKeysConstants#PRIMARY_REPETITIVE_KEYS}.
     */
    private List<String> mRepetitiveKeys;


    /** The text color for the extra keys button. Defaults to {@link #DEFAULT_BUTTON_TEXT_COLOR}. */
    private int mButtonTextColor;
    /** The text color for the extra keys button when its active.
     * Defaults to {@link #DEFAULT_BUTTON_ACTIVE_TEXT_COLOR}. */
    private int mButtonActiveTextColor;
    /** The background color for the extra keys button. Defaults to {@link #DEFAULT_BUTTON_BACKGROUND_COLOR}. */
    private int mButtonBackgroundColor;
    /** The background color for the extra keys button when its active. Defaults to
     * {@link #DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR}. */
    private int mButtonActiveBackgroundColor;
    /** The border color for the extra keys button. */
    private int mButtonBorderColor = DEFAULT_BUTTON_BORDER_COLOR;
    /** The border width in pixels. */
    private float mButtonBorderWidth = 0f;
    /** The corner radius in pixels. */
    private float mButtonCornerRadius = 0f;
    /** The text size in sp, or -1 to keep the platform default. */
    private float mButtonTextSizeSp = -1f;
    /** The typeface for the extra keys button, or null to keep the platform default. */
    private Typeface mButtonTypeface = null;
    /** The gap between buttons in pixels. */
    private int mButtonGap = 0;


    /**
     * Defines the duration in milliseconds before a press turns into a long press. The default
     * duration used is the one returned by a call to {@link ViewConfiguration#getLongPressTimeout()}
     * which will return the system defined duration which can be changed in accessibility settings.
     * The duration must be in between {@link #MIN_LONG_PRESS_DURATION} and {@link #MAX_LONG_PRESS_DURATION},
     * otherwise {@link #FALLBACK_LONG_PRESS_DURATION} is used.
     */
    private int mLongPressTimeout;

    /**
     * Defines the duration in milliseconds for the delay between trigger of each repeat of
     * {@link #mRepetitiveKeys}. The default value is defined by {@link #DEFAULT_LONG_PRESS_REPEAT_DELAY}.
     * The duration must be in between {@link #MIN_LONG_PRESS__REPEAT_DELAY} and
     * {@link #MAX_LONG_PRESS__REPEAT_DELAY}, otherwise {@link #DEFAULT_LONG_PRESS_REPEAT_DELAY} is used.
     */
    private int mLongPressRepeatDelay;


    /** The popup window shown if {@link ExtraKeyButton#popup} returns a {@code non-null} value
     * and a swipe up action is done on an extra key. */
    private PopupWindow mPopupWindow;

    private ScheduledExecutorService mScheduledExecutor;
    private Handler mHandler;
    private SpecialButtonsLongHoldRunnable mSpecialButtonsLongHoldRunnable;
    private int mLongPressCount;


    public ExtraKeysView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setRepetitiveKeys(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS);
        setSpecialButtons(getDefaultSpecialButtons(this));

        setButtonColors(DEFAULT_BUTTON_TEXT_COLOR, DEFAULT_BUTTON_ACTIVE_TEXT_COLOR, DEFAULT_BUTTON_BACKGROUND_COLOR, DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR);

        setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
        setLongPressRepeatDelay(DEFAULT_LONG_PRESS_REPEAT_DELAY);
    }


    /** Set {@link #mExtraKeysViewClient}. */
    public void setExtraKeysViewClient(IExtraKeysView extraKeysViewClient) {
        mExtraKeysViewClient = extraKeysViewClient;
    }

    /** Set {@link #mRepetitiveKeys}. Must not be {@code null}. */
    public void setRepetitiveKeys(@NonNull List<String> repetitiveKeys) {
        mRepetitiveKeys = repetitiveKeys;
    }

    /** Set {@link #mSpecialButtonsKeys}. Must not be {@code null}. */
    public void setSpecialButtons(@NonNull Map<SpecialButton, SpecialButtonState> specialButtons) {
        mSpecialButtons = specialButtons;
        mSpecialButtonsKeys = this.mSpecialButtons.keySet().stream().map(SpecialButton::getKey).collect(Collectors.toSet());
    }


    /**
     * Set the {@link ExtraKeysView} button colors.
     *
     * @param buttonTextColor The value for {@link #mButtonTextColor}.
     * @param buttonActiveTextColor The value for {@link #mButtonActiveTextColor}.
     * @param buttonBackgroundColor The value for {@link #mButtonBackgroundColor}.
     * @param buttonActiveBackgroundColor The value for {@link #mButtonActiveBackgroundColor}.
     */
    public void setButtonColors(int buttonTextColor, int buttonActiveTextColor, int buttonBackgroundColor, int buttonActiveBackgroundColor) {
        mButtonTextColor = buttonTextColor;
        mButtonActiveTextColor = buttonActiveTextColor;
        mButtonBackgroundColor = buttonBackgroundColor;
        mButtonActiveBackgroundColor = buttonActiveBackgroundColor;
    }


    /** Get {@link #mButtonTextColor}. */
    public int getButtonTextColor() {
        return mButtonTextColor;
    }

    /** Get {@link #mButtonActiveTextColor}. */
    public int getButtonActiveTextColor() {
        return mButtonActiveTextColor;
    }

    /**
     * Set the extra keys button appearance. All dimensions are in pixels.
     *
     * @param borderColor The border color, ignored when {@code borderWidth <= 0}.
     * @param borderWidth The border width in pixels.
     * @param cornerRadius The corner radius in pixels (rounded buttons).
     * @param textSizeSp The text size in sp, or {@code <= 0} to keep the platform default.
     * @param typeface The typeface, or {@code null} to keep the platform default.
     * @param gap The gap between buttons in pixels.
     */
    public void setButtonAppearance(int borderColor, float borderWidth, float cornerRadius,
                                    float textSizeSp, @Nullable Typeface typeface, int gap) {
        mButtonBorderColor = borderColor;
        mButtonBorderWidth = Math.max(0f, borderWidth);
        mButtonCornerRadius = Math.max(0f, cornerRadius);
        mButtonTextSizeSp = textSizeSp;
        mButtonTypeface = typeface;
        mButtonGap = Math.max(0, gap);
        applyAppearanceToButtons();
    }

    /** Applies the current appearance values to every button already added to the view. */
    private void applyAppearanceToButtons() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof Button)
                applyButtonAppearance((Button) child);
        }
    }

    /** Applies text size, typeface, gap and idle background to a single button. */
    private void applyButtonAppearance(Button button) {
        if (mButtonTextSizeSp > 0)
            button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, mButtonTextSizeSp);
        if (mButtonTypeface != null)
            button.setTypeface(mButtonTypeface);
        if (button.getLayoutParams() instanceof GridLayout.LayoutParams)
            ((GridLayout.LayoutParams) button.getLayoutParams())
                    .setMargins(mButtonGap, mButtonGap, mButtonGap, mButtonGap);
        setButtonBackground(button, false);
    }

    /** Builds a rounded/bordered background drawable for the given state color. */
    private Drawable createButtonBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(mButtonCornerRadius);
        if (mButtonBorderWidth > 0f)
            drawable.setStroke(Math.max(1, Math.round(mButtonBorderWidth)), mButtonBorderColor);
        return drawable;
    }

    /** Switches a button between its idle and pressed backgrounds without losing rounding/border. */
    private void setButtonBackground(View view, boolean active) {
        view.setBackground(createButtonBackground(active ? mButtonActiveBackgroundColor : mButtonBackgroundColor));
    }

    /** Set {@link #mLongPressTimeout}. */
    public void setLongPressTimeout(int longPressDuration) {
        if (longPressDuration >= MIN_LONG_PRESS_DURATION && longPressDuration <= MAX_LONG_PRESS_DURATION) {
            mLongPressTimeout = longPressDuration;
        } else {
            mLongPressTimeout = FALLBACK_LONG_PRESS_DURATION;
        }
    }

    /** Set {@link #mLongPressRepeatDelay}. */
    public void setLongPressRepeatDelay(int longPressRepeatDelay) {
        if (mLongPressRepeatDelay >= MIN_LONG_PRESS__REPEAT_DELAY && mLongPressRepeatDelay <= MAX_LONG_PRESS__REPEAT_DELAY) {
            mLongPressRepeatDelay = longPressRepeatDelay;
        } else {
            mLongPressRepeatDelay = DEFAULT_LONG_PRESS_REPEAT_DELAY;
        }
    }

    /** Get the default map that can be used for {@link #mSpecialButtons}. */
    @NonNull
    public Map<SpecialButton, SpecialButtonState> getDefaultSpecialButtons(ExtraKeysView extraKeysView) {
        return new HashMap<>() {{
            put(SpecialButton.CTRL, new SpecialButtonState(extraKeysView));
            put(SpecialButton.ALT, new SpecialButtonState(extraKeysView));
            put(SpecialButton.SHIFT, new SpecialButtonState(extraKeysView));
            put(SpecialButton.META, new SpecialButtonState(extraKeysView));
            put(SpecialButton.FN, new SpecialButtonState(extraKeysView));
        }};
    }

    /**
     * Reload this instance of {@link ExtraKeysView} with the info passed in {@code extraKeysInfo}.
     */
    @SuppressLint("ClickableViewAccessibility")
    public void reload() {
        TermuxX11ExtraKeys.setExtraKeys();
        ExtraKeysInfo extraKeysInfo = TermuxX11ExtraKeys.getExtraKeysInfo();
        if (extraKeysInfo == null)
            return;

        for(SpecialButtonState state : mSpecialButtons.values())
            state.buttons = new ArrayList<>();

        removeAllViews();

        ExtraKeyButton[][] buttons = extraKeysInfo.getMatrix();

        setRowCount(buttons.length);
        setColumnCount(maximumLength(buttons));

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                final ExtraKeyButton buttonInfo = buttons[row][col];

                Button button;
                if (isSpecialButton(buttonInfo)) {
                    button = createSpecialButton(buttonInfo.key, true);
                    if (button == null) return;
                } else {
                    button = new Button(getContext(), null, android.R.attr.buttonBarButtonStyle);
                }

                button.setText(buttonInfo.display);
                button.setTextColor(mButtonTextColor);
                button.setAllCaps(true);
                button.setPadding(0, 0, 0, 0);

                button.setOnClickListener(view -> {
                    performExtraKeyButtonHapticFeedback(view, buttonInfo, button);
                    onAnyExtraKeyButtonClick(view, buttonInfo, button);
                });

                button.setOnTouchListener((view, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            setButtonBackground(view, true);
                            // Start long press scheduled executors which will be stopped in next MotionEvent
                            startScheduledExecutors(view, buttonInfo, button);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            if (buttonInfo.popup != null) {
                                // Show popup on swipe up
                                if (mPopupWindow == null && event.getY() < 0) {
                                    stopScheduledExecutors();
                                    setButtonBackground(view, false);
                                    showPopup(view, buttonInfo.popup);
                                }
                                if (mPopupWindow != null && event.getY() > 0) {
                                    setButtonBackground(view, true);
                                    dismissPopup();
                                }
                            }
                            return true;

                        case MotionEvent.ACTION_CANCEL:
                            setButtonBackground(view, false);
                            stopScheduledExecutors();
                            return true;

                        case MotionEvent.ACTION_UP:
                            setButtonBackground(view, false);
                            stopScheduledExecutors();
                            // If ACTION_UP up was not from a repetitive key or was with a key with a popup button
                            if (mLongPressCount == 0 || mPopupWindow != null) {
                                // Trigger popup button click if swipe up complete
                                if (mPopupWindow != null) {
                                    dismissPopup();
                                    if (buttonInfo.popup != null) {
                                        onAnyExtraKeyButtonClick(view, buttonInfo.popup, button);
                                    }
                                } else {
                                    view.performClick();
                                }
                            }
                            return true;

                        default:
                            return true;
                    }
                });

                LayoutParams param = new GridLayout.LayoutParams();
                param.width = 0;
                param.height = 0;
                param.setMargins(0, 0, 0, 0);
                param.columnSpec = GridLayout.spec(col, GridLayout.FILL, 1.f);
                param.rowSpec = GridLayout.spec(row, GridLayout.FILL, 1.f);
                button.setLayoutParams(param);
                applyButtonAppearance(button);

                addView(button);
            }
        }
    }

    public void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, Button button) {
        if (mExtraKeysViewClient != null)
            mExtraKeysViewClient.onExtraKeyButtonClick(view, buttonInfo, button);
    }

    public void performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, Button button) {
        if (mExtraKeysViewClient != null) {
            // If client handled the feedback, then just return
            if (mExtraKeysViewClient.performExtraKeyButtonHapticFeedback(view, buttonInfo, button))
                return;
        }

        if (Settings.System.getInt(getContext().getContentResolver(),
            Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0) {

            if (Build.VERSION.SDK_INT >= 28) {
                button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } else {
                // Perform haptic feedback only if no total silence mode enabled.
                if (Settings.Global.getInt(getContext().getContentResolver(), "zen_mode", 0) != 2) {
                    button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
            }
        }
    }

    public void onAnyExtraKeyButtonClick(View view, @NonNull ExtraKeyButton buttonInfo, Button button) {
        if (isSpecialButton(buttonInfo)) {
            if (mLongPressCount > 0) return;
            SpecialButtonState state = mSpecialButtons.get(SpecialButton.valueOf(buttonInfo.key));
            if (state == null) return;

            // Toggle active state and disable lock state if new state is not active
            state.setIsActive(!state.isActive);
            if (!state.isActive)
                state.setIsLocked(false);
        } else {
            onExtraKeyButtonClick(view, buttonInfo, button);
        }
    }

    public void startScheduledExecutors(View view, ExtraKeyButton buttonInfo, Button button) {
        stopScheduledExecutors();
        mLongPressCount = 0;
        if (mRepetitiveKeys.contains(buttonInfo.key)) {
            // Auto repeat key if long pressed until ACTION_UP stops it by calling stopScheduledExecutors.
            // Currently, only one (last) repeat key can run at a time. Old ones are stopped.
            mScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            mScheduledExecutor.scheduleWithFixedDelay(() -> {
                mLongPressCount++;
                onExtraKeyButtonClick(view, buttonInfo, button);
            }, mLongPressTimeout, mLongPressRepeatDelay, TimeUnit.MILLISECONDS);
        } else if (isSpecialButton(buttonInfo)) {
            // Lock the key if long pressed by running mSpecialButtonsLongHoldRunnable after
            // waiting for mLongPressTimeout milliseconds. If user does not long press, then the
            // ACTION_UP triggered will cancel the runnable by calling stopScheduledExecutors before
            // it has a chance to run.
            SpecialButtonState state = mSpecialButtons.get(SpecialButton.valueOf(buttonInfo.key));
            if (state == null) return;
            if (mHandler == null)
                mHandler = new Handler(Looper.getMainLooper());
            mSpecialButtonsLongHoldRunnable = new SpecialButtonsLongHoldRunnable(state);
            mHandler.postDelayed(mSpecialButtonsLongHoldRunnable, mLongPressTimeout);
        }
    }

    public void stopScheduledExecutors() {
        if (mScheduledExecutor != null) {
            mScheduledExecutor.shutdownNow();
            mScheduledExecutor = null;
        }

        if (mSpecialButtonsLongHoldRunnable != null && mHandler != null) {
            mHandler.removeCallbacks(mSpecialButtonsLongHoldRunnable);
            mSpecialButtonsLongHoldRunnable = null;
        }
    }

    public class SpecialButtonsLongHoldRunnable implements Runnable {
        public final SpecialButtonState mState;

        public SpecialButtonsLongHoldRunnable(SpecialButtonState state) {
            mState = state;
        }

        public void run() {
            // Toggle active and lock state
            mState.setIsLocked(!mState.isActive);
            mState.setIsActive(!mState.isActive);
            mLongPressCount++;
        }
    }

    void showPopup(View view, ExtraKeyButton extraButton) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        Button button;
        if (isSpecialButton(extraButton)) {
            button = createSpecialButton(extraButton.key, false);
            if (button == null) return;
        } else {
            button = new Button(getContext(), null, android.R.attr.buttonBarButtonStyle);
            button.setTextColor(mButtonTextColor);
        }
        button.setText(extraButton.display);
        button.setAllCaps(true);
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setWidth(width);
        button.setHeight(height);
        if (mButtonTextSizeSp > 0)
            button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, mButtonTextSizeSp);
        if (mButtonTypeface != null)
            button.setTypeface(mButtonTypeface);
        button.setBackground(createButtonBackground(mButtonActiveBackgroundColor));
        mPopupWindow = new PopupWindow(this);
        mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
        mPopupWindow.setContentView(button);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(false);
        mPopupWindow.showAsDropDown(view, 0, -2 * height);
    }

    public void dismissPopup() {
        mPopupWindow.setContentView(null);
        mPopupWindow.dismiss();
        mPopupWindow = null;
    }

    /** Check whether a {@link ExtraKeyButton} is a {@link SpecialButton}. */
    public boolean isSpecialButton(ExtraKeyButton button) {
        return mSpecialButtonsKeys.contains(button.key);
    }

    /**
     * Read whether {@link SpecialButton} registered in {@link #mSpecialButtons} is active or not.
     *
     * @param specialButton The {@link SpecialButton} to read.
     * @param autoSetInActive Set to {@code true} if {@link SpecialButtonState#isActive} should be
     *                        set {@code false} if button is not locked.
     * @return Returns {@code null} if button does not exist in {@link #mSpecialButtons}. If button
     *         exists, then returns {@code true} if the button is created in {@link ExtraKeysView}
     *         and is active, otherwise {@code false}.
     */
    @Nullable
    public Boolean readSpecialButton(SpecialButton specialButton, boolean autoSetInActive) {
        SpecialButtonState state = mSpecialButtons.get(specialButton);
        if (state == null) return null;

        if (!state.isCreated || !state.isActive)
            return false;

        // Disable active state only if not locked
        if (autoSetInActive && !state.isLocked)
            state.setIsActive(false);

        return true;
    }

    public Button createSpecialButton(String buttonKey, boolean needUpdate) {
        SpecialButtonState state = mSpecialButtons.get(SpecialButton.valueOf(buttonKey));
        if (state == null) return null;
        state.setIsCreated(true);
        Button button = new Button(getContext(), null, android.R.attr.buttonBarButtonStyle);
        button.setTextColor(state.isActive ? mButtonActiveTextColor : mButtonTextColor);
        if (needUpdate) {
            state.buttons.add(button);
        }
        return button;
    }

    /**
     * General util function to compute the longest column length in a matrix.
     */
    public static int maximumLength(Object[][] matrix) {
        int m = 0;
        for (Object[] row : matrix)
            m = Math.max(m, row.length);
        return m;
    }
}
