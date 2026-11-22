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
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String SEP = "\n@@@\n";
    private static final String AUTHOR = "lengroo错觉与风季一起吃薯条并看着李空壳打CF";
    private int BG = 0xFF0B0B0D;
    private int CARD = 0xFF15151A;
    private int TEXT = 0xFFEDEAE6;
    private int SUB = 0xFF6E6E78;
    private int ACCENT = 0xFFFF5A7A;
    private int LINE = 0xFF3A3A42;
    private int RIPPLE = 0x22FFFFFF;

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
    private Button indexBtn;
    private int currentIndex = 0;
    private final Set<Integer> favOwn = new HashSet<Integer>();
    private final Set<Integer> favHot = new HashSet<Integer>();
    private Button favBtn;
    private Button speakBtn;
    private final ArrayList<Integer> history = new ArrayList<Integer>();
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean speakingNow = false;
    private boolean speakEnglish = false;
    private boolean continuousActive = false;
    private boolean loadedOwn = false;
    private boolean loadedHot = false;
    private static final int REQ_BACKUP_EXPORT = 1002;
    private static final int REQ_BACKUP_IMPORT = 1003;
    private static final Random RNG = new Random();
    private int textSizeLevel = 1;
    private int FIELD;
    private int HINT;
    private int flipStreak = 0;
    private Button todayBtn;
    private java.util.Map<Character, String> pyMap;
    private String[] pyCacheOwn;
    private String[] pyCacheHot;
    private int lineLevel = 1;
    private Button typeBtn;
    private AlertDialog typeDialog;
    private TextView source;
    private ScrollView scroll;
    private Button toggle;
    private Button modeBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
        prefs = getSharedPreferences("dby", MODE_PRIVATE);
        mode = prefs.getInt("mode", 0);
        randomMode = prefs.getBoolean("random_mode", false);
        countOwn = prefs.getInt("count_own", 0);
        countHot = prefs.getInt("count_hot", 0);
        readSeen("seen_own", seenOwn);
        readSeen("fav_own", favOwn);
        readSeen("fav_hot", favHot);
        speakEnglish = prefs.getBoolean("speak_en", false);
        textSizeLevel = prefs.getInt("text_size", 1);
        lineLevel = prefs.getInt("line_space", 1);
        readSeen("seen_hot", seenHot);

        ensurePools(mode);
        reloadImported();
        rebuildPools();
        startUi(savedInstanceState);
    }

    private void startUi(android.os.Bundle savedInstanceState) {
        buildUi();
        if (savedInstanceState != null && savedInstanceState.containsKey("cur_idx")) {
            String[] pool = mode == 0 ? own : hot;
            int idx = savedInstanceState.getInt("cur_idx", 0);
            if (pool.length > 0) {
                currentIndex = Math.max(0, Math.min(idx, pool.length - 1));
                renderIndex(currentIndex);
            } else {
                showNext();
            }
        } else {
            showNext();
        }
    }


    private void applyTheme() {
        ThemeKit.configure(this);
        boolean night = ThemeKit.isNight(this);
        BG = ThemeKit.BG;
        CARD = ThemeKit.CARD;
        TEXT = ThemeKit.TEXT;
        SUB = ThemeKit.SUB;
        ACCENT = ThemeKit.ACCENT;
        LINE = ThemeKit.LINE;
        RIPPLE = ThemeKit.RIPPLE;
        FIELD = ThemeKit.FIELD;
        HINT = ThemeKit.HINT;
        try {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                if (night) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        } catch (Throwable ignored) {
        }
    }

    private float dp(float v) {
        return ThemeKit.dp(v);
    }

    private String[] load(String name) {
        return QuoteRepo.load(this, name);
    }

    private void readSeen(String key, Set<Integer> into) {
        QuoteRepo.readSeen(prefs, key, into);
    }

    private String writeSeen(Set<Integer> set) {
        return QuoteRepo.writeSeen(set);
    }

    private GradientDrawable round(int color, float radius) {
        return ThemeKit.round(color, radius);
    }

    private GradientDrawable outline(int fill, int stroke, float radius) {
        return ThemeKit.outline(fill, stroke, radius);
    }

    // Gives a button press feedback: a Material ripple over the same shape.
    // A bare GradientDrawable has no pressed state, so buttons felt dead.
    private Drawable ripple(Drawable content, int color) {
        return ThemeKit.ripple(content, color);
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

    private void ensurePools(int m) {
        if (m == 0 && !loadedOwn) {
            baseOwn = load("quotes.txt");
            baseOwnEn = load("quotes_en.txt");
            loadedOwn = true;
        } else if (m == 1 && !loadedHot) {
            baseHot = load("hot.txt");
            baseHotEn = load("hot_en.txt");
            loadedHot = true;
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
        // 池子变了，拼音首字母缓存就过期了（导入/删除后下标会错位），下次搜索再重建。
        pyCacheOwn = null;
        pyCacheHot = null;
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
            ensurePools(0);
            ensurePools(1);
            rebuildPools();
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
        if (requestCode == REQ_BACKUP_EXPORT && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            writeBackupTo(data.getData());
        }
        if (requestCode == REQ_BACKUP_IMPORT && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            readBackupFrom(data.getData());
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
                .setNeutralButton("管理已导入", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        showImportManager();
                    }
                })
                .show();
        styleDialog(dlg);
    }

    private void styleDialog(AlertDialog d) {
        ThemeKit.styleDialog(d);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding((int) dp(20), (int) dp(18), (int) dp(14), 0);

        TextView title = new TextView(this);
        title.setText("每日一遍");
        title.setTextColor(TEXT);
        title.setTextSize(22);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            title.setAutoSizeTextTypeUniformWithConfiguration(15, 22, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
        }
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(title);

        modeBtn = new Button(this);
        modeBtn.setAllCaps(false);
        modeBtn.setTextSize(11);
        modeBtn.setTextColor(ACCENT);
        modeBtn.setPadding((int) dp(8), 0, (int) dp(8), 0);
        modeBtn.setMinWidth(0);
        modeBtn.setMinimumWidth(0);
        modeBtn.setMinHeight(0);
        modeBtn.setMinimumHeight(0);
        modeBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
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
        mlp.leftMargin = (int) dp(8);
        titleRow.addView(modeBtn, mlp);

        todayBtn = new Button(this);
        todayBtn.setText("今日");
        todayBtn.setAllCaps(false);
        todayBtn.setTextSize(11);
        todayBtn.setTextColor(SUB);
        todayBtn.setPadding((int) dp(8), 0, (int) dp(8), 0);
        todayBtn.setMinWidth(0);
        todayBtn.setMinimumWidth(0);
        todayBtn.setMinHeight(0);
        todayBtn.setMinimumHeight(0);
        todayBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        todayBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showToday();
            }
        });
        LinearLayout.LayoutParams tdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        tdLp.leftMargin = (int) dp(6);
        titleRow.addView(todayBtn, tdLp);

        Button reviewBtn = new Button(this);
        reviewBtn.setText("回顾");
        reviewBtn.setAllCaps(false);
        reviewBtn.setTextSize(11);
        reviewBtn.setTextColor(SUB);
        reviewBtn.setPadding((int) dp(8), 0, (int) dp(8), 0);
        reviewBtn.setMinWidth(0);
        reviewBtn.setMinimumWidth(0);
        reviewBtn.setMinHeight(0);
        reviewBtn.setMinimumHeight(0);
        reviewBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        reviewBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showReview();
            }
        });
        LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        rvLp.leftMargin = (int) dp(6);
        titleRow.addView(reviewBtn, rvLp);

        LinearLayout topButtons = new LinearLayout(this);
        topButtons.setOrientation(LinearLayout.HORIZONTAL);
        topButtons.setGravity(Gravity.CENTER_VERTICAL);

        Button settings = new Button(this);
        settings.setText("设置");
        settings.setAllCaps(false);
        settings.setTextSize(10);
        settings.setTextColor(SUB);
        settings.setPadding((int) dp(9), 0, (int) dp(9), 0);
        settings.setMinWidth(0);
        settings.setMinimumWidth(0);
        settings.setMinHeight(0);
        settings.setMinimumHeight(0);
        settings.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSettings();
            }
        });
        topButtons.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28)));

        Button about = new Button(this);
        about.setText("作者");
        about.setAllCaps(false);
        about.setTextSize(10);
        about.setTextColor(SUB);
        about.setPadding((int) dp(9), 0, (int) dp(9), 0);
        about.setMinWidth(0);
        about.setMinimumWidth(0);
        about.setMinHeight(0);
        about.setMinimumHeight(0);
        about.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        about.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAbout();
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        alp.leftMargin = (int) dp(6);
        topButtons.addView(about, alp);

        Button importer = new Button(this);
        importer.setText("导入");
        importer.setAllCaps(false);
        importer.setTextSize(10);
        importer.setTextColor(SUB);
        importer.setPadding((int) dp(9), 0, (int) dp(9), 0);
        importer.setMinWidth(0);
        importer.setMinimumWidth(0);
        importer.setMinHeight(0);
        importer.setMinimumHeight(0);
        importer.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        importer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showImportHelp();
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

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding((int) dp(20), (int) dp(8), 0, 0);

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
        root.addView(meta);

        source = new TextView(this);
        source.setTextColor(SUB);
        source.setTextSize(11);
        source.setPadding((int) dp(20), (int) dp(6), 0, 0);
        root.addView(source);

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
        sv.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy;
            long st;

            public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    sx = e.getX();
                    sy = e.getY();
                    st = System.currentTimeMillis();
                } else if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                    float dx = e.getX() - sx;
                    float dy = e.getY() - sy;
                    if (Math.abs(dx) > dp(56) && Math.abs(dx) > Math.abs(dy) * 1.6f
                            && System.currentTimeMillis() - st < 700) {
                        if (dx < 0) showNext();
                        else goBack();
                    }
                }
                return false;
            }
        });
        scroll = sv;
        body = new TextView(this);
        body.setTextColor(TEXT);
        body.setTextSize(16);
        body.setLineSpacing(dp(9), 1f);
        body.setPadding((int) dp(20), (int) dp(20), (int) dp(20), (int) dp(6));
        body.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        body.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showActions();
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

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 0, 0, (int) dp(10));
        favBtn = new Button(this);
        favBtn.setText("收藏");
        favBtn.setAllCaps(false);
        favBtn.setTextSize(11);
        favBtn.setTextColor(0xFF8A8A94);
        favBtn.setPadding((int) dp(16), 0, (int) dp(16), 0);
        favBtn.setMinWidth(0);
        favBtn.setMinimumWidth(0);
        favBtn.setMinHeight(0);
        favBtn.setMinimumHeight(0);
        favBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        favBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFavs();
            }
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        flp.rightMargin = (int) dp(8);
        footer.addView(favBtn, flp);

        indexBtn = new Button(this);
        indexBtn.setAllCaps(false);
        indexBtn.setTextSize(11);
        indexBtn.setTextColor(ACCENT);
        indexBtn.setPadding((int) dp(16), 0, (int) dp(16), 0);
        indexBtn.setMinWidth(0);
        indexBtn.setMinimumWidth(0);
        indexBtn.setMinHeight(0);
        indexBtn.setMinimumHeight(0);
        indexBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        indexBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showIndex();
            }
        });
        footer.addView(indexBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28)));

        speakBtn = new Button(this);
        speakBtn.setText("朗读");
        speakBtn.setAllCaps(false);
        speakBtn.setTextSize(11);
        speakBtn.setTextColor(0xFF8A8A94);
        speakBtn.setPadding((int) dp(16), 0, (int) dp(16), 0);
        speakBtn.setMinWidth(0);
        speakBtn.setMinimumWidth(0);
        speakBtn.setMinHeight(0);
        speakBtn.setMinimumHeight(0);
        speakBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        speakBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleSpeak();
            }
        });
        speakBtn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showSpeakLang();
                return true;
            }
        });
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        slp2.leftMargin = (int) dp(8);
        footer.addView(speakBtn, slp2);
        typeBtn = new Button(this);
        typeBtn.setText("Aa");
        typeBtn.setAllCaps(false);
        typeBtn.setTextSize(12);
        typeBtn.setTextColor(0xFF8A8A94);
        typeBtn.setPadding((int) dp(14), 0, (int) dp(14), 0);
        typeBtn.setMinWidth(0);
        typeBtn.setMinimumWidth(0);
        typeBtn.setMinHeight(0);
        typeBtn.setMinimumHeight(0);
        typeBtn.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        typeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showTypography();
            }
        });
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        tlp2.leftMargin = (int) dp(8);
        footer.addView(typeBtn, tlp2);

        card.addView(footer);

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
        toggle.setBackground(ripple(outline(0x00000000, LINE, dp(16)), RIPPLE));
        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = 1 - mode;
                prefs.edit().putInt("mode", mode).apply();
                ensurePools(mode);
                rebuildPools();
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
        applyTypography();
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

        Button poke = new Button(this);
        poke.setText("不要点我");
        poke.setAllCaps(false);
        poke.setTextSize(11);
        poke.setTextColor(0xFF8A8A94);
        poke.setPadding((int) dp(12), 0, (int) dp(12), 0);
        poke.setMinWidth(0);
        poke.setMinimumWidth(0);
        poke.setMinHeight(0);
        poke.setMinimumHeight(0);
        poke.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        poke.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSecret();
            }
        });

        LinearLayout aboutTitleRow = new LinearLayout(this);
        aboutTitleRow.setOrientation(LinearLayout.HORIZONTAL);
        aboutTitleRow.setGravity(Gravity.CENTER_VERTICAL);
        aboutTitleRow.setPadding(0, 0, 0, (int) dp(12));
        aboutTitleRow.addView(t0, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        aboutTitleRow.addView(poke, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28)));
        box.addView(aboutTitleRow);

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

    private void removeFrom(Set<Integer> set, int from) {
        List<Integer> drop = new ArrayList<Integer>();
        for (Integer v : set) {
            if (v >= from) drop.add(v);
        }
        set.removeAll(drop);
    }

    private void shiftSetAfterDelete(Set<Integer> set, int removed) {
        List<Integer> list = new ArrayList<Integer>(set);
        set.clear();
        for (Integer v : list) {
            if (v < removed) set.add(v);
            else if (v > removed) set.add(v - 1);
        }
    }

    private void persistAllSets() {
        prefs.edit().putString("seen_own", writeSeen(seenOwn))
                .putString("seen_hot", writeSeen(seenHot))
                .putString("fav_own", writeSeen(favOwn))
                .putString("fav_hot", writeSeen(favHot)).apply();
    }

    private void deleteImported(int pos) {
        String[] src = mode == 0 ? impOwn : impHot;
        String[] srcEn = mode == 0 ? impOwnEn : impHotEn;
        if (pos < 0 || pos >= src.length) return;
        int baseLen = (mode == 0 ? baseOwn : baseHot).length;
        int globalIdx = baseLen + pos;
        List<String> z = new ArrayList<String>();
        List<String> e = new ArrayList<String>();
        for (int i = 0; i < src.length; i++) {
            if (i == pos) continue;
            z.add(src[i]);
            e.add(i < srcEn.length ? srcEn[i] : "");
        }
        try {
            if (mode == 0) {
                writeCustom("custom_own.txt", z);
                writeCustom("custom_own_en.txt", e);
            } else {
                writeCustom("custom_hot.txt", z);
                writeCustom("custom_hot_en.txt", e);
            }
        } catch (Throwable t) {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            return;
        }
        shiftSetAfterDelete(seenOwn, globalIdx);
        shiftSetAfterDelete(seenHot, globalIdx);
        shiftSetAfterDelete(favOwn, globalIdx);
        shiftSetAfterDelete(favHot, globalIdx);
        persistAllSets();
        reloadImported();
        rebuildPools();
        if (currentIndex > globalIdx) currentIndex--;
        else if (currentIndex == globalIdx) currentIndex = Math.max(0, globalIdx - 1);
        renderIndex(currentIndex);
        Toast.makeText(this, "已删除这条导入的文案", Toast.LENGTH_SHORT).show();
    }

    private void clearImported() {
        int baseLen = (mode == 0 ? baseOwn : baseHot).length;
        try {
            if (mode == 0) {
                writeCustom("custom_own.txt", new ArrayList<String>());
                writeCustom("custom_own_en.txt", new ArrayList<String>());
            } else {
                writeCustom("custom_hot.txt", new ArrayList<String>());
                writeCustom("custom_hot_en.txt", new ArrayList<String>());
            }
        } catch (Throwable t) {
            Toast.makeText(this, "清空失败", Toast.LENGTH_SHORT).show();
            return;
        }
        removeFrom(seenOwn, baseLen);
        removeFrom(seenHot, baseLen);
        removeFrom(favOwn, baseLen);
        removeFrom(favHot, baseLen);
        persistAllSets();
        reloadImported();
        rebuildPools();
        currentIndex = 0;
        renderIndex(0);
        Toast.makeText(this, "已清空导入的文案", Toast.LENGTH_SHORT).show();
    }

    private void showImportManager() {
        final String[] src = mode == 0 ? impOwn : impHot;
        if (src.length == 0) {
            Toast.makeText(this, "还没有导入过文案", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> items = new ArrayList<String>();
        for (int i = 0; i < src.length; i++) {
            String t = src[i].replace(String.valueOf((char) 10), " ").trim();
            if (t.length() > 26) t = t.substring(0, 26) + "…";
            items.add((i + 1) + ".  " + t);
        }
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(TEXT);
                tv.setTextSize(13);
                tv.setPadding((int) dp(18), (int) dp(12), (int) dp(18), (int) dp(12));
                tv.setSingleLine(true);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return tv;
            }
        };
        ListView lv = new ListView(this);
        lv.setAdapter(ad);
        lv.setBackgroundColor(CARD);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setCacheColorHint(0);

        TextView title = new TextView(this);
        title.setText("我导入的 · 共 " + src.length + " 篇");
        title.setTextColor(TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(4));

        TextView hint = new TextView(this);
        hint.setText("点开跳到那篇，长按删除这一条");
        hint.setTextColor(0xFF6E6E78);
        hint.setTextSize(11);
        hint.setPadding((int) dp(20), 0, (int) dp(20), (int) dp(8));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.addView(title);
        box.addView(hint);
        box.addView(lv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(420)));

        final AlertDialog[] holder = new AlertDialog[1];
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("清空全部", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        confirmClearImported();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
        holder[0] = dlg;
        styleDialog(dlg);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int baseLen = (mode == 0 ? baseOwn : baseHot).length;
                jumpTo(baseLen + position);
                if (holder[0] != null) holder[0].dismiss();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                deleteImported(position);
                if (holder[0] != null) holder[0].dismiss();
                showImportManager();
                return true;
            }
        });
    }

    private void confirmClearImported() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(this);
        t.setText("清空导入的文案？");
        t.setTextColor(TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView m = new TextView(this);
        m.setText("会把当前池子里你自己导入的 "
                + (mode == 0 ? impOwn.length : impHot.length) + " 条全部删掉，内置文案不受影响。");
        m.setTextColor(SUB);
        m.setTextSize(13);
        m.setLineSpacing(dp(5), 1f);
        m.setPadding(0, (int) dp(10), 0, 0);
        box.addView(m);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("清空", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        clearImported();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        styleDialog(dlg);
    }

    private void showSecret() {
        String text = "";
        try {
            InputStream in = getAssets().open("nopoint.txt");
            text = readAll(in).trim();
            in.close();
        } catch (Throwable t) {
            text = "文本读取失败";
        }
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(TEXT);
        tv.setTextSize(14);
        tv.setLineSpacing(dp(7), 1f);
        tv.setPadding((int) dp(20), (int) dp(16), (int) dp(20), (int) dp(8));
        tv.setTextIsSelectable(true);

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        TextView title = new TextView(this);
        title.setText("不要点我");
        title.setTextColor(TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding((int) dp(20), (int) dp(18), (int) dp(20), 0);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.addView(title);
        box.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(460)));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("关闭", null)
                .show();
        styleDialog(dlg);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speakingNow) stopSpeak();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("cur_idx", currentIndex);
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
        Random r = RNG;
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
        // 今天的翻篇数也写进同一个 Editor，一次落盘。
        QuoteRepo.recordFlipInto(prefs, ed);
        ed.apply();

        pushHistory(idx);
        renderIndex(idx);
        flipStreak++;
        if (flipStreak >= 10) {
            flipStreak = 0;
            if (prefs.getBoolean("comfort_on", true)) showComfort();
        }
    }

    private void pushHistory(int idx) {
        history.add(idx);
        while (history.size() > 200) history.remove(0);
    }

    private void goBack() {
        if (history.size() < 2) {
            Toast.makeText(this, "已经在最新一篇了", Toast.LENGTH_SHORT).show();
            return;
        }
        history.remove(history.size() - 1);
        int prev = history.get(history.size() - 1);
        String[] pool = mode == 0 ? own : hot;
        if (prev >= pool.length) {
            history.clear();
            return;
        }
        currentIndex = prev;
        renderIndex(prev);
    }

    private void toggleSpeak() {
        if (speakingNow) {
            stopSpeak();
            Toast.makeText(this, "已停止朗读", Toast.LENGTH_SHORT).show();
            return;
        }
        if (body.getText().toString().trim().length() == 0) return;
        if (tts != null && ttsReady) {
            speakNow();
            return;
        }
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (Throwable ignored) {
            }
            tts = null;
            ttsReady = false;
        }
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    speakingNow = false;
                                    updateSpeakLabel();
                                    if (continuousActive && prefs.getBoolean("continuous", false)) {
                                        final android.os.Handler h = new android.os.Handler(
                                                android.os.Looper.getMainLooper());
                                        h.postDelayed(new Runnable() {
                                            public void run() {
                                                if (!continuousActive) return;
                                                showNext();
                                                h.postDelayed(new Runnable() {
                                                    public void run() {
                                                        if (continuousActive && !speakingNow) toggleSpeak();
                                                    }
                                                }, 600);
                                            }
                                        }, 400);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    speakingNow = false;
                                    updateSpeakLabel();
                                }
                            });
                        }
                    });
                    ttsReady = true;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            speakNow();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            showTtsHelp("系统里没有可用的语音引擎（TTS），多半是没装或没启用。");
                        }
                    });
                }
            }
        });
    }

    private void showTtsHelp(String why) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(this);
        t.setText("语音引擎初始化失败");
        t.setTextColor(TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView m = new TextView(this);
        m.setText(why + String.valueOf((char) 10) + String.valueOf((char) 10) +
                "可以到系统「设置 → 无障碍 → 文字转语音(TTS)」里安装 / 启用一个引擎并选中文，然后回来点「重试」。");
        m.setTextColor(SUB);
        m.setTextSize(13);
        m.setLineSpacing(dp(5), 1f);
        m.setPadding(0, (int) dp(10), 0, (int) dp(6));
        box.addView(m);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("打开语音设置", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        openTtsSettings();
                    }
                })
                .setNeutralButton("重试", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        tts = null;
                        ttsReady = false;
                        toggleSpeak();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
        styleDialog(dlg);
    }

    private void openTtsSettings() {
        String[] actions = new String[]{
                "com.android.settings.TTS_SETTINGS",
                "com.android.settings.Settings$TextToSpeechSettingsActivity",
                android.provider.Settings.ACTION_SETTINGS
        };
        for (int i = 0; i < actions.length; i++) {
            try {
                startActivity(new Intent(actions[i]));
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyTypography() {
        int[] zh = new int[]{14, 16, 18, 21};
        int[] en = new int[]{11, 13, 15, 17};
        int[] zl = new int[]{6, 9, 13};
        int[] el = new int[]{4, 6, 9};
        int ts = Math.max(0, Math.min(textSizeLevel, zh.length - 1));
        int ls = Math.max(0, Math.min(lineLevel, zl.length - 1));
        if (body != null) {
            body.setTextSize(zh[ts]);
            body.setLineSpacing(dp(zl[ls]), 1f);
        }
        if (trans != null) {
            trans.setTextSize(en[ts]);
            trans.setLineSpacing(dp(el[ls]), 1f);
        }
    }

    private void showTypography() {
        if (typeDialog != null && typeDialog.isShowing()) {
            typeDialog.dismiss();
        }
        typeDialog = null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(6));

        TextView title = new TextView(this);
        title.setText("排版");
        title.setTextColor(TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(0, 0, 0, (int) dp(12));
        box.addView(title);

        box.addView(labelRow("字号"));
        final String[] sizeNames = new String[]{"小", "标准", "大", "特大"};
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < sizeNames.length; i++) {
            final int which = i;
            sizeRow.addView(chip(sizeNames[i], textSizeLevel == i, new View.OnClickListener() {
                public void onClick(View v) {
                    textSizeLevel = which;
                    prefs.edit().putInt("text_size", textSizeLevel).apply();
                    applyTypography();
                    showTypography();
                }
            }), chipParams(i == 0 ? 0 : 8));
        }
        box.addView(sizeRow);

        TextView gap = new TextView(this);
        gap.setText("");
        gap.setHeight((int) dp(10));
        box.addView(gap);

        box.addView(labelRow("行距"));
        final String[] lineNames = new String[]{"紧凑", "标准", "宽松"};
        LinearLayout lineRow = new LinearLayout(this);
        lineRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < lineNames.length; i++) {
            final int which = i;
            lineRow.addView(chip(lineNames[i], lineLevel == i, new View.OnClickListener() {
                public void onClick(View v) {
                    lineLevel = which;
                    prefs.edit().putInt("line_space", lineLevel).apply();
                    applyTypography();
                    showTypography();
                }
            }), chipParams(i == 0 ? 0 : 8));
        }
        box.addView(lineRow);

        TextView tip = new TextView(this);
        tip.setText("中文与英文会一起缩放，设置会记住。");
        tip.setTextColor(SUB);
        tip.setTextSize(11);
        tip.setPadding(0, (int) dp(14), 0, 0);
        box.addView(tip);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        typeDialog = dlg;
        styleDialog(dlg);
    }

    private TextView labelRow(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(SUB);
        t.setTextSize(12);
        t.setPadding(0, (int) dp(4), 0, (int) dp(8));
        return t;
    }

    private LinearLayout.LayoutParams chipParams(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, (int) dp(34), 1f);
        lp.leftMargin = (int) dp(leftMargin);
        return lp;
    }

    private TextView chip(String text, boolean on, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(on ? ACCENT : TEXT);
        t.setBackground(outline(on ? 0x22FF5A7A : 0x00000000, on ? ACCENT : LINE, dp(12)));
        t.setOnClickListener(l);
        return t;
    }

    private void speakNow() {
        if (tts == null || !ttsReady) return;
        String text = speakEnglish
                ? (trans != null ? trans.getText().toString().trim() : "")
                : body.getText().toString().trim();
        if (text.length() == 0) {
            Toast.makeText(this, speakEnglish ? "这篇没有英文译文" : "没有可朗读的文字",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        int r = tts.setLanguage(speakEnglish ? Locale.US : Locale.SIMPLIFIED_CHINESE);
        if (!speakEnglish && (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)) {
            r = tts.setLanguage(Locale.CHINESE);
        }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            showTtsHelp(speakEnglish ? "语音引擎里没有英文语音包。" : "语音引擎里没有中文语音包。");
            return;
        }
        tts.setSpeechRate(speakEnglish ? 0.92f : 0.95f);
        speakingNow = true;
        continuousActive = true;
        updateSpeakLabel();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snfx");
        Toast.makeText(this, "朗读中（" + (speakEnglish ? "英文" : "中文") + "）· 点底部「停止」可结束",
                Toast.LENGTH_SHORT).show();
    }

    private void showSpeakLang() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(6));

        TextView title = new TextView(this);
        title.setText("朗读语言");
        title.setTextColor(TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(0, 0, 0, (int) dp(10));
        box.addView(title);

        String[] names = new String[]{"中文（默认）", "English"};
        boolean[] checked = new boolean[]{!speakEnglish, speakEnglish};
        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < names.length; i++) {
            final int which = i;
            final boolean on = checked[i];
            TextView row = new TextView(this);
            row.setText((on ? "✓  " : "     ") + names[i]);
            row.setTextColor(on ? ACCENT : TEXT);
            row.setTextSize(15);
            row.setPadding((int) dp(8), (int) dp(16), (int) dp(8), (int) dp(16));
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (holder[0] != null) holder[0].dismiss();
                    if (speakingNow) stopSpeak();
                    speakEnglish = which == 1;
                    prefs.edit().putBoolean("speak_en", speakEnglish).apply();
                    Toast.makeText(MainActivity.this,
                            "朗读语言：" + (speakEnglish ? "英文" : "中文"), Toast.LENGTH_SHORT).show();
                }
            });
            box.addView(row);
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        holder[0] = dlg;
        styleDialog(dlg);
    }

    private void stopSpeak() {
        speakingNow = false;
        continuousActive = false;
        updateSpeakLabel();
        if (tts != null) tts.stop();
    }

    private void updateSpeakLabel() {
        if (speakBtn != null) {
            speakBtn.setText(speakingNow ? "停止" : "朗读");
            speakBtn.setTextColor(speakingNow ? ACCENT : 0xFF8A8A94);
        }
    }

    private void renderIndex(int idx) {
        if (speakingNow) stopSpeak();
        currentIndex = idx;
        String[] pool = mode == 0 ? own : hot;
        String[] enPool = mode == 0 ? ownEn : hotEn;
        if (idx < 0 || idx >= pool.length) return;
        body.setText(pool[idx].trim());
        if (trans != null) trans.setText(idx < enPool.length ? enPool[idx].trim() : "");
        if (scroll != null) {
            scroll.scrollTo(0, 0);
            scroll.post(new Runnable() {
                public void run() {
                    scroll.scrollTo(0, 0);
                }
            });
        }
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
        if (indexBtn != null) indexBtn.setText("目录 · " + pool.length);
        if (favBtn != null) favBtn.setText("收藏 · " + (mode == 0 ? favOwn : favHot).size());
    }

    private void jumpTo(int idx) {
        String[] pool = mode == 0 ? own : hot;
        if (idx < 0 || idx >= pool.length) return;
        Set<Integer> seen = mode == 0 ? seenOwn : seenHot;
        seen.add(idx);
        if (mode == 0) {
            lastOwn = idx;
            countOwn++;
        } else {
            lastHot = idx;
            countHot++;
        }
        SharedPreferences.Editor ed = prefs.edit()
                .putInt("count_own", countOwn).putInt("count_hot", countHot)
                .putString("seen_own", writeSeen(seenOwn))
                .putString("seen_hot", writeSeen(seenHot));
        QuoteRepo.recordFlipInto(prefs, ed);
        ed.apply();
        pushHistory(idx);
        renderIndex(idx);
    }

    SharedPreferences prefs() {
        return prefs;
    }

    int favCount() {
        return favOwn.size() + favHot.size();
    }

    int seenCount() {
        return (mode == 0 ? seenOwn : seenHot).size();
    }

    int poolLength() {
        return (mode == 0 ? own : hot).length;
    }

    void exitApp() {
        finishAffinity();
    }

    private Set<Integer> favSet() {
        return mode == 0 ? favOwn : favHot;
    }

    private void persistFavs() {
        prefs.edit().putString("fav_own", writeSeen(favOwn))
                .putString("fav_hot", writeSeen(favHot)).apply();
    }

    private void toggleFav(int idx) {
        Set<Integer> set = favSet();
        if (set.contains(idx)) {
            set.remove(idx);
            Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
        } else {
            set.add(idx);
            Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show();
        }
        persistFavs();
        renderIndex(currentIndex);
    }

    private void showActions() {
        final int idx = currentIndex;
        boolean fav = favSet().contains(idx);
        String[] labels = new String[]{"复制", fav ? "取消收藏" : "收藏", "分享成图"};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            TextView row = new TextView(this);
            row.setText(labels[i]);
            row.setTextColor(which == 2 ? ACCENT : TEXT);
            row.setTextSize(15);
            row.setPadding((int) dp(24), (int) dp(18), (int) dp(24), (int) dp(18));
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (holder[0] != null) holder[0].dismiss();
                    if (which == 0) copy();
                    else if (which == 1) toggleFav(idx);
                    else shareCurrent(idx);
                }
            });
            box.addView(row);
        }
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("取消", null)
                .show();
        holder[0] = dlg;
        styleDialog(dlg);
    }

    private void shareCurrent(int idx) {
        String[] pool = mode == 0 ? own : hot;
        String[] enPool = mode == 0 ? ownEn : hotEn;
        if (idx < 0 || idx >= pool.length) return;
        String en = idx < enPool.length ? enPool[idx].trim() : "";
        try {
            ShareCard.render(getCacheDir(), pool[idx].trim(), en, "每日一遍 · " + AUTHOR);
            Uri u = Uri.parse("content://" + ShareProvider.AUTHORITY + "/share.png");
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "分享这篇"));
        } catch (Throwable t) {
            Toast.makeText(this, "生成分享图失败：" + t.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showFavs() {
        final Set<Integer> set = favSet();
        final String[] pool = mode == 0 ? own : hot;
        if (set.isEmpty()) {
            Toast.makeText(this, "还没有收藏，长按正文可以收藏", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Integer> ids = new ArrayList<Integer>();
        for (Integer i : set) {
            if (i >= 0 && i < pool.length) ids.add(i);
        }
        java.util.Collections.sort(ids);
        List<String> items = new ArrayList<String>();
        for (int k = 0; k < ids.size(); k++) {
            int real = ids.get(k);
            String t = pool[real].replace(String.valueOf((char) 10), " ").trim();
            if (t.length() > 26) t = t.substring(0, 26) + "…";
            items.add((k + 1) + ".  " + t);
        }
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(ids.get(position) == currentIndex ? ACCENT : TEXT);
                tv.setTextSize(13);
                tv.setPadding((int) dp(18), (int) dp(12), (int) dp(18), (int) dp(12));
                tv.setSingleLine(true);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return tv;
            }
        };
        ListView lv = new ListView(this);
        lv.setAdapter(ad);
        lv.setBackgroundColor(CARD);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setCacheColorHint(0);

        TextView title = new TextView(this);
        title.setText("收藏 · 共 " + ids.size() + " 篇");
        title.setTextColor(TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(4));

        TextView hint = new TextView(this);
        hint.setText("点开跳到那篇，长按移出收藏");
        hint.setTextColor(0xFF6E6E78);
        hint.setTextSize(11);
        hint.setPadding((int) dp(20), 0, (int) dp(20), (int) dp(8));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.addView(title);
        box.addView(hint);
        box.addView(lv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(440)));

        final AlertDialog[] holder = new AlertDialog[1];
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        holder[0] = dlg;
        styleDialog(dlg);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                jumpTo(ids.get(position));
                if (holder[0] != null) holder[0].dismiss();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                set.remove(ids.get(position));
                persistFavs();
                if (holder[0] != null) holder[0].dismiss();
                Toast.makeText(MainActivity.this, "已移出收藏", Toast.LENGTH_SHORT).show();
                showFavs();
                return true;
            }
        });
    }

    private String dayKey() {
        return QuoteRepo.dayKey();
    }

    private void showToday() {
        String[] pool = mode == 0 ? own : hot;
        if (pool.length == 0) return;
        java.util.Calendar c = java.util.Calendar.getInstance();
        long seed = c.get(java.util.Calendar.YEAR) * 1000L + c.get(java.util.Calendar.DAY_OF_YEAR);
        int idx = (int) Math.abs((seed * 2654435761L) % pool.length);
        jumpTo(idx % pool.length);
        Toast.makeText(this, "今天就看这一篇", Toast.LENGTH_SHORT).show();
    }

    private String[] loadComfort() {
        return QuoteRepo.loadComfort(this);
    }

    private void showComfort() {
        Dialogs.showComfort(this);
    }

    private void showSettings() {
        Dialogs.showSettings(this);
    }

    private void showReview() {
        Dialogs.showReview(this);
    }

    private void loadPy() {
        if (pyMap != null) return;
        pyMap = new java.util.HashMap<Character, String>();
        try {
            java.io.InputStream in = getAssets().open("pinyin.txt");
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() >= 3) pyMap.put(line.charAt(0), line.substring(2, 3));
            }
            br.close();
        } catch (Throwable ignored) {
        }
    }

    private String initialsOf(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 'a' && ch <= 'z') sb.append(ch);
            else if (ch >= 'A' && ch <= 'Z') sb.append((char) (ch + 32));
            else if (ch >= 0x4e00 && ch <= 0x9fff) {
                String p = pyMap == null ? null : pyMap.get(ch);
                if (p != null) sb.append(p);
            }
        }
        return sb.toString();
    }

    private String[] pyCache() {
        loadPy();
        if (mode == 0) {
            if (pyCacheOwn == null) {
                pyCacheOwn = new String[own.length];
                for (int i = 0; i < own.length; i++) pyCacheOwn[i] = initialsOf(own[i]);
            }
            return pyCacheOwn;
        }
        if (pyCacheHot == null) {
            pyCacheHot = new String[hot.length];
            for (int i = 0; i < hot.length; i++) pyCacheHot[i] = initialsOf(hot[i]);
        }
        return pyCacheHot;
    }

    void clearHistory() {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("清空浏览记录？")
                .setMessage("已看、已翻次数、每日统计都会归零，收藏保留。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        seenOwn.clear();
                        seenHot.clear();
                        countOwn = 0;
                        countHot = 0;
                        flipStreak = 0;
                        prefs.edit().putInt("count_own", 0).putInt("count_hot", 0)
                                .putString("seen_own", "").putString("seen_hot", "")
                                .putString("daily", "").apply();
                        refreshLabels();
                        showNext();
                        Toast.makeText(MainActivity.this, "记录已清空", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        styleDialog(dlg);
    }

    void exportBackup() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "meiriyibian-backup.json");
        try {
            startActivityForResult(i, REQ_BACKUP_EXPORT);
        } catch (Throwable t) {
            Toast.makeText(this, "打不开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    void importBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_BACKUP_IMPORT);
        } catch (Throwable t) {
            Toast.makeText(this, "打不开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeBackupTo(Uri uri) {
        try {
            java.io.OutputStream out = getContentResolver().openOutputStream(uri);
            String json = Backup.toJson(prefs, seenOwn, seenHot, favOwn, favHot);
            out.write(json.getBytes("UTF-8"));
            out.flush();
            out.close();
            Toast.makeText(this, "备份已导出", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "导出失败：" + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void readBackupFrom(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            String json = readAll(in);
            in.close();
            SharedPreferences.Editor ed = prefs.edit();
            if (!Backup.apply(json, ed, seenOwn, seenHot, favOwn, favHot)) {
                Toast.makeText(this, "这个文件不是本应用的备份", Toast.LENGTH_LONG).show();
                return;
            }
            mode = prefs.getInt("mode", 0);
            countOwn = prefs.getInt("count_own", 0);
            countHot = prefs.getInt("count_hot", 0);
            speakEnglish = prefs.getBoolean("speak_en", false);
            textSizeLevel = prefs.getInt("text_size", 1);
            lineLevel = prefs.getInt("line_space", 1);
            ensurePools(mode);
            rebuildPools();
            currentIndex = Math.min(currentIndex, Math.max(0, (mode == 0 ? own : hot).length - 1));
            applyTypography();
            refreshLabels();
            renderIndex(currentIndex);
            Toast.makeText(this, "备份已恢复", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "恢复失败：" + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void showIndex() {
        final String[] pool = mode == 0 ? own : hot;
        final String[] enPool = mode == 0 ? ownEn : hotEn;
        if (pool.length == 0) return;
        final List<Integer> ids = new ArrayList<Integer>();
        final List<String> rows = new ArrayList<String>();
        for (int i = 0; i < pool.length; i++) {
            ids.add(i);
            String t = pool[i].replace(String.valueOf((char) 10), " ").trim();
            if (t.length() > 26) t = t.substring(0, 26) + "…";
            rows.add((i + 1) + ".  " + t);
        }
        final ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                boolean cur = ids.get(position) == currentIndex;
                tv.setTextColor(cur ? ACCENT : TEXT);
                tv.setTypeface(Typeface.create("sans-serif", cur ? Typeface.BOLD : Typeface.NORMAL));
                tv.setTextSize(13);
                tv.setPadding((int) dp(18), (int) dp(12), (int) dp(18), (int) dp(12));
                tv.setSingleLine(true);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return tv;
            }
        };
        ListView lv = new ListView(this);
        lv.setAdapter(ad);
        lv.setBackgroundColor(CARD);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setCacheColorHint(0);

        EditText search = new EditText(this);
        search.setHint("搜索中文 / 英文 / 拼音首字母");
        search.setSingleLine(true);
        search.setTextColor(TEXT);

        search.setTextSize(14);
        search.setBackground(round(FIELD, dp(12)));
        search.setHintTextColor(HINT);
        search.setPadding((int) dp(16), (int) dp(10), (int) dp(16), (int) dp(10));

        TextView title = new TextView(this);
        title.setText("目录 · 共 " + pool.length + " 篇");
        title.setTextColor(TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        TextView dlgTitle = title;
        dlgTitle.setTextSize(17);

        Button locate = new Button(this);
        locate.setText("定位当前");
        locate.setAllCaps(false);
        locate.setTextSize(11);
        locate.setTextColor(ACCENT);
        locate.setPadding((int) dp(10), 0, (int) dp(10), 0);
        locate.setMinWidth(0);
        locate.setMinimumWidth(0);
        locate.setMinHeight(0);
        locate.setMinimumHeight(0);
        locate.setBackground(ripple(outline(0x00000000, LINE, dp(14)), RIPPLE));
        locate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int pos = ids.indexOf(currentIndex);
                if (pos < 0) return;
                int itemH = lv.getChildAt(0) != null ? lv.getChildAt(0).getHeight() : (int) dp(44);
                int offset = Math.max(0, (lv.getHeight() - itemH) / 2);
                lv.setSelectionFromTop(pos, offset);
            }
        });

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding((int) dp(20), (int) dp(16), (int) dp(14), (int) dp(4));
        titleRow.addView(dlgTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams locLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) dp(28));
        locLp.leftMargin = (int) dp(8);
        titleRow.addView(locate, locLp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.addView(titleRow);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = (int) dp(20);
        slp.rightMargin = (int) dp(20);
        slp.bottomMargin = (int) dp(8);
        box.addView(search, slp);
        box.addView(lv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(420)));

        final AlertDialog[] holder = new AlertDialog[1];
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        holder[0] = dlg;
        styleDialog(dlg);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                jumpTo(ids.get(position));
                if (holder[0] != null) holder[0].dismiss();
            }
        });

        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
                String kw = s.toString().trim().toLowerCase();
                boolean pyQuery = kw.length() > 0 && kw.matches("[a-z]+");
                String[] pyArr = pyQuery ? pyCache() : null;
                ids.clear();
                rows.clear();
                for (int i = 0; i < pool.length; i++) {
                    if (kw.length() == 0) {
                        ids.add(i);
                        String t = pool[i].replace(String.valueOf((char) 10), " ").trim();
                        if (t.length() > 26) t = t.substring(0, 26) + "…";
                        rows.add((i + 1) + ".  " + t);
                        continue;
                    }
                    String en = i < enPool.length ? enPool[i].toLowerCase() : "";
                    boolean hit = pool[i].contains(s.toString().trim()) || en.contains(kw);
                    if (!hit && pyQuery) {
                        hit = pyArr != null && i < pyArr.length && pyArr[i].contains(kw);
                    }
                    if (hit) {
                        ids.add(i);
                        String t = pool[i].replace(String.valueOf((char) 10), " ").trim();
                        if (t.length() > 26) t = t.substring(0, 26) + "…";
                        rows.add((i + 1) + ".  " + t);
                    }
                }
                ad.notifyDataSetChanged();
            }

            public void afterTextChanged(android.text.Editable e) {
            }
        });
    }
}
