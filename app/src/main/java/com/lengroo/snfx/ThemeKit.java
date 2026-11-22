package com.lengroo.snfx;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;

/** 配色与绘制工具：一套深浅两色，所有界面元素都从这里取值。 */
public final class ThemeKit {

    /** 主题模式：跟随系统 / 深色 / 浅色。 */
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_LIGHT = 2;

    public static int BG, CARD, TEXT, SUB, ACCENT, LINE, FIELD, HINT, RIPPLE;

    private static float density = 3f;

    private ThemeKit() {
    }

    public static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void configure(Context c) {
        configure(c, THEME_SYSTEM);
    }

    public static void configure(Context c, int mode) {
        density = c.getResources().getDisplayMetrics().density;
        boolean night = mode == THEME_DARK ? true
                : mode == THEME_LIGHT ? false : isNight(c);
        if (night) {
            BG = 0xFF0B0B0D;
            CARD = 0xFF15151A;
            TEXT = 0xFFEDEAE6;
            SUB = 0xFF6E6E78;
            ACCENT = 0xFFFF5A7A;
            LINE = 0xFF3A3A42;
            RIPPLE = 0x22FFFFFF;
            FIELD = 0xFF1F1F26;
            HINT = 0xFF8A8A94;
        } else {
            BG = 0xFFF4F4F7;
            CARD = 0xFFFFFFFF;
            TEXT = 0xFF17171B;
            SUB = 0xFF6B6B76;
            ACCENT = 0xFFD84069;
            LINE = 0xFFE3E3EA;
            RIPPLE = 0x22000000;
            FIELD = 0xFFEFEFF4;
            HINT = 0xFF7A7A86;
        }
    }

    public static float dp(float v) {
        return v * density;
    }

    public static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    public static GradientDrawable outline(int fill, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke((int) Math.max(1f, dp(1f)), stroke);
        return g;
    }

    public static Drawable ripple(Drawable content, int color) {
        return new RippleDrawable(ColorStateList.valueOf(color), content, null);
    }

    public static void styleDialog(AlertDialog d) {
        try {
            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new InsetDrawable(
                        round(CARD, dp(18)), (int) dp(6)));
            }
            Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok != null) ok.setTextColor(ACCENT);
            Button no = d.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (no != null) no.setTextColor(SUB);
            Button mid = d.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (mid != null) mid.setTextColor(ACCENT);
        } catch (Throwable ignored) {
        }
    }
}
