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
        box.setPadding((int) dp(22), (int) dp(18), (int) dp(22), (int) dp(8));

        TextView title = new TextView(a);
        title.setText("设置");
        title.setTextColor(ThemeKit.TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title);

        section(box, a, "提醒");
        CheckBox cb = new CheckBox(a);
        cb.setText("每翻 10 篇弹一句安慰话");
        cb.setTextSize(14);
        cb.setTextColor(ThemeKit.TEXT);
        cb.setChecked(a.prefs().getBoolean("comfort_on", true));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemeKit.ACCENT));
        cb.setPadding(0, (int) dp(8), 0, (int) dp(2));
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
        cont.setPadding(0, (int) dp(2), 0, (int) dp(2));
        cont.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                a.prefs().edit().putBoolean("continuous", on).apply();
                Toast.makeText(a, on ? "已开启连读：像播客一样一篇接一篇" : "已关闭连读",
                        Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(cont);

        final CheckBox rem = new CheckBox(a);
        rem.setText("每天定时提醒我看今日一句");
        rem.setTextSize(14);
        rem.setTextColor(ThemeKit.TEXT);
        rem.setChecked(a.prefs().getBoolean("reminder_on", false));
        rem.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemeKit.ACCENT));
        rem.setPadding(0, (int) dp(2), 0, (int) dp(2));
        rem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                a.setReminder(on, rem);
            }
        });
        box.addView(rem);

        final TextView remTime = new TextView(a);
        remTime.setText("提醒时间 " + a.reminderTimeStr());
        remTime.setTextColor(ThemeKit.SUB);
        remTime.setTextSize(13);
        remTime.setGravity(Gravity.CENTER_VERTICAL);
        remTime.setPadding((int) dp(30), (int) dp(6), (int) dp(4), (int) dp(10));
        remTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int h = a.prefs().getInt("reminder_hour", 22);
                int m = a.prefs().getInt("reminder_min", 0);
                new android.app.TimePickerDialog(a,
                        new android.app.TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(android.widget.TimePicker p, int hour, int min) {
                                a.prefs().edit().putInt("reminder_hour", hour)
                                        .putInt("reminder_min", min).apply();
                                remTime.setText("提醒时间 " + a.reminderTimeStr());
                                if (a.prefs().getBoolean("reminder_on", false)) {
                                    ReminderReceiver.schedule(a);
                                }
                                Toast.makeText(a, "提醒时间改成 " + a.reminderTimeStr(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }, h, m, true).show();
            }
        });
        box.addView(remTime);

        divider(box, a);

        section(box, a, "外观");
        final TextView themeRow = row(a, "外观：" + a.themeModeName(
                a.prefs().getInt("theme_mode", ThemeKit.THEME_SYSTEM)),
                new View.OnClickListener() {
                    public void onClick(View v) {
                        final AlertDialog[] holder = new AlertDialog[1];
                        LinearLayout tbox = new LinearLayout(a);
                        tbox.setOrientation(LinearLayout.VERTICAL);
                        tbox.setBackgroundColor(ThemeKit.CARD);
                        String[] names = {"跟随系统", "深色", "浅色"};
                        final int cur = a.prefs().getInt("theme_mode", ThemeKit.THEME_SYSTEM);
                        for (int k = 0; k < names.length; k++) {
                            final int mode = k;
                            TextView r = new TextView(a);
                            r.setText(names[k] + (mode == cur ? "  ✓" : ""));
                            r.setTextColor(mode == cur ? ThemeKit.ACCENT : ThemeKit.TEXT);
                            r.setTextSize(15);
                            r.setPadding((int) dp(24), (int) dp(18), (int) dp(24), (int) dp(18));
                            r.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if (holder[0] != null) holder[0].dismiss();
                                    a.setThemeMode(mode);
                                }
                            });
                            tbox.addView(r);
                        }
                        AlertDialog dlg = new AlertDialog.Builder(a)
                                .setView(tbox)
                                .setNegativeButton("取消", null)
                                .show();
                        holder[0] = dlg;
                        ThemeKit.styleDialog(dlg);
                    }
                });
        box.addView(themeRow);

        divider(box, a);

        section(box, a, "内容");
        box.addView(row(a, "埋一句给未来的自己", new View.OnClickListener() {
            public void onClick(View v) {
                a.editCapsule();
            }
        }));

        divider(box, a);

        section(box, a, "数据");
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

    private static void section(LinearLayout box, final MainActivity a, String text) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextColor(ThemeKit.ACCENT);
        t.setTextSize(11);
        t.setLetterSpacing(0.1f);
        t.setPadding((int) dp(2), (int) dp(14), 0, (int) dp(4));
        box.addView(t);
    }

    private static void divider(LinearLayout box, final MainActivity a) {
        View v = new View(a);
        v.setBackgroundColor(ThemeKit.LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) dp(1)));
        lp.topMargin = (int) dp(12);
        box.addView(v, lp);
    }

    public static void showReview(final MainActivity a) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int week = 0;
        int[] counts = new int[7];
        String[] labels = new String[7];
        String[] names = new String[]{"日", "一", "二", "三", "四", "五", "六"};
        // daily 只解析一次：7 天 + 连续天数共用这一个 Map。
        java.util.LinkedHashMap<String, Integer> daily = QuoteRepo.dailyMap(a.prefs());
        for (int i = 6; i >= 0; i--) {
            java.util.Calendar d = (java.util.Calendar) c.clone();
            d.add(java.util.Calendar.DAY_OF_YEAR, -i);
            String key = QuoteRepo.dayKey(d);
            Integer v = daily.get(key);
            int n = v == null ? 0 : v.intValue();
            week += n;
            counts[6 - i] = n;
            labels[6 - i] = i == 0 ? "今天" : "周" + names[d.get(java.util.Calendar.DAY_OF_WEEK) - 1];
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
        sum.setText("近 7 天翻了 " + week + " 篇 · 连续 " + QuoteRepo.streak(daily) + " 天");
        sum.setTextColor(ThemeKit.ACCENT);
        sum.setTextSize(15);
        sum.setPadding(0, (int) dp(10), 0, (int) dp(8));
        box.addView(sum);

        WeekChartView chart = new WeekChartView(a, counts, labels);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) dp(148));
        clp.topMargin = (int) dp(6);
        chart.setLayoutParams(clp);
        box.addView(chart);

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
