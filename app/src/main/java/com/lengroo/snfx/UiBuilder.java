package com.lengroo.snfx;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 主界面搭建：buildUi() 与小组件 helper，从 MainActivity 拆出（第 1 步）。 */
public final class UiBuilder {

    private UiBuilder() {
    }

    /** buildUi() 创建的视图，由 MainActivity 接管到自己的字段上。 */
    public static final class Views {
        public TextView counter;
        public TextView body;
        public TextView trans;
        public TextView source;
        public ScrollView scroll;
        public Button modeBtn;
        public Button todayBtn;
        public Button favBtn;
        public Button indexBtn;
        public Button speakBtn;
        public Button typeBtn;
        public Button toggle;
    }

    private static float dp(float v) {
        return ThemeKit.dp(v);
    }

    private static GradientDrawable round(int color, float radius) {
        return ThemeKit.round(color, radius);
    }

    private static GradientDrawable outline(int fill, int stroke, float radius) {
        return ThemeKit.outline(fill, stroke, radius);
    }

    private static Drawable ripple(Drawable content, int color) {
        return ThemeKit.ripple(content, color);
    }

    public static Views buildUi(final MainActivity a) {
        Views views = new Views();
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeKit.BG);
        root.setFitsSystemWindows(true);

        LinearLayout headerRow = new LinearLayout(a);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding((int) dp(20), (int) dp(18), (int) dp(14), 0);

        TextView title = new TextView(a);
        title.setText("每日一遍");
        title.setTextColor(ThemeKit.TEXT);
        title.setTextSize(22);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            title.setAutoSizeTextTypeUniformWithConfiguration(15, 22, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
        }
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        LinearLayout titleRow = new LinearLayout(a);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(title);

        views.modeBtn = new Button(a);
        views.modeBtn.setAllCaps(false);
        views.modeBtn.setTextSize(11);
        views.modeBtn.setTextColor(ThemeKit.ACCENT);
        views.modeBtn.setPadding((int) dp(8), 0, (int) dp(8), 0);
        views.modeBtn.setMinWidth(0);
        views.modeBtn.setMinimumWidth(0);
        views.modeBtn.setMinHeight(0);
        views.modeBtn.setMinimumHeight(0);
        views.modeBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        views.modeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.toggleRandomMode();
            }
        });
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        mlp.leftMargin = (int) dp(8);
        titleRow.addView(views.modeBtn, mlp);

        views.todayBtn = new Button(a);
        views.todayBtn.setText("今日");
        views.todayBtn.setAllCaps(false);
        views.todayBtn.setTextSize(11);
        views.todayBtn.setTextColor(ThemeKit.SUB);
        views.todayBtn.setPadding((int) dp(8), 0, (int) dp(8), 0);
        views.todayBtn.setMinWidth(0);
        views.todayBtn.setMinimumWidth(0);
        views.todayBtn.setMinHeight(0);
        views.todayBtn.setMinimumHeight(0);
        views.todayBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        views.todayBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showToday();
            }
        });
        LinearLayout.LayoutParams tdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        tdLp.leftMargin = (int) dp(6);
        titleRow.addView(views.todayBtn, tdLp);

        Button reviewBtn = new Button(a);
        reviewBtn.setText("回顾");
        reviewBtn.setAllCaps(false);
        reviewBtn.setTextSize(11);
        reviewBtn.setTextColor(ThemeKit.SUB);
        reviewBtn.setPadding((int) dp(8), 0, (int) dp(8), 0);
        reviewBtn.setMinWidth(0);
        reviewBtn.setMinimumWidth(0);
        reviewBtn.setMinHeight(0);
        reviewBtn.setMinimumHeight(0);
        reviewBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        reviewBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showReview();
            }
        });
        LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        rvLp.leftMargin = (int) dp(6);
        titleRow.addView(reviewBtn, rvLp);

        LinearLayout topButtons = new LinearLayout(a);
        topButtons.setOrientation(LinearLayout.HORIZONTAL);
        topButtons.setGravity(Gravity.CENTER_VERTICAL);

        Button settings = new Button(a);
        settings.setText("设置");
        settings.setAllCaps(false);
        settings.setTextSize(10);
        settings.setTextColor(ThemeKit.SUB);
        settings.setPadding((int) dp(9), 0, (int) dp(9), 0);
        settings.setMinWidth(0);
        settings.setMinimumWidth(0);
        settings.setMinHeight(0);
        settings.setMinimumHeight(0);
        settings.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showSettings();
            }
        });
        topButtons.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28)));

        Button about = new Button(a);
        about.setText("作者");
        about.setAllCaps(false);
        about.setTextSize(10);
        about.setTextColor(ThemeKit.SUB);
        about.setPadding((int) dp(9), 0, (int) dp(9), 0);
        about.setMinWidth(0);
        about.setMinimumWidth(0);
        about.setMinHeight(0);
        about.setMinimumHeight(0);
        about.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        about.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showAbout();
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        alp.leftMargin = (int) dp(6);
        topButtons.addView(about, alp);

        Button importer = new Button(a);
        importer.setText("导入");
        importer.setAllCaps(false);
        importer.setTextSize(10);
        importer.setTextColor(ThemeKit.SUB);
        importer.setPadding((int) dp(9), 0, (int) dp(9), 0);
        importer.setMinWidth(0);
        importer.setMinimumWidth(0);
        importer.setMinHeight(0);
        importer.setMinimumHeight(0);
        importer.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        importer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ImportManager.showImportHelp(a);
            }
        });
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        ilp.leftMargin = (int) dp(6);
        topButtons.addView(importer, ilp);

        headerRow.addView(titleRow, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(topButtons);
        root.addView(headerRow);

        LinearLayout meta = new LinearLayout(a);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding((int) dp(20), (int) dp(8), 0, 0);

        View dot = new View(a);
        dot.setBackground(round(ThemeKit.ACCENT, dp(3)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams((int) dp(6), (int) dp(6));
        dlp.rightMargin = (int) dp(8);
        meta.addView(dot, dlp);

        views.counter = new TextView(a);
        views.counter.setTextColor(ThemeKit.SUB);
        views.counter.setTextSize(11);
        views.counter.setSingleLine(true);
        views.counter.setEllipsize(android.text.TextUtils.TruncateAt.END);
        meta.addView(views.counter);
        root.addView(meta);

        views.source = new TextView(a);
        views.source.setTextColor(ThemeKit.SUB);
        views.source.setTextSize(11);
        views.source.setPadding((int) dp(20), (int) dp(6), 0, 0);
        root.addView(views.source);

        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(ThemeKit.CARD, dp(18)));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        clp.leftMargin = (int) dp(16);
        clp.rightMargin = (int) dp(16);
        clp.bottomMargin = (int) dp(10);

        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        sv.setHorizontalScrollBarEnabled(false);
        views.scroll = sv;
        views.body = new TextView(a);
        views.body.setTextColor(ThemeKit.TEXT);
        views.body.setTextSize(16);
        views.body.setLineSpacing(dp(9), 1f);
        views.body.setPadding((int) dp(20), (int) dp(20), (int) dp(20), (int) dp(6));
        views.body.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        views.body.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                a.showActions();
                return true;
            }
        });
        LinearLayout textCol = new LinearLayout(a);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = new TextView(a);
        mark.setText("\u201c");
        mark.setTextColor(0x1AFFFFFF);
        mark.setTextSize(48);
        mark.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        mark.setPadding((int) dp(20), (int) dp(16), (int) dp(20), 0);
        textCol.addView(mark, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textCol.addView(views.body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        views.trans = new TextView(a);
        views.trans.setTextColor(0xFF8A8A94);
        views.trans.setTextSize(13);
        views.trans.setLineSpacing(dp(6), 1f);
        views.trans.setPadding((int) dp(20), 0, (int) dp(20), (int) dp(20));
        views.trans.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        textCol.addView(views.trans, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.addView(textCol, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout footer = new LinearLayout(a);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 0, 0, (int) dp(10));
        views.favBtn = new Button(a);
        views.favBtn.setText("收藏");
        views.favBtn.setAllCaps(false);
        views.favBtn.setTextSize(11);
        views.favBtn.setTextColor(0xFF8A8A94);
        views.favBtn.setPadding((int) dp(16), 0, (int) dp(16), 0);
        views.favBtn.setMinWidth(0);
        views.favBtn.setMinimumWidth(0);
        views.favBtn.setMinHeight(0);
        views.favBtn.setMinimumHeight(0);
        views.favBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        views.favBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showFavs();
            }
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        flp.rightMargin = (int) dp(8);
        footer.addView(views.favBtn, flp);

        views.indexBtn = new Button(a);
        views.indexBtn.setAllCaps(false);
        views.indexBtn.setTextSize(11);
        views.indexBtn.setTextColor(ThemeKit.ACCENT);
        views.indexBtn.setPadding((int) dp(16), 0, (int) dp(16), 0);
        views.indexBtn.setMinWidth(0);
        views.indexBtn.setMinimumWidth(0);
        views.indexBtn.setMinHeight(0);
        views.indexBtn.setMinimumHeight(0);
        views.indexBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        views.indexBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showIndex();
            }
        });
        footer.addView(views.indexBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28)));

        views.speakBtn = new Button(a);
        views.speakBtn.setText("朗读");
        views.speakBtn.setAllCaps(false);
        views.speakBtn.setTextSize(11);
        views.speakBtn.setTextColor(0xFF8A8A94);
        views.speakBtn.setPadding((int) dp(16), 0, (int) dp(16), 0);
        views.speakBtn.setMinWidth(0);
        views.speakBtn.setMinimumWidth(0);
        views.speakBtn.setMinHeight(0);
        views.speakBtn.setMinimumHeight(0);
        views.speakBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        views.speakBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.ttsCtl().toggleSpeak(a);
            }
        });
        views.speakBtn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                a.ttsCtl().showSpeakLang(a);
                return true;
            }
        });
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        slp2.leftMargin = (int) dp(8);
        footer.addView(views.speakBtn, slp2);
        views.typeBtn = new Button(a);
        views.typeBtn.setText("Aa");
        views.typeBtn.setAllCaps(false);
        views.typeBtn.setTextSize(12);
        views.typeBtn.setTextColor(0xFF8A8A94);
        views.typeBtn.setPadding((int) dp(14), 0, (int) dp(14), 0);
        views.typeBtn.setMinWidth(0);
        views.typeBtn.setMinimumWidth(0);
        views.typeBtn.setMinHeight(0);
        views.typeBtn.setMinimumHeight(0);
        views.typeBtn.setBackground(ripple(outline(0x00000000, ThemeKit.LINE, dp(14)), ThemeKit.RIPPLE));
        views.typeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.ttsCtl().showTypography(a);
            }
        });
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        tlp2.leftMargin = (int) dp(8);
        footer.addView(views.typeBtn, tlp2);

        card.addView(footer);

        root.addView(card, clp);

        Button btn = new Button(a);
        btn.setText("换一篇");
        btn.setAllCaps(false);
        btn.setTextSize(16);
        btn.setTextColor(0xFF12060A);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setBackground(ripple(round(ThemeKit.ACCENT, dp(16)), 0x33FFFFFF));
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.showNext();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(52));
        blp.leftMargin = (int) dp(16);
        blp.rightMargin = (int) dp(16);
        blp.bottomMargin = (int) dp(8);
        root.addView(btn, blp);

        views.toggle = new Button(a);
        views.toggle.setAllCaps(false);
        views.toggle.setTextSize(13);
        views.toggle.setTextColor(ThemeKit.ACCENT);
        views.toggle.setBackground(ripple(round(0x00000000, dp(16)), ThemeKit.RIPPLE));
        views.toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.toggleMode();
            }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(40));
        tlp.leftMargin = (int) dp(16);
        tlp.rightMargin = (int) dp(16);
        tlp.bottomMargin = (int) dp(20);
        root.addView(views.toggle, tlp);

        a.setContentView(root);
        return views;
    }

    public static TextView labelRow(final MainActivity a, String text) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextColor(ThemeKit.SUB);
        t.setTextSize(12);
        t.setPadding(0, (int) dp(4), 0, (int) dp(8));
        return t;
    }

    public static LinearLayout.LayoutParams chipParams(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, (int) dp(34), 1f);
        lp.leftMargin = (int) dp(leftMargin);
        return lp;
    }

    public static TextView chip(final MainActivity a, String text, boolean on, View.OnClickListener l) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(on ? ThemeKit.ACCENT : ThemeKit.TEXT);
        t.setBackground(outline(on ? 0x22FF5A7A : 0x00000000, on ? ThemeKit.ACCENT : ThemeKit.LINE, dp(12)));
        t.setOnClickListener(l);
        return t;
    }
}
