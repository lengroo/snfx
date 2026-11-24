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
import android.widget.CheckBox;
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
    static final int REQ_IMPORT = 1001;
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
    private boolean loadedOwn = false;
    private boolean loadedHot = false;
    private static final int REQ_BACKUP_EXPORT = 1002;
    private static final int REQ_BACKUP_IMPORT = 1003;
    private static final Random RNG = new Random();
    private int FIELD;
    private int HINT;
    private int flipStreak = 0;
    private Button todayBtn;
    private java.util.Map<Character, String> pyMap;
    private String[] pyCacheOwn;
    private String[] pyCacheHot;
    private Button typeBtn;
    private TextView source;
    private ScrollView scroll;
    private float swipeSx, swipeSy;
    private long swipeSt;
    private boolean swipeFired;
    private Button toggle;
    private Button modeBtn;
    private SharedPreferences prefs;
    private final TtsController ttsCtl = new TtsController();

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
        ttsCtl.setSpeakEnglish(prefs.getBoolean("speak_en", false));
        ttsCtl.setTextSizeLevel(prefs.getInt("text_size", 1));
        ttsCtl.setLineLevel(prefs.getInt("line_space", 1));
        readSeen("seen_hot", seenHot);

        ensurePools(mode);
        reloadImported();
        rebuildPools();
        startUi(savedInstanceState);
        // 提醒开着的话，把闹钟重新约一遍（重启 / 强行停止后闹钟会丢）。
        if (prefs.getBoolean("reminder_on", false)) ReminderReceiver.schedule(this);
    }

    private void startUi(android.os.Bundle savedInstanceState) {
        UiBuilder.Views v = UiBuilder.buildUi(this);
        modeBtn = v.modeBtn;
        todayBtn = v.todayBtn;
        counter = v.counter;
        source = v.source;
        scroll = v.scroll;
        body = v.body;
        trans = v.trans;
        favBtn = v.favBtn;
        indexBtn = v.indexBtn;
        speakBtn = v.speakBtn;
        typeBtn = v.typeBtn;
        toggle = v.toggle;
        ttsCtl.applyTypography(this);
        refreshLabels();
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
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                checkCapsule();
            }
        }, 600);
    }


    private void applyTheme() {
        int mode = getSharedPreferences("dby", MODE_PRIVATE)
                .getInt("theme_mode", ThemeKit.THEME_SYSTEM);
        ThemeKit.configure(this, mode);
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

    String readAll(InputStream in) throws Exception {
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

    void ensurePools(int m) {
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

    void reloadImported() {
        impOwn = loadCustom("custom_own.txt");
        impOwnEn = padTo(loadCustom("custom_own_en.txt"), impOwn.length);
        impHot = loadCustom("custom_hot.txt");
        impHotEn = padTo(loadCustom("custom_hot_en.txt"), impHot.length);
    }

    void rebuildPools() {
        own = concat(baseOwn, impOwn);
        ownEn = concat(baseOwnEn, impOwnEn);
        hot = concat(baseHot, impHot);
        hotEn = concat(baseHotEn, impHotEn);
        // 池子变了，拼音首字母缓存就过期了（导入/删除后下标会错位），下次搜索再重建。
        pyCacheOwn = null;
        pyCacheHot = null;
    }

    void writeCustom(String name, List<String> list) throws Exception {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            ImportManager.importFromUri(this, data.getData());
        }
        if (resultCode == RESULT_OK && BackupIO.handle(this, requestCode, resultCode, data)) {
            return;
        }
    }



    private void styleDialog(AlertDialog d) {
        ThemeKit.styleDialog(d);
    }


    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent e) {
        // 卡片内的左右滑动切篇：在 Activity 层跟踪整段手势。
        // 之前把监听挂在 ScrollView 上，手指落在正文上时事件先被 TextView 接走，
        // 监听收不到 MOVE/UP，所以有的时候怎么划都不切篇。
        if (scroll != null) {
            int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                int[] loc = new int[2];
                scroll.getLocationOnScreen(loc);
                float x = e.getRawX(), y = e.getRawY();
                if (x >= loc[0] && x <= loc[0] + scroll.getWidth()
                        && y >= loc[1] && y <= loc[1] + scroll.getHeight()) {
                    swipeSx = e.getRawX();
                    swipeSy = e.getRawY();
                    swipeSt = System.currentTimeMillis();
                    swipeFired = false;
                } else {
                    swipeFired = true;
                }
            } else if (action == android.view.MotionEvent.ACTION_MOVE) {
                if (!swipeFired) {
                    float dx = e.getRawX() - swipeSx;
                    float dy = e.getRawY() - swipeSy;
                    if (Math.abs(dx) > dp(40) && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                        swipeFired = true;
                        if (dx < 0) showNext();
                        else goBack();
                    }
                }
            } else if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {
                if (!swipeFired) {
                    float dx = e.getRawX() - swipeSx;
                    float dy = e.getRawY() - swipeSy;
                    if (Math.abs(dx) > dp(40) && Math.abs(dx) > Math.abs(dy) * 1.2f
                            && System.currentTimeMillis() - swipeSt < 1200) {
                        if (dx < 0) showNext();
                        else goBack();
                    }
                }
                swipeFired = true;
            }
        }
        return super.dispatchTouchEvent(e);
    }

    void showAbout() {
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

    void shiftAllSetsAfterDelete(int removed) {
        shiftSetAfterDelete(seenOwn, removed);
        shiftSetAfterDelete(seenHot, removed);
        shiftSetAfterDelete(favOwn, removed);
        shiftSetAfterDelete(favHot, removed);
    }

    void removeFromAllSets(int from) {
        removeFrom(seenOwn, from);
        removeFrom(seenHot, from);
        removeFrom(favOwn, from);
        removeFrom(favHot, from);
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

    void persistAllSets() {
        prefs.edit().putString("seen_own", writeSeen(seenOwn))
                .putString("seen_hot", writeSeen(seenHot))
                .putString("fav_own", writeSeen(favOwn))
                .putString("fav_hot", writeSeen(favHot)).apply();
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
        if (ttsCtl.isSpeakingNow()) ttsCtl.stopSpeak(this);
    }

    @Override
    protected void onDestroy() {
        ttsCtl.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("cur_idx", currentIndex);
    }

    void toggleRandomMode() {
        randomMode = !randomMode;
        prefs.edit().putBoolean("random_mode", randomMode).apply();
        refreshLabels();
        showNext();
    }

    void toggleMode() {
        mode = 1 - mode;
        prefs.edit().putInt("mode", mode).apply();
        ensurePools(mode);
        rebuildPools();
        refreshLabels();
        showNext();
    }

    void refreshLabels() {
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

    void showNext() {
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
        renderIndex(idx, 1);
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
        renderIndex(prev, -1);
    }













    void renderIndex(int idx) {
        renderIndex(idx, 0);
    }

    /** 翻篇：dir = 1 往前翻（内容从右滑进来），-1 往回翻（从左滑进来），0 不滑动。 */
    void renderIndex(int idx, int dir) {
        if (ttsCtl.isSpeakingNow()) ttsCtl.stopSpeak(this);
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
        // 淡入 + 轻滑：先退到 24dp 外、透明，再滑回来。
        float fromX = dir == 0 ? 0f : dir * dp(24);
        body.setAlpha(0f);
        body.setTranslationX(fromX);
        body.animate().alpha(1f).translationX(0f).setDuration(220).start();
        if (trans != null) {
            trans.setAlpha(0f);
            trans.setTranslationX(fromX);
            trans.animate().alpha(1f).translationX(0f).setDuration(220).start();
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

    void jumpTo(int idx) {
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
        renderIndex(idx, 1);
    }

    TtsController ttsCtl() {
        return ttsCtl;
    }

    TextView bodyView() {
        return body;
    }

    TextView transView() {
        return trans;
    }

    void setSpeakLabel(boolean speaking) {
        if (speakBtn != null) {
            speakBtn.setText(speaking ? "停止" : "朗读");
            speakBtn.setTextColor(speaking ? ACCENT : 0xFF8A8A94);
        }
    }

    int mode() {
        return mode;
    }

    String[] importPool() {
        return mode == 0 ? impOwn : impHot;
    }

    String[] importPoolEn() {
        return mode == 0 ? impOwnEn : impHotEn;
    }

    String[] basePool() {
        return mode == 0 ? baseOwn : baseHot;
    }

    String[] own() {
        return own;
    }

    String[] hot() {
        return hot;
    }

    int currentIndex() {
        return currentIndex;
    }

    void setCurrentIndex(int idx) {
        currentIndex = idx;
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

    void showActions() {
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

    private void shareCurrent(final int idx) {
        String[] pool = mode == 0 ? own : hot;
        String[] enPool = mode == 0 ? ownEn : hotEn;
        if (idx < 0 || idx >= pool.length) return;
        final String en = idx < enPool.length ? enPool[idx].trim() : "";
        final String zh = pool[idx].trim();
        final int last = prefs.getInt("share_template", ShareCard.TEMPLATE_DARK);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final AlertDialog[] holder = new AlertDialog[1];
        String[] names = {"深色杂志", "浅色纸感", "红", "橙", "黄", "绿", "蓝", "靛", "紫"};
        for (int k = 0; k < names.length; k++) {
            final int template = k;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int) dp(24), (int) dp(18), (int) dp(24), (int) dp(18));
            View dot = new View(this);
            dot.setBackground(outline(ShareCard.templateColor(template), LINE, dp(8)));
            row.addView(dot, new LinearLayout.LayoutParams((int) dp(16), (int) dp(16)));
            TextView name = new TextView(this);
            name.setText(names[k] + (template == last ? "  ✓" : ""));
            name.setTextColor(template == last ? ACCENT : TEXT);
            name.setTextSize(15);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.leftMargin = (int) dp(12);
            row.addView(name, nlp);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (holder[0] != null) holder[0].dismiss();
                    prefs.edit().putInt("share_template", template).apply();
                    renderAndShare(zh, en, template);
                }
            });
            box.addView(row);
        }
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(new android.widget.ScrollView(this) {{
                    addView(box);
                }})
                .setNegativeButton("取消", null)
                .show();
        holder[0] = dlg;
        styleDialog(dlg);
    }

    private void renderAndShare(String zh, String en, int template) {
        try {
            ShareCard.render(getCacheDir(), zh, en, "每日一遍 · " + AUTHOR, template);
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

    void showFavs() {
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

    void showToday() {
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

    void showSettings() {
        Dialogs.showSettings(this);
    }

    public String themeModeName(int mode) {
        return mode == ThemeKit.THEME_DARK ? "深色"
                : mode == ThemeKit.THEME_LIGHT ? "浅色" : "跟随系统";
    }

    /** 手动换主题：存下来，重建界面（当前篇会保留）。 */
    public void setThemeMode(int mode) {
        prefs.edit().putInt("theme_mode", mode).apply();
        Toast.makeText(this, "已切换到" + themeModeName(mode), Toast.LENGTH_SHORT).show();
        recreate();
    }

    void showReview() {
        Dialogs.showReview(this);
    }

    private static final int REQ_REMINDER_NOTIF = 1001;
    private CheckBox pendingReminderBox;
    private boolean suppressReminderCb;

    /** 定时提醒的开关：开要通知权限，没给就先申请，决定等回调。 */
    public void setReminder(boolean on, CheckBox cb) {
        if (suppressReminderCb) return;
        if (!on) {
            prefs.edit().putBoolean("reminder_on", false).apply();
            ReminderReceiver.cancel(this);
            Toast.makeText(this, "已关闭定时提醒", Toast.LENGTH_SHORT).show();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingReminderBox = cb;
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQ_REMINDER_NOTIF);
            return;
        }
        prefs.edit().putBoolean("reminder_on", true).apply();
        ReminderReceiver.schedule(this);
        Toast.makeText(this, "已开启：每天 " + reminderTimeStr() + " 提醒你看今日一句",
                Toast.LENGTH_SHORT).show();
    }

    public String reminderTimeStr() {
        return String.format(Locale.CHINA, "%02d:%02d",
                prefs.getInt("reminder_hour", 22), prefs.getInt("reminder_min", 0));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_REMINDER_NOTIF) return;
        CheckBox cb = pendingReminderBox;
        pendingReminderBox = null;
        if (grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // 勾已经打上了，不用再碰它，直接约闹钟
            setReminder(true, cb);
        } else {
            // 编程式取消勾选，不走开关流程
            suppressReminderCb = true;
            if (cb != null) cb.setChecked(false);
            suppressReminderCb = false;
            Toast.makeText(this, "没给通知权限就推不了，去系统设置里打开通知",
                    Toast.LENGTH_LONG).show();
        }
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

    private String capsuleDay(java.util.Calendar c) {
        return String.format("%04d%02d%02d", c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }

    void editCapsule() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.setPadding((int) dp(22), (int) dp(18), (int) dp(22), (int) dp(6));

        TextView t = new TextView(this);
        t.setText("埋一句给未来的自己");
        t.setTextColor(TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        final EditText et = new EditText(this);
        et.setText(prefs.getString("capsule_text", ""));
        et.setHint("写给那天的你");
        et.setMinLines(3);
        et.setMaxLines(6);
        et.setGravity(Gravity.TOP);
        et.setTextColor(TEXT);
        et.setHintTextColor(HINT);
        et.setBackground(round(FIELD, dp(12)));
        et.setPadding((int) dp(14), (int) dp(12), (int) dp(14), (int) dp(12));
        LinearLayout.LayoutParams etlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etlp.topMargin = (int) dp(12);
        box.addView(et, etlp);

        final java.util.Calendar when = java.util.Calendar.getInstance();
        String saved = prefs.getString("capsule_date", "");
        if (saved.length() == 8) {
            try {
                when.set(Integer.parseInt(saved.substring(0, 4)),
                        Integer.parseInt(saved.substring(4, 6)) - 1,
                        Integer.parseInt(saved.substring(6, 8)));
            } catch (Throwable ignored) {
                when.add(java.util.Calendar.DAY_OF_YEAR, 14);
            }
        } else {
            when.add(java.util.Calendar.DAY_OF_YEAR, 14);
        }

        final TextView dateRow = new TextView(this);
        dateRow.setText("打开日期：" + when.get(java.util.Calendar.YEAR) + "-"
                + (when.get(java.util.Calendar.MONTH) + 1) + "-"
                + when.get(java.util.Calendar.DAY_OF_MONTH));
        dateRow.setTextColor(ACCENT);
        dateRow.setTextSize(14);
        dateRow.setPadding(0, (int) dp(14), 0, (int) dp(6));
        dateRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new android.app.DatePickerDialog(MainActivity.this,
                        new android.app.DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(android.widget.DatePicker view, int y, int m, int d) {
                        when.set(y, m, d);
                        dateRow.setText("打开日期：" + y + "-" + (m + 1) + "-" + d);
                    }
                }, when.get(java.util.Calendar.YEAR), when.get(java.util.Calendar.MONTH),
                        when.get(java.util.Calendar.DAY_OF_MONTH)).show();
            }
        });
        box.addView(dateRow);

        TextView tip = new TextView(this);
        tip.setText("到那天打开软件，它才会出现。别的时候看不到。");
        tip.setTextColor(SUB);
        tip.setTextSize(11);
        box.addView(tip);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("埋下", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        String txt = et.getText().toString().trim();
                        if (txt.length() == 0) {
                            Toast.makeText(MainActivity.this, "还没写呢", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        prefs.edit().putString("capsule_text", txt)
                                .putString("capsule_date", capsuleDay(when))
                                .putBoolean("capsule_sent", false).apply();
                        ReminderReceiver.scheduleCapsule(MainActivity.this);
                        Toast.makeText(MainActivity.this, "已经埋好了，那天早上会提醒你",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("清除", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        prefs.edit().putString("capsule_text", "").putString("capsule_date", "")
                                .putBoolean("capsule_sent", false).apply();
                        ReminderReceiver.cancelCapsule(MainActivity.this);
                        Toast.makeText(MainActivity.this, "已清除", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        styleDialog(dlg);
    }

    private void checkCapsule() {
        final String txt = prefs.getString("capsule_text", "");
        String day = prefs.getString("capsule_date", "");
        if (txt.length() == 0 || day.length() != 8) return;
        if (prefs.getBoolean("capsule_sent", false)) return;
        String today = QuoteRepo.dayKey();
        if (today.compareTo(day) < 0) return;
        prefs.edit().putBoolean("capsule_sent", true).apply();
        ReminderReceiver.cancelCapsule(this);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(this);
        t.setText("那天埋下的话");
        t.setTextColor(TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView body2 = new TextView(this);
        body2.setText(txt);
        body2.setTextColor(TEXT);
        body2.setTextSize(15);
        body2.setLineSpacing(dp(6), 1f);
        body2.setPadding(0, (int) dp(12), 0, (int) dp(8));
        box.addView(body2);

        TextView when = new TextView(this);
        when.setText("原定日期：" + day.substring(0, 4) + "-" + day.substring(4, 6) + "-"
                + day.substring(6, 8));
        when.setTextColor(SUB);
        when.setTextSize(11);
        box.addView(when);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("收下", null)
                .show();
        styleDialog(dlg);
    }

    String buildBackupJson() {
        return Backup.toJson(prefs, seenOwn, seenHot, favOwn, favHot);
    }

    boolean applyBackupJson(String json) {
        SharedPreferences.Editor ed = prefs.edit();
        if (!Backup.apply(json, ed, seenOwn, seenHot, favOwn, favHot)) return false;
        mode = prefs.getInt("mode", 0);
        countOwn = prefs.getInt("count_own", 0);
        countHot = prefs.getInt("count_hot", 0);
        ttsCtl.setSpeakEnglish(prefs.getBoolean("speak_en", false));
        ttsCtl.setTextSizeLevel(prefs.getInt("text_size", 1));
        ttsCtl.setLineLevel(prefs.getInt("line_space", 1));
        ensurePools(mode);
        rebuildPools();
        int len = (mode == 0 ? own : hot).length;
        currentIndex = Math.max(0, Math.min(currentIndex, Math.max(0, len - 1)));
        ttsCtl.applyTypography(this);
        refreshLabels();
        if (len > 0) renderIndex(currentIndex);
        checkCapsule();
        return true;
    }

    void exportBackup() {
        BackupIO.export(this);
    }

    void importBackup() {
        BackupIO.importFrom(this);
    }

    private void writeBackupTo(Uri uri) {
        // 逻辑在 BackupIO，这里只留薄封装
    }

    private void readBackupFrom(Uri uri) {
        // 逻辑在 BackupIO，这里只留薄封装
    }

    void showIndex() {
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
