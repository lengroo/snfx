package com.lengroo.snfx;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** 把一篇文案渲染成 1080x1920 的分享图。 */
public final class ShareCard {

    private static final int W = 1080;
    private static final int H = 1920;
    private static final int BG = 0xFF0B0B0D;
    private static final int CARD = 0xFF15151A;
    private static final int TEXT = 0xFFEDEAE6;
    private static final int SUB = 0xFF8A8A94;
    private static final int ACCENT = 0xFFFF5A7A;

    private ShareCard() {
    }

    public static File render(File dir, String zh, String en, String footer) throws Exception {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(BG);

        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(CARD);
        RectF card = new RectF(56, 200, W - 56, H - 220);
        c.drawRoundRect(card, 40, 40, cardPaint);

        Paint brand = new Paint(Paint.ANTI_ALIAS_FLAG);
        brand.setColor(ACCENT);
        brand.setTextSize(34);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        c.drawText("每日一遍", 56, 140, brand);

        Paint zhPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        zhPaint.setColor(TEXT);
        zhPaint.setTextSize(48);
        zhPaint.setTypeface(Typeface.create("serif", Typeface.NORMAL));

        Paint enPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        enPaint.setColor(SUB);
        enPaint.setTextSize(34);
        enPaint.setTypeface(Typeface.create("serif", Typeface.NORMAL));

        float x = 104;
        float maxW = W - 208;
        float y = 300;
        float lineH = 74;
        y = drawWrapped(c, zh, zhPaint, x, maxW, y, lineH, H - 420);

        y += 34;
        Paint divider = new Paint();
        divider.setColor(0xFF2A2A32);
        c.drawRect(x, y, W - 104, y + 2, divider);
        y += 46;

        drawWrapped(c, en, enPaint, x, maxW, y, 52, H - 300);

        Paint foot = new Paint(Paint.ANTI_ALIAS_FLAG);
        foot.setColor(SUB);
        foot.setTextSize(28);
        foot.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        c.drawText(footer, 56, H - 110, foot);

        File out = new File(dir, "share.png");
        FileOutputStream fos = new FileOutputStream(out);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        bmp.recycle();
        return out;
    }

    /** 中文按字断行，英文按词断行；超出高度就停。 */
    private static float drawWrapped(Canvas c, String text, Paint p, float x, float maxW,
                                     float y, float lineH, float limitY) {
        if (text == null) return y;
        List<String> tokens = tokenize(text);
        StringBuilder line = new StringBuilder();
        for (String t : tokens) {
            String probe = line.toString() + t;
            if (p.measureText(probe) > maxW && line.length() > 0) {
                c.drawText(line.toString(), x, y, p);
                y += lineH;
                if (y > limitY) return y;
                line.setLength(0);
                if (t.equals(" ")) continue;
            }
            line.append(t);
        }
        if (line.length() > 0) {
            c.drawText(line.toString(), x, y, p);
            y += lineH;
        }
        return y;
    }

    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<String>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            boolean cjk = ch >= 0x4E00 && ch <= 0x9FA5;
            if (cjk || ch == 0xFF0C || ch == 0x3002 || ch == 0xFF01 || ch == 0xFF1F
                    || ch == 0xFF1B || ch == 0xFF1A || ch == 0x201C || ch == 0x201D) {
                if (word.length() > 0) {
                    out.add(word.toString());
                    word.setLength(0);
                }
                out.add(String.valueOf(ch));
            } else if (ch == ' ') {
                if (word.length() > 0) {
                    out.add(word.toString());
                    word.setLength(0);
                }
                out.add(" ");
            } else {
                word.append(ch);
            }
        }
        if (word.length() > 0) out.add(word.toString());
        return out;
    }
}
