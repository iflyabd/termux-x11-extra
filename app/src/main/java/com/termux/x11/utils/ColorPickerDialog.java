package com.termux.x11.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Simple self-contained ARGB color picker dialog: saturation/value square, hue strip,
 * alpha strip, checkerboard preview and a hex input field.
 *
 * No external dependencies are used, so it can be shown for any preference that stores a
 * {@code #AARRGGBB} string.
 */
public final class ColorPickerDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private ColorPickerDialog() { }

    /** Shows the picker and reports the chosen color through {@code listener}. */
    public static AlertDialog show(@NonNull Context context, @Nullable CharSequence title,
                                   int initialColor, @NonNull OnColorSelectedListener listener) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(dp(context, 20));
        root.setPadding(pad, Math.round(dp(context, 8)), pad, 0);
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        PreviewView preview = new PreviewView(context);
        root.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(dp(context, 36))));

        ColorPickerView picker = new ColorPickerView(context);
        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(dp(context, 250)));
        pickerParams.topMargin = Math.round(dp(context, 12));
        root.addView(picker, pickerParams);

        EditText hex = new EditText(context);
        hex.setSingleLine(true);
        hex.setTypeface(Typeface.MONOSPACE);
        hex.setHint("Hex color #AARRGGBB");
        hex.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        hex.setText(formatColor(initialColor));
        hex.setSelection(hex.getText().length());
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexParams.topMargin = Math.round(dp(context, 8));
        root.addView(hex, hexParams);

        final boolean[] syncing = {true};
        picker.setOnColorChangedListener(color -> {
            if (syncing[0])
                return;
            syncing[0] = true;
            String text = formatColor(color);
            hex.setText(text);
            hex.setSelection(text.length());
            preview.setColor(color);
            syncing[0] = false;
        });

        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (syncing[0])
                    return;
                Integer parsed = parseColor(s.toString(), picker.getColorAlpha());
                if (parsed == null)
                    return;
                syncing[0] = true;
                picker.setColor(parsed);
                preview.setColor(parsed);
                syncing[0] = false;
            }
        });

        picker.setColor(initialColor);
        preview.setColor(initialColor);
        syncing[0] = false;

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title != null ? title : "Pick a color")
                .setView(root)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    Integer parsed = parseColor(hex.getText().toString(), picker.getColorAlpha());
                    listener.onColorSelected(parsed != null ? parsed : picker.getColor());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton("Reset", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                syncing[0] = true;
                picker.setColor(initialColor);
                hex.setText(formatColor(initialColor));
                hex.setSelection(hex.getText().length());
                preview.setColor(initialColor);
                syncing[0] = false;
            });
        });

        if (dialog.getWindow() != null)
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        return dialog;
    }

    public static String formatColor(int color) {
        return String.format(Locale.US, "#%08X", color);
    }

    /**
     * Parses {@code #AARRGGBB}, {@code #RRGGBB} (alpha filled from {@code fallbackAlpha})
     * or the same values without a leading {@code #}.
     */
    @Nullable
    public static Integer parseColor(@Nullable String input, int fallbackAlpha) {
        if (input == null)
            return null;
        String text = input.trim();
        if (text.startsWith("#"))
            text = text.substring(1);
        if (text.length() == 6)
            text = String.format(Locale.US, "%02X", fallbackAlpha & 0xFF) + text;
        if (text.length() != 8)
            return null;
        try {
            long value = Long.parseLong(text, 16);
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    /** Draws a checkerboard, used behind transparent colors. */
    static void drawCheckerboard(Canvas canvas, RectF rect, float cell, Paint paint) {
        int rows = (int) Math.ceil(rect.height() / cell);
        int cols = (int) Math.ceil(rect.width() / cell);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float left = rect.left + col * cell;
                float top = rect.top + row * cell;
                float right = Math.min(left + cell, rect.right);
                float bottom = Math.min(top + cell, rect.bottom);
                paint.setColor(((row + col) & 1) == 0 ? 0xFFFFFFFF : 0xFFCCCCCC);
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Preview swatch that shows the alpha over a checkerboard. */
    static final class PreviewView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();
        private int color = Color.TRANSPARENT;

        PreviewView(Context context) {
            super(context);
        }

        void setColor(int color) {
            this.color = color;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float inset = dp(1);
            float radius = dp(8);
            rect.set(inset, inset, getWidth() - inset, getHeight() - inset);

            path.reset();
            path.addRoundRect(rect, radius, radius, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(path);
            drawCheckerboard(canvas, rect, dp(8), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRect(rect, paint);
            canvas.restore();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0x66FFFFFF);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }

    /** Saturation/value square + hue strip + alpha strip. */
    static final class ColorPickerView extends View {
        interface OnColorChangedListener {
            void onColorChanged(int color);
        }

        private static final int REGION_NONE = 0;
        private static final int REGION_SV = 1;
        private static final int REGION_HUE = 2;
        private static final int REGION_ALPHA = 3;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF svRect = new RectF();
        private final RectF hueRect = new RectF();
        private final RectF alphaRect = new RectF();
        private final float[] hsv = {0f, 1f, 1f};
        private int alpha = 255;
        private int activeRegion = REGION_NONE;

        @Nullable
        private OnColorChangedListener listener;

        ColorPickerView(Context context) {
            super(context);
            setFocusable(true);
        }

        void setOnColorChangedListener(@Nullable OnColorChangedListener listener) {
            this.listener = listener;
        }

        void setColor(int color) {
            Color.colorToHSV(color, hsv);
            alpha = Color.alpha(color);
            invalidate();
        }

        int getColorAlpha() {
            return alpha;
        }

        int getColor() {
            return Color.HSVToColor(alpha, hsv);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float pad = dp(2);
            float strip = dp(22);
            float gap = dp(14);
            float width = w - pad * 2;
            float svHeight = Math.max(dp(60),
                    h - pad * 2 - strip * 2 - gap * 2);
            svRect.set(pad, pad, pad + width, pad + svHeight);
            hueRect.set(pad, svRect.bottom + gap, pad + width, svRect.bottom + gap + strip);
            alphaRect.set(pad, hueRect.bottom + gap, pad + width, hueRect.bottom + gap + strip);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = dp(8);

            // Saturation / value square.
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(svRect.left, svRect.top, svRect.right, svRect.top,
                    Color.WHITE, Color.HSVToColor(new float[]{hsv[0], 1f, 1f}),
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(svRect, radius, radius, paint);
            paint.setShader(new LinearGradient(svRect.left, svRect.top, svRect.left, svRect.bottom,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(svRect, radius, radius, paint);
            paint.setShader(null);

            // Hue strip.
            int[] hueColors = {
                    0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
                    0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
            };
            paint.setShader(new LinearGradient(hueRect.left, hueRect.top, hueRect.right, hueRect.top,
                    hueColors, null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(hueRect, hueRect.height() / 2f, hueRect.height() / 2f, paint);
            paint.setShader(null);

            // Alpha strip: checkerboard below, transparent -> opaque gradient above.
            int rgb = Color.HSVToColor(new float[]{hsv[0], hsv[1], hsv[2]}) & 0x00FFFFFF;
            canvas.save();
            Path alphaPath = new Path();
            alphaPath.addRoundRect(alphaRect, alphaRect.height() / 2f, alphaRect.height() / 2f,
                    Path.Direction.CW);
            canvas.clipPath(alphaPath);
            drawCheckerboard(canvas, alphaRect, dp(8), paint);
            paint.setShader(new LinearGradient(alphaRect.left, alphaRect.top, alphaRect.right,
                    alphaRect.top, rgb, rgb | 0xFF000000, Shader.TileMode.CLAMP));
            canvas.drawRect(alphaRect, paint);
            paint.setShader(null);
            canvas.restore();

            // SV cursor.
            float sx = svRect.left + hsv[1] * svRect.width();
            float sy = svRect.top + (1f - hsv[2]) * svRect.height();
            drawThumb(canvas, sx, sy, dp(7));

            // Hue cursor.
            float hx = hueRect.left + (hsv[0] / 360f) * hueRect.width();
            drawThumb(canvas, hx, hueRect.centerY(), dp(9));

            // Alpha cursor.
            float ax = alphaRect.left + (alpha / 255f) * alphaRect.width();
            drawThumb(canvas, ax, alphaRect.centerY(), dp(9));
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }

        private void drawThumb(Canvas canvas, float x, float y, float radius) {
            thumbPaint.setStyle(Paint.Style.FILL);
            thumbPaint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, radius, thumbPaint);
            thumbPaint.setStyle(Paint.Style.STROKE);
            thumbPaint.setStrokeWidth(dp(1.5f));
            thumbPaint.setColor(0xCC000000);
            canvas.drawCircle(x, y, radius, thumbPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    activeRegion = regionAt(event.getX(), event.getY());
                    if (activeRegion == REGION_NONE)
                        return false;
                    if (getParent() != null)
                        getParent().requestDisallowInterceptTouchEvent(true);
                    updateFromTouch(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (activeRegion != REGION_NONE)
                        updateFromTouch(event.getX(), event.getY());
                    return activeRegion != REGION_NONE;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeRegion = REGION_NONE;
                    if (getParent() != null)
                        getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        private int regionAt(float x, float y) {
            if (contains(svRect, x, y))
                return REGION_SV;
            if (containsExpanded(hueRect, x, y))
                return REGION_HUE;
            if (containsExpanded(alphaRect, x, y))
                return REGION_ALPHA;
            return REGION_NONE;
        }

        private boolean contains(RectF rect, float x, float y) {
            return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
        }

        private boolean containsExpanded(RectF rect, float x, float y) {
            float extra = dp(10);
            return x >= rect.left && x <= rect.right
                    && y >= rect.top - extra && y <= rect.bottom + extra;
        }

        private void updateFromTouch(float touchX, float touchY) {
            if (activeRegion == REGION_SV) {
                float x = clamp(touchX, svRect.left, svRect.right);
                float y = clamp(touchY, svRect.top, svRect.bottom);
                hsv[1] = (x - svRect.left) / svRect.width();
                hsv[2] = 1f - (y - svRect.top) / svRect.height();
            } else if (activeRegion == REGION_HUE) {
                float x = clamp(touchX, hueRect.left, hueRect.right);
                hsv[0] = ((x - hueRect.left) / hueRect.width()) * 359.99f;
            } else if (activeRegion == REGION_ALPHA) {
                float x = clamp(touchX, alphaRect.left, alphaRect.right);
                alpha = Math.round(((x - alphaRect.left) / alphaRect.width()) * 255f);
            }
            invalidate();
            if (listener != null)
                listener.onColorChanged(getColor());
        }
    }
}
