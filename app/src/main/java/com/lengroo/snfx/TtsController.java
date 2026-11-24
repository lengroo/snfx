package com.lengroo.snfx;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** 朗读（TTS）与排版：从 MainActivity 拆出（第 2 步）。TTS 状态随方法一起搬过来。 */
public final class TtsController {

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean speakingNow = false;
    private boolean speakEnglish = false;
    private boolean continuousActive = false;
    private int textSizeLevel = 1;
    private int lineLevel = 1;
    private AlertDialog typeDialog;

    private static float dp(float v) {
        return ThemeKit.dp(v);
    }

    public boolean isSpeakingNow() {
        return speakingNow;
    }

    public void setSpeakEnglish(boolean en) {
        speakEnglish = en;
    }

    public void setTextSizeLevel(int level) {
        textSizeLevel = level;
    }

    public void setLineLevel(int level) {
        lineLevel = level;
    }

    /** 释放 TTS 引擎（MainActivity.onDestroy 调用）。 */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    public void toggleSpeak(final MainActivity a) {
        TextView body = a.bodyView();
        if (speakingNow) {
            stopSpeak(a);
            Toast.makeText(a, "已停止朗读", Toast.LENGTH_SHORT).show();
            return;
        }
        if (body.getText().toString().trim().length() == 0) return;
        if (tts != null && ttsReady) {
            speakNow(a);
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
        tts = new TextToSpeech(a, new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            a.runOnUiThread(new Runnable() {
                                public void run() {
                                    speakingNow = false;
                                    a.setSpeakLabel(speakingNow);
                                    if (continuousActive && a.prefs().getBoolean("continuous", false)) {
                                        final android.os.Handler h = new android.os.Handler(
                                                android.os.Looper.getMainLooper());
                                        h.postDelayed(new Runnable() {
                                            public void run() {
                                                if (!continuousActive) return;
                                                a.showNext();
                                                h.postDelayed(new Runnable() {
                                                    public void run() {
                                                        if (continuousActive && !speakingNow) toggleSpeak(a);
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
                            a.runOnUiThread(new Runnable() {
                                public void run() {
                                    speakingNow = false;
                                    a.setSpeakLabel(speakingNow);
                                }
                            });
                        }
                    });
                    ttsReady = true;
                    a.runOnUiThread(new Runnable() {
                        public void run() {
                            speakNow(a);
                        }
                    });
                } else {
                    a.runOnUiThread(new Runnable() {
                        public void run() {
                            showTtsHelp(a, "系统里没有可用的语音引擎（TTS），多半是没装或没启用。");
                        }
                    });
                }
            }
        });
    }

    public void showTtsHelp(final MainActivity a, String why) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(a);
        t.setText("语音引擎初始化失败");
        t.setTextColor(ThemeKit.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView m = new TextView(a);
        m.setText(why + String.valueOf((char) 10) + String.valueOf((char) 10) +
                "可以到系统「设置 → 无障碍 → 文字转语音(TTS)」里安装 / 启用一个引擎并选中文，然后回来点「重试」。");
        m.setTextColor(ThemeKit.SUB);
        m.setTextSize(13);
        m.setLineSpacing(dp(5), 1f);
        m.setPadding(0, (int) dp(10), 0, (int) dp(6));
        box.addView(m);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setPositiveButton("打开语音设置", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        openTtsSettings(a);
                    }
                })
                .setNeutralButton("重试", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        tts = null;
                        ttsReady = false;
                        toggleSpeak(a);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
        ThemeKit.styleDialog(dlg);
    }

    public void openTtsSettings(final MainActivity a) {
        String[] actions = new String[]{
                "com.android.settings.TTS_SETTINGS",
                "com.android.settings.Settings$TextToSpeechSettingsActivity",
                android.provider.Settings.ACTION_SETTINGS
        };
        for (int i = 0; i < actions.length; i++) {
            try {
                a.startActivity(new Intent(actions[i]));
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    public void applyTypography(final MainActivity a) {
        TextView body = a.bodyView();
        TextView trans = a.transView();
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

    public void showTypography(final MainActivity a) {
        if (typeDialog != null && typeDialog.isShowing()) {
            typeDialog.dismiss();
        }
        typeDialog = null;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(6));

        TextView title = new TextView(a);
        title.setText("排版");
        title.setTextColor(ThemeKit.TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(0, 0, 0, (int) dp(12));
        box.addView(title);

        box.addView(UiBuilder.labelRow(a, "字号"));
        final String[] sizeNames = new String[]{"小", "标准", "大", "特大"};
        LinearLayout sizeRow = new LinearLayout(a);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < sizeNames.length; i++) {
            final int which = i;
            sizeRow.addView(UiBuilder.chip(a, sizeNames[i], textSizeLevel == i, new View.OnClickListener() {
                public void onClick(View v) {
                    textSizeLevel = which;
                    a.prefs().edit().putInt("text_size", textSizeLevel).apply();
                    applyTypography(a);
                    showTypography(a);
                }
            }), UiBuilder.chipParams(i == 0 ? 0 : 8));
        }
        box.addView(sizeRow);

        TextView gap = new TextView(a);
        gap.setText("");
        gap.setHeight((int) dp(10));
        box.addView(gap);

        box.addView(UiBuilder.labelRow(a, "行距"));
        final String[] lineNames = new String[]{"紧凑", "标准", "宽松"};
        LinearLayout lineRow = new LinearLayout(a);
        lineRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < lineNames.length; i++) {
            final int which = i;
            lineRow.addView(UiBuilder.chip(a, lineNames[i], lineLevel == i, new View.OnClickListener() {
                public void onClick(View v) {
                    lineLevel = which;
                    a.prefs().edit().putInt("line_space", lineLevel).apply();
                    applyTypography(a);
                    showTypography(a);
                }
            }), UiBuilder.chipParams(i == 0 ? 0 : 8));
        }
        box.addView(lineRow);

        TextView tip = new TextView(a);
        tip.setText("中文与英文会一起缩放，设置会记住。");
        tip.setTextColor(ThemeKit.SUB);
        tip.setTextSize(11);
        tip.setPadding(0, (int) dp(14), 0, 0);
        box.addView(tip);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        typeDialog = dlg;
        ThemeKit.styleDialog(dlg);
    }

    public void speakNow(final MainActivity a) {
        TextView body = a.bodyView();
        TextView trans = a.transView();
        if (tts == null || !ttsReady) return;
        String text = speakEnglish
                ? (trans != null ? trans.getText().toString().trim() : "")
                : body.getText().toString().trim();
        if (text.length() == 0) {
            Toast.makeText(a, speakEnglish ? "这篇没有英文译文" : "没有可朗读的文字",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        int r = tts.setLanguage(speakEnglish ? Locale.US : Locale.SIMPLIFIED_CHINESE);
        if (!speakEnglish && (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)) {
            r = tts.setLanguage(Locale.CHINESE);
        }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            showTtsHelp(a, speakEnglish ? "语音引擎里没有英文语音包。" : "语音引擎里没有中文语音包。");
            return;
        }
        tts.setSpeechRate(a.prefs().getInt("speak_rate", 95) / 100f);
        speakingNow = true;
        continuousActive = true;
        a.setSpeakLabel(speakingNow);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snfx");
        Toast.makeText(a, "朗读中（" + (speakEnglish ? "英文" : "中文") + "）· 点底部「停止」可结束",
                Toast.LENGTH_SHORT).show();
    }

    public void showSpeakLang(final MainActivity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(6));

        TextView title = new TextView(a);
        title.setText("朗读");
        title.setTextColor(ThemeKit.TEXT);
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
            TextView row = new TextView(a);
            row.setText((on ? "✓  " : "     ") + names[i]);
            row.setTextColor(on ? ThemeKit.ACCENT : ThemeKit.TEXT);
            row.setTextSize(15);
            row.setPadding((int) dp(8), (int) dp(16), (int) dp(8), (int) dp(16));
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (holder[0] != null) holder[0].dismiss();
                    if (speakingNow) stopSpeak(a);
                    speakEnglish = which == 1;
                    a.prefs().edit().putBoolean("speak_en", speakEnglish).apply();
                    Toast.makeText(a,
                            "朗读语言：" + (speakEnglish ? "英文" : "中文"), Toast.LENGTH_SHORT).show();
                }
            });
            box.addView(row);
        }

        View line = new View(a);
        line.setBackgroundColor(ThemeKit.LINE);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) dp(1)));
        lineLp.topMargin = (int) dp(12);
        lineLp.bottomMargin = (int) dp(4);
        box.addView(line, lineLp);

        final TextView rateLabel = new TextView(a);
        rateLabel.setText("语速 " + String.format("%.2f", a.prefs().getInt("speak_rate", 95) / 100f));
        rateLabel.setTextColor(ThemeKit.SUB);
        rateLabel.setTextSize(12);
        rateLabel.setPadding(0, (int) dp(10), 0, 0);
        box.addView(rateLabel);

        android.widget.SeekBar bar = new android.widget.SeekBar(a);
        bar.setMax(60);
        bar.setProgress(Math.max(0, Math.min(60, a.prefs().getInt("speak_rate", 95) - 70)));
        bar.setPadding(0, (int) dp(4), 0, (int) dp(2));
        bar.setTag(rateLabel);
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar sb, int p, boolean fromUser) {
                int rate = 70 + p;
                a.prefs().edit().putInt("speak_rate", rate).apply();
                ((TextView) sb.getTag()).setText("语速 " + String.format("%.2f", rate / 100f));
            }

            public void onStartTrackingTouch(android.widget.SeekBar sb) {
            }

            public void onStopTrackingTouch(android.widget.SeekBar sb) {
            }
        });
        box.addView(bar);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
        holder[0] = dlg;
        ThemeKit.styleDialog(dlg);
    }

    public void stopSpeak(final MainActivity a) {
        speakingNow = false;
        continuousActive = false;
        a.setSpeakLabel(speakingNow);
        if (tts != null) tts.stop();
    }
}
