package com.lengroo.snfx;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 设置 / 回顾 / 停一下 三个弹窗。 */
public final class Dialogs {

    private Dialogs() {
    }

    private static float dp(float v) {
        return ThemeKit.dp(v);
    }

    private static TextView row(final MainActivity a, String text, View.OnClickListener l) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextColor(ThemeKit.TEXT);
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding((int) dp(4), (int) dp(12), (int) dp(4), (int) dp(12));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    public static void showSettings(final MainActivity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(8));

        TextView title = new TextView(a);
        title.setText("设置");
        title.setTextColor(ThemeKit.TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title);

        CheckBox cb = new CheckBox(a);
        cb.setText("每翻 10 篇弹一句安慰话");
        cb.setTextSize(14);
        cb.setTextColor(ThemeKit.TEXT);
        cb.setChecked(a.prefs().getBoolean("comfort_on", true));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemeKit.ACCENT));
        cb.setPadding(0, (int) dp(14), 0, (int) dp(4));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                a.prefs().edit().putBoolean("comfort_on", on).apply();
                Toast.makeText(a, on ? "已开启：每 10 篇停一下" : "已关闭：不再弹安慰话",
                        Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(cb);

        CheckBox cont = new CheckBox(a);
        cont.setText("自动连读（念完自动下一篇）");
        cont.setTextSize(14);
        cont.setTextColor(ThemeKit.TEXT);
        cont.setChecked(a.prefs().getBoolean("continuous", false));
        cont.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemeKit.ACCENT));
        cont.setPadding(0, (int) dp(6), 0, (int) dp(4));
        cont.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                a.prefs().edit().putBoolean("continuous", on).apply();
                Toast.makeText(a, on ? "已开启连读：像播客一样一篇接一篇" : "已关闭连读",
                        Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(cont);

        TextView tip = new TextView(a);
        tip.setText("关掉安慰话后，翻多少篇都不会打断你。");
        tip.setTextColor(ThemeKit.SUB);
        tip.setTextSize(11);
        tip.setPadding(0, (int) dp(6), 0, (int) dp(14));
        box.addView(tip);

        box.addView(row(a, "清空浏览记录", new View.OnClickListener() {
            public void onClick(View v) {
                a.clearHistory();
            }
        }));
        box.addView(row(a, "导出备份到文件", new View.OnClickListener() {
            public void onClick(View v) {
                a.exportBackup();
            }
        }));
        box.addView(row(a, "从备份文件恢复", new View.OnClickListener() {
            public void onClick(View v) {
                a.importBackup();
            }
        }));

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        ThemeKit.styleDialog(dlg);
    }

    public static void showReview(final MainActivity a) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int week = 0;
        StringBuilder rows = new StringBuilder();
        String[] names = new String[]{"日", "一", "二", "三", "四", "五", "六"};
        for (int i = 6; i >= 0; i--) {
            java.util.Calendar d = (java.util.Calendar) c.clone();
            d.add(java.util.Calendar.DAY_OF_YEAR, -i);
            String key = String.format("%04d%02d%02d", d.get(java.util.Calendar.YEAR),
                    d.get(java.util.Calendar.MONTH) + 1, d.get(java.util.Calendar.DAY_OF_MONTH));
            int n = QuoteRepo.dailyCount(a.prefs(), key);
            week += n;
            rows.append(i == 0 ? "今天" : "周" + names[d.get(java.util.Calendar.DAY_OF_WEEK) - 1]);
            rows.append("  ").append(n).append(" 篇").append((char) 10);
        }

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(a);
        t.setText("回顾");
        t.setTextColor(ThemeKit.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView sum = new TextView(a);
        sum.setText("近 7 天翻了 " + week + " 篇 · 连续 " + QuoteRepo.streak(a.prefs()) + " 天");
        sum.setTextColor(ThemeKit.ACCENT);
        sum.setTextSize(15);
        sum.setPadding(0, (int) dp(10), 0, (int) dp(8));
        box.addView(sum);

        TextView detail = new TextView(a);
        detail.setText(rows.toString());
        detail.setTextColor(ThemeKit.SUB);
        detail.setTextSize(12);
        detail.setLineSpacing(dp(4), 1f);
        box.addView(detail);

        TextView foot = new TextView(a);
        foot.setText("收藏 " + a.favCount() + " 篇 · 当前池已看 "
                + a.seenCount() + " / " + a.poolLength());
        foot.setTextColor(ThemeKit.SUB);
        foot.setTextSize(12);
        foot.setPadding(0, (int) dp(12), 0, (int) dp(6));
        box.addView(foot);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        ThemeKit.styleDialog(dlg);
    }

    public static void showComfort(final MainActivity a) {
        String[] lines = QuoteRepo.loadComfort(a);
        String msg = lines[(int) (Math.random() * lines.length)].trim();

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(a);
        t.setText("停一下");
        t.setTextColor(ThemeKit.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView m = new TextView(a);
        m.setText(msg);
        m.setTextColor(ThemeKit.SUB);
        m.setTextSize(14);
        m.setLineSpacing(dp(6), 1f);
        m.setPadding(0, (int) dp(12), 0, (int) dp(6));
        box.addView(m);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        a.exitApp();
                    }
                })
                .setPositiveButton("继续", null)
                .show();
        ThemeKit.styleDialog(dlg);
    }
}
