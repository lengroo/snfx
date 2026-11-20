package com.lengroo.snfx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
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
    private String[] ownEn = new String[0];
    private String[] hotEn = new String[0];
    private String[] baseOwn = new String[0], baseOwnEn = new String[0];
    private String[] baseHot = new String[0], baseHotEn = new String[0];
    private String[] impOwn = new String[0], impOwnEn = new String[0];
    private String[] impHot = new String[0], impHotEn = new String[0];
    private static final int REQ_IMPORT = 1001;
    private int mode = 0;              // 0 原创 / 1 抖音热门
    private boolean randomMode = false; // false 默认（不重复） / true 纯随机
    private int lastOwn = -1, lastHot = -1;
    private int countOwn = 0, countHot = 0;
    private final Set<Integer> seenOwn = new HashSet<Integer>();
    private final Set<Integer> seenHot = new HashSet<Integer>();

    private TextView counter;
    private TextView body;
    private TextView trans;
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

        baseOwn = load("quotes.txt");
        baseHot = load("hot.txt");
        baseOwnEn = load("quotes_en.txt");
        baseHotEn = load("hot_en.txt");
        reloadImported();
        rebuildPools();
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

    private static String[] concat(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static String[] padTo(String[] en, int n) {
        if (en.length >= n) return en;
        String[] r = new String[n];
        for (int i = 0; i < n; i++) r[i] = i < en.length ? en[i] : "";
        return r;
    }

    private String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), "UTF-8");
    }

    private String[] loadCustom(String name) {
        try {
            File f = new File(getFilesDir(), name);
            if (!f.exists()) return new String[0];
            String all = readAll(new FileInputStream(f)).trim();
            if (all.length() == 0) return new String[0];
            return all.split(SEP);
        } catch (Throwable t) {
            return new String[0];
        }
    }

    private void reloadImported() {
        impOwn = loadCustom("custom_own.txt");
        impOwnEn = padTo(loadCustom("custom_own_en.txt"), impOwn.length);
        impHot = loadCustom("custom_hot.txt");
        impHotEn = padTo(loadCustom("custom_hot_en.txt"), impHot.length);
    }

    private void rebuildPools() {
        own = concat(baseOwn, impOwn);
        ownEn = concat(baseOwnEn, impOwnEn);
        hot = concat(baseHot, impHot);
        hotEn = concat(baseHotEn, impHotEn);
    }

    private void writeCustom(String name, List<String> list) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(SEP);
            sb.append(list.get(i));
        }
        FileOutputStream out = new FileOutputStream(new File(getFilesDir(), name));
        out.write(sb.toString().getBytes("UTF-8"));
        out.close();
    }

    // 导入格式：每行一篇；空行与 # 开头忽略；可写「中文|English」，英文可省略
    private void importFromUri(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            String all = readAll(in);
            in.close();
            List<String> zh = new ArrayList<String>();
            List<String> en = new ArrayList<String>();
            String[] lines = all.split("\r?\n");
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i].trim();
                if (ln.length() == 0) continue;
                if (ln.startsWith("#")) continue;
                String z = ln, e = "";
                int bar = ln.indexOf('|');
                if (bar < 0) bar = ln.indexOf((char) 0xFF5C);
                if (bar > 0) {
                    z = ln.substring(0, bar).trim();
                    e = ln.substring(bar + 1).trim();
                }
                if (z.length() < 4) continue;
                zh.add(z);
                en.add(e);
            }
            if (zh.isEmpty()) {
                Toast.makeText(this, "没读到可用文案，看看导入格式说明", Toast.LENGTH_LONG).show();
                return;
            }
            List<String> cz = new ArrayList<String>();
            List<String> ce = new ArrayList<String>();
            String[] srcZ = mode == 0 ? impOwn : impHot;
            String[] srcE = mode == 0 ? impOwnEn : impHotEn;
            for (int i = 0; i < srcZ.length; i++) {
                cz.add(srcZ[i]);
                ce.add(i < srcE.length ? srcE[i] : "");
            }
            Set<String> have = new HashSet<String>();
            for (String z : own) have.add(z.trim());
            for (String z : hot) have.add(z.trim());
            int added = 0;
            for (int i = 0; i < zh.size(); i++) {
                String z = zh.get(i);
                if (have.contains(z)) continue;
                have.add(z);
                cz.add(z);
                ce.add(en.get(i));
                added++;
            }
            if (added == 0) {
                Toast.makeText(this, "这些文案已经在里面了", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mode == 0) {
                writeCustom("custom_own.txt", cz);
                writeCustom("custom_own_en.txt", ce);
            } else {
                writeCustom("custom_hot.txt", cz);
                writeCustom("custom_hot_en.txt", ce);
            }
            reloadImported();
            rebuildPools();
            showNext();
            Toast.makeText(this, "已导入 " + added + " 条到" + (mode == 0 ? "原创池" : "热门池"),
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "导入失败：" + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importFromUri(data.getData());
        }
    }

    private void pickTxt() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/plain");
            startActivityForResult(i, REQ_IMPORT);
        } catch (Throwable t) {
            try {
                Intent i2 = new Intent(Intent.ACTION_GET_CONTENT);
                i2.setType("text/plain");
                startActivityForResult(i2, REQ_IMPORT);
            } catch (Throwable t2) {
                Toast.makeText(this, "这台设备没有可用的文件选择器", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showImportHelp() {
        String NL = String.valueOf((char) 10);
        String head = "导入你自己的 txt 到" + (mode == 0 ? "原创池" : "热门池") + "（当前这个池子）";
        String l1 = "格式：一行一篇";
        String l2 = "· 空行会被忽略";
        String l3 = "· 以 # 开头的行是注释，也会忽略";
        String l4 = "· 想带英文就写「中文|English」，竖线用英文或中文的都行";
        String l5 = "· 不写英文也可以，英文那行会空着";
        String l6 = "· 每篇 4 个字以上才会被收下";
        String eg = "示例：";
        String e1 = "# 我自己的文案";
        String e2 = "你别再试探我了，我不会心软的。|Stop testing me. I will not soften.";
        String e3 = "我的心是水泥做的，捂不热，也砸不碎。";
        String tail = "重复的会被跳过；导入后接在池子末尾。";
        StringBuilder sb = new StringBuilder();
        sb.append(head).append(NL).append(NL);
        sb.append(l1).append(NL);
        sb.append(l2).append(NL).append(l3).append(NL);
        sb.append(l4).append(NL).append(l5).append(NL).append(l6).append(NL).append(NL);
        sb.append(eg).append(NL).append(e1).append(NL);
        sb.append(e2).append(NL).append(e3).append(NL).append(NL).append(tail);
        TextView tip = new TextView(this);
        tip.setText(sb.toString());
        tip.setTextColor(TEXT);
        tip.setTextSize(13);
        tip.setLineSpacing(dp(5), 1f);
        tip.setPadding((int) dp(20), (int) dp(16), (int) dp(20), (int) dp(8));
        TextView dlgTitle = new TextView(this);
        dlgTitle.setText("导入 txt");
        dlgTitle.setTextColor(TEXT);
        dlgTitle.setTextSize(17);
        dlgTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        dlgTitle.setPadding((int) dp(20), (int) dp(18), (int) dp(20), 0);

        ScrollView sv = new ScrollView(this);
        sv.addView(tip);

        LinearLayout dlgBox = new LinearLayout(this);
        dlgBox.setOrientation(LinearLayout.VERTICAL);
        dlgBox.addView(dlgTitle);
        dlgBox.addView(sv);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(dlgBox)
                .setPositiveButton("选择文件", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        pickTxt();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        styleDialog(dlg);
    }

    private void styleDialog(AlertDialog d) {
        try {
            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new android.graphics.drawable.InsetDrawable(
                        round(CARD, dp(18)), (int) dp(6)));
            }
            Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok != null) ok.setTextColor(ACCENT);
            Button no = d.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (no != null) no.setTextColor(SUB);
        } catch (Throwable ignored) {
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.TOP);
        headerRow.setPadding((int) dp(20), (int) dp(20), (int) dp(14), (int) dp(8));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("每日一遍");
        title.setTextColor(TEXT);
        title.setTextSize(24);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(title);
        header.addView(titleRow);

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
        counter.setTextSize(11);
        counter.setSingleLine(true);
        counter.setEllipsize(android.text.TextUtils.TruncateAt.END);
        meta.addView(counter);
        header.addView(meta);

        source = new TextView(this);
        source.setTextColor(SUB);
        source.setTextSize(11);
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
        modeBtn.setTextSize(11);
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
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        mlp.leftMargin = (int) dp(12);
        titleRow.addView(modeBtn, mlp);

        Button about = new Button(this);
        about.setText("作者");
        about.setAllCaps(false);
        about.setTextSize(11);
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
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        alp.leftMargin = (int) dp(8);
        topButtons.addView(about, alp);

        Button importer = new Button(this);
        importer.setText("导入");
        importer.setAllCaps(false);
        importer.setTextSize(11);
        importer.setTextColor(SUB);
        importer.setPadding((int) dp(12), 0, (int) dp(12), 0);
        importer.setMinWidth(0);
        importer.setMinimumWidth(0);
        importer.setMinHeight(0);
        importer.setMinimumHeight(0);
        importer.setBackground(ripple(outline(0x00000000, LINE, dp(14)), 0x22FFFFFF));
        importer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showImportHelp();
            }
        });
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        ilp.leftMargin = (int) dp(8);
        topButtons.addView(importer, ilp);

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
        body.setPadding((int) dp(20), (int) dp(20), (int) dp(20), (int) dp(6));
        body.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        body.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                copy();
                return true;
            }
        });
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        trans = new TextView(this);
        trans.setTextColor(0xFF8A8A94);
        trans.setTextSize(13);
        trans.setLineSpacing(dp(6), 1f);
        trans.setPadding((int) dp(20), 0, (int) dp(20), (int) dp(20));
        trans.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        textCol.addView(trans, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.addView(textCol, new ViewGroup.LayoutParams(
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
        styleDialog(dlg);
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
            String zh = body.getText().toString();
            String en = trans != null ? trans.getText().toString() : "";
            String all = en.length() > 0 ? zh + "\n\n" + en : zh;
            cm.setPrimaryClip(ClipData.newPlainText("每日一遍", all));
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
        String[] enPool = mode == 0 ? ownEn : hotEn;
        if (trans != null) trans.setText(idx < enPool.length ? enPool[idx].trim() : "");
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
        if (trans != null) {
            trans.setAlpha(0f);
            trans.animate().alpha(1f).setDuration(180).start();
        }

        String seenInfo;
        if (randomMode) {
            seenInfo = "随机模式";
        } else {
            int n = (mode == 0 ? seenOwn : seenHot).size();
            seenInfo = "已看 " + n + " / " + pool.length;
        }
        int flips = mode == 0 ? countOwn : countHot;
        counter.setText("第 " + (idx + 1) + " / " + pool.length + " 篇 · 已翻 " + flips + " 次");
        source.setText((mode == 0 ? "水泥封心 · 原创长文案" : "抖音热门 · 全网热门语录")
                + " · " + seenInfo);
    }
}
