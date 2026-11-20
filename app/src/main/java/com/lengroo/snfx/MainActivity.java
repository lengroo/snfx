package com.lengroo.snfx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String SEP = "\n@@@\n";
    private static final String AUTHOR = "lengroo错觉与风季一起吃薯条并看着李空壳打CF";
    private static final int BG = 0xFF0B0B0D;
    private static final int CARD = 0xFF15151A;
    private static final int TEXT = 0xFFEDEAE6;
    private static final int SUB = 0xFF6E6E78;
    private static final int ACCENT = 0xFFFF5A7A;
    private static final int LINE = 0xFF3A3A42;

    private String[] own = new String[0];
    private String[] hot = new String[0];
    private int mode = 0;              // 0 原创 / 1 抖音热门
    private boolean randomMode = false; // false 默认（不重复） / true 纯随机
    private int lastOwn = -1, lastHot = -1;
    private int countOwn = 0, countHot = 0;
    private final Set<Integer> seenOwn = new HashSet<Integer>();
    private final Set<Integer> seenHot = new HashSet<Integer>();

    private TextView counter;
    private TextView body;
    private TextView source;
    private ScrollView scroll;
    private Button toggle;
    private Button modeBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
        } catch (Throwable ignored) {
        }
        prefs = getSharedPreferences("dby", MODE_PRIVATE);
        mode = prefs.getInt("mode", 0);
        randomMode = prefs.getBoolean("random_mode", false);
        countOwn = prefs.getInt("count_own", 0);
        countHot = prefs.getInt("count_hot", 0);
        readSeen("seen_own", seenOwn);
        readSeen("seen_hot", seenHot);

        own = load("quotes.txt");
        hot = load("hot.txt");
        buildUi();
        showNext();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private String[] load(String name) {
        InputStream in = null;
        try {
            in = getAssets().open(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            String all = new String(bos.toByteArray(), "UTF-8").trim();
            if (all.length() > 0) return all.split(SEP);
        } catch (Throwable t) {
            return new String[0];
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
        }
        return new String[0];
    }

    private void readSeen(String key, Set<Integer> into) {
        into.clear();
        String s = prefs.getString(key, "");
        if (s.length() == 0) return;
        String[] parts = s.split(",");
        for (int i = 0; i < parts.length; i++) {
            try {
                into.add(Integer.parseInt(parts[i].trim()));
            } catch (Throwable ignored) {
            }
        }
    }

    private String writeSeen(Set<Integer> set) {
        StringBuilder sb = new StringBuilder();
        for (Integer v : set) {
            if (sb.length() > 0) sb.append(',');
            sb.append(v.intValue());
        }
        return sb.toString();
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private GradientDrawable outline(int fill, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke((int) Math.max(1f, dp(1f)), stroke);
        return g;
    }

    // Gives a button press feedback: a Material ripple over the same shape.
    // A bare GradientDrawable has no pressed state, so buttons felt dead.
    private Drawable ripple(Drawable content, int color) {
        return new RippleDrawable(ColorStateList.valueOf(color), content, null);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.TOP);
        headerRow.setPadding((int) dp(22), (int) dp(24), (int) dp(16), (int) dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("每日一遍");
        title.setTextColor(TEXT);
        title.setTextSize(26);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(title);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(0, (int) dp(8), 0, 0);

        View dot = new View(this);
        dot.setBackground(round(ACCENT, dp(3)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams((int) dp(6), (int) dp(6));
        dlp.rightMargin = (int) dp(8);
        meta.addView(dot, dlp);

        counter = new TextView(this);
        counter.setTextColor(SUB);
        counter.setTextSize(12);
        meta.addView(counter);
        header.addView(meta);

        source = new TextView(this);
        source.setTextColor(SUB);
        source.setTextSize(12);
        source.setPadding(0, (int) dp(6), 0, 0);
        header.addView(source);

        headerRow.addView(header, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // 上方：模式切换 + 作者
        LinearLayout topButtons = new LinearLayout(this);
        topButtons.setOrientation(LinearLayout.HORIZONTAL);
        topButtons.setGravity(Gravity.TOP);

        modeBtn = new Button(this);
        modeBtn.setAllCaps(false);
        modeBtn.setTextSize(12);
        modeBtn.setTextColor(ACCENT);
        modeBtn.setPadding((int) dp(10), 0, (int) dp(10), 0);
        modeBtn.setMinWidth(0);
        modeBtn.setMinimumWidth(0);
        modeBtn.setMinHeight(0);
        modeBtn.setMinimumHeight(0);
        modeBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), 0x22FFFFFF));
        modeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                randomMode = !randomMode;
                prefs.edit().putBoolean("random_mode", randomMode).apply();
                refreshLabels();
                showNext();
            }
        });
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(32));
        topButtons.addView(modeBtn, mlp);

        Button about = new Button(this);
        about.setText("作者");
        about.setAllCaps(false);
        about.setTextSize(12);
        about.setTextColor(SUB);
        about.setPadding((int) dp(12), 0, (int) dp(12), 0);
        about.setMinWidth(0);
        about.setMinimumWidth(0);
        about.setMinHeight(0);
        about.setMinimumHeight(0);
        about.setBackground(ripple(outline(0x00000000, LINE, dp(14)), 0x22FFFFFF));
        about.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAbout();
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(32));
        alp.leftMargin = (int) dp(8);
        topButtons.addView(about, alp);

        headerRow.addView(topButtons);
        root.addView(headerRow);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(CARD, dp(18)));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        clp.leftMargin = (int) dp(16);
        clp.rightMargin = (int) dp(16);
        clp.bottomMargin = (int) dp(10);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        scroll = sv;
        body = new TextView(this);
        body.setTextColor(TEXT);
        body.setTextSize(16);
        body.setLineSpacing(dp(9), 1f);
        body.setPadding((int) dp(20), (int) dp(20), (int) dp(20), (int) dp(20));
        body.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        body.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                copy();
                return true;
            }
        });
        sv.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(card, clp);

        Button btn = new Button(this);
        btn.setText("换一篇");
        btn.setAllCaps(false);
        btn.setTextSize(16);
        btn.setTextColor(0xFF12060A);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setBackground(ripple(round(ACCENT, dp(16)), 0x33FFFFFF));
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showNext();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(52));
        blp.leftMargin = (int) dp(16);
        blp.rightMargin = (int) dp(16);
        blp.bottomMargin = (int) dp(8);
        root.addView(btn, blp);

        toggle = new Button(this);
        toggle.setAllCaps(false);
        toggle.setTextSize(14);
        toggle.setTextColor(ACCENT);
        toggle.setBackground(ripple(outline(0x00000000, LINE, dp(16)), 0x22FFFFFF));
        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = 1 - mode;
                prefs.edit().putInt("mode", mode).apply();
                refreshLabels();
                showNext();
            }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(46));
        tlp.leftMargin = (int) dp(16);
        tlp.rightMargin = (int) dp(16);
        tlp.bottomMargin = (int) dp(20);
        root.addView(toggle, tlp);

        setContentView(root);
        refreshLabels();
    }

    private void showAbout() {
        // The app theme is Theme.Material (light), so a stock AlertDialog
        // is light — but the content text here is near-white, which made
        // the author line invisible. Build the dialog dark end to end:
        // custom title inside the box, dark window background, accent button.
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(6));

        TextView t0 = new TextView(this);
        t0.setText("关于");
        t0.setTextColor(TEXT);
        t0.setTextSize(18);
        t0.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t0.setPadding(0, 0, 0, (int) dp(12));
        box.addView(t0);

        TextView t1 = new TextView(this);
        t1.setText("作者");
        t1.setTextColor(0xFF8A8A94);
        t1.setTextSize(12);
        box.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText(AUTHOR);
        t2.setTextColor(TEXT);
        t2.setTextSize(16);
        t2.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t2.setLineSpacing(dp(6), 1f);
        t2.setPadding(0, (int) dp(8), 0, (int) dp(14));
        t2.setTextIsSelectable(true);
        box.addView(t2);

        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        box.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(1)));

        String ver = "";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable ignored) {
        }

        TextView t3 = new TextView(this);
        t3.setText("每日一遍 " + (ver.length() > 0 ? "v" + ver : "") + "\n原创池 "
                + own.length + " 篇 · 抖音热门 " + hot.length + " 条\n默认模式不重复，随机模式可重复。长按正文可复制。");
        t3.setTextColor(0xFF8A8A94);
        t3.setTextSize(12);
        t3.setLineSpacing(dp(5), 1f);
        t3.setPadding(0, (int) dp(14), 0, 0);
        box.addView(t3);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("关闭", null)
                .show();
        try {
            if (dlg.getWindow() != null) {
                dlg.getWindow().setBackgroundDrawable(round(CARD, dp(18)));
            }
            Button pb = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (pb != null) pb.setTextColor(ACCENT);
        } catch (Throwable ignored) {
        }
    }

    private void refreshLabels() {
        if (toggle != null) {
            toggle.setText(mode == 0 ? "切到抖音热门" : "切回原创文案");
        }
        if (modeBtn != null) {
            modeBtn.setText(randomMode ? "随机模式" : "默认模式");
            modeBtn.setTextColor(randomMode ? ACCENT : 0xFF8A8A94);
        }
        if (source != null) {
            source.setText((mode == 0 ? "水泥封心 · 原创长文案" : "抖音热门 · 全网热门伤感语录合集")
                    + (randomMode ? " · 纯随机" : " · 不重复"));
        }
    }

    private void copy() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("每日一遍", body.getText()));
            Toast.makeText(this, "已复制，去粘贴吧", Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private void showNext() {
        String[] pool = mode == 0 ? own : hot;
        int last = mode == 0 ? lastOwn : lastHot;
        if (pool.length == 0) {
            body.setText("文案加载失败。");
            counter.setText("0 / 0");
            return;
        }
        Random r = new Random(System.nanoTime());
        int idx;
        if (randomMode) {
            idx = r.nextInt(pool.length);
            if (pool.length > 1) {
                while (idx == last) idx = r.nextInt(pool.length);
            }
        } else {
            Set<Integer> seen = mode == 0 ? seenOwn : seenHot;
            if (seen.size() >= pool.length) {
                // A full round is done; start a fresh one and say so.
                seen.clear();
                Toast.makeText(this, "这一轮看完了，从头再来", Toast.LENGTH_SHORT).show();
            }
            // Pick the k-th unseen entry directly. The old rejection loop
            // (random until unseen, capped at 5000 tries) could still land
            // on a seen entry when the pool was nearly exhausted.
            int k = r.nextInt(pool.length - seen.size());
            idx = -1;
            for (int i = 0; i < pool.length; i++) {
                if (!seen.contains(i)) {
                    if (k == 0) {
                        idx = i;
                        break;
                    }
                    k--;
                }
            }
            seen.add(idx);
        }

        if (mode == 0) {
            lastOwn = idx;
            countOwn++;
        } else {
            lastHot = idx;
            countHot++;
        }

        SharedPreferences.Editor ed = prefs.edit()
                .putInt("mode", mode)
                .putBoolean("random_mode", randomMode)
                .putInt("count_own", countOwn)
                .putInt("count_hot", countHot);
        if (mode == 0) ed.putString("seen_own", writeSeen(seenOwn));
        else ed.putString("seen_hot", writeSeen(seenHot));
        ed.apply();

        body.setText(pool[idx].trim());
        // A new quote starts at the top: without this, a ScrollView left
        // scrolled down by a long previous quote shows the new one mid-way.
        if (scroll != null) {
            scroll.scrollTo(0, 0);
            scroll.post(new Runnable() {
                public void run() {
                    scroll.scrollTo(0, 0);
                }
            });
        }
        // Subtle fade-in so the switch does not feel like a hard cut.
        body.setAlpha(0f);
        body.animate().alpha(1f).setDuration(180).start();

        String seenInfo;
        if (randomMode) {
            seenInfo = "随机模式";
        } else {
            int n = (mode == 0 ? seenOwn : seenHot).size();
            seenInfo = "已看 " + n + " / " + pool.length;
        }
        counter.setText("第 " + (idx + 1) + " / " + pool.length + " 篇 · 已翻 "
                + (mode == 0 ? countOwn : countHot) + " 次 · " + seenInfo);
    }
}
