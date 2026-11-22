package com.lengroo.snfx;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 把一篇文案渲染成 1080x1920 的分享图。
 *
 * 设计：深色杂志卡片。顶部一颗小心形 + 间距拉开的品牌字；正文是衬线大字、
 * 行距宽松，整块文字在卡片里垂直居中；正文后一条粉色短线，再接英文译文；
 * 底部分隔线 + 品牌与日期。背景是纵向渐变 + 右上角一团很淡的粉色光晕，
 * 外圈一根细描边，代替原来死板的实心卡片。
 */
public final class ShareCard {

    private static final int W = 1080;
    private static final int H = 1920;

    // 背景：纵向渐变 + 粉色光晕
    private static final int BG_TOP = 0xFF17171D;
    private static final int BG_BOTTOM = 0xFF0A0A0D;
    private static final int GLOW = 0xFFFF5A7A;

    private static final int FRAME = 0x14FFFFFF;   // 细描边：白色 8%
    private static final int HAIRLINE = 0x1AFFFFFF; // 底部分隔线：白色 10%
    private static final int TEXT = 0xFFF2EFEA;     // 正文：暖白
    private static final int BRAND = 0xFFD8D8E0;    // 品牌字：浅灰
    private static final int SUB = 0xFF9C9CA8;      // 英文 / 页脚：中灰
    private static final int ACCENT = 0xFFFF5A7A;   // 粉色点缀
    private static final int QUOTE_MARK = 0x38FF5A7A; // 大引号：粉色 22%

    private static final int MARGIN = 120;          // 左右边距
    private static final int FRAME_INSET = 44;      // 描边距边缘
    private static final int CONTENT_TOP = 300;     // 正文区上沿
    private static final int CONTENT_BOTTOM = 1690; // 正文区下沿

    private ShareCard() {
    }

    public static File render(File dir, String zh, String en, String footer) throws Exception {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        drawBackground(c);
        drawFrame(c);
        drawBrand(c);

        // --- 先排版，再把正文 + 英文整块垂直居中 ---
        ZhStyle zhStyle = pickZhStyle(zh);
        List<String> zhLines = wrap(zh, zhStyle.paint, W - MARGIN * 2);
        float zhH = zhLines.isEmpty() ? 0 : zhLines.size() * zhStyle.lineH;

        Paint enPaint = enPaint();
        List<String> enLines = wrap(en, enPaint, W - MARGIN * 2);
        float enH = enLines.isEmpty() ? 0 : enLines.size() * 50;

        boolean hasEn = !enLines.isEmpty();
        float gapRule = hasEn ? 44 + 5 + 36 : 0; // 正文到短线 + 短线到英文
        float blockH = zhH + gapRule + enH;
        float top = CONTENT_TOP + (CONTENT_BOTTOM - CONTENT_TOP - blockH) / 2;

        drawQuoteMark(c, top);
        float y = top;
        y = drawLines(c, zhLines, zhStyle.paint, MARGIN, y, zhStyle.lineH);
        if (hasEn) {
            y += 44;
            Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
            rule.setColor(ACCENT);
            c.drawRoundRect(new RectF(MARGIN, y, MARGIN + 72, y + 5), 2.5f, 2.5f, rule);
            y += 5 + 36;
            y = drawLines(c, enLines, enPaint, MARGIN, y, 50);
        }

        drawFooter(c, footer);

        File out = new File(dir, "share.png");
        FileOutputStream fos = new FileOutputStream(out);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        bmp.recycle();
        return out;
    }

    /** 纵向渐变底 + 右上角一团淡粉色光晕。 */
    private static void drawBackground(Canvas c) {
        Paint bg = new Paint();
        bg.setShader(new LinearGradient(0, 0, 0, H, BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, bg);

        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new RadialGradient(W * 0.78f, 240, 560,
                Color.argb(26, 255, 90, 122), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, glow);
    }

    /** 外圈细描边，圆角。 */
    private static void drawFrame(Canvas c) {
        Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(2);
        frame.setColor(FRAME);
        c.drawRoundRect(new RectF(FRAME_INSET, FRAME_INSET, W - FRAME_INSET, H - FRAME_INSET),
                48, 48, frame);
    }

    /** 顶部：左边小心形 + 间距拉开的「每日一遍」，右边日期。 */
    private static void drawBrand(Canvas c) {
        float cy = 168; // 基线
        drawHeart(c, MARGIN, cy - 26, 30, ACCENT);

        Paint brand = new Paint(Paint.ANTI_ALIAS_FLAG);
        brand.setColor(BRAND);
        brand.setTextSize(30);
        brand.setLetterSpacing(0.42f);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        c.drawText("每日一遍", MARGIN + 46, cy, brand);

        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(SUB);
        datePaint.setTextSize(26);
        datePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        String date = new SimpleDateFormat("yyyy.MM.dd", Locale.CHINA).format(new Date());
        c.drawText(date, W - MARGIN - datePaint.measureText(date), cy, datePaint);
    }

    /** 正文左上角一枚很大的淡粉色引号，衬在第一行后面。 */
    private static void drawQuoteMark(Canvas c, float quoteTop) {
        Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mark.setColor(QUOTE_MARK);
        mark.setTextSize(300);
        mark.setTypeface(Typeface.create("serif", Typeface.BOLD));
        c.drawText("\u201C", 92, quoteTop + 40, mark);
    }

    /** 底部：分隔线 + 品牌页脚。 */
    private static void drawFooter(Canvas c, String footer) {
        Paint line = new Paint();
        line.setColor(HAIRLINE);
        c.drawRect(MARGIN, 1760, W - MARGIN, 1761.5f, line);

        Paint foot = new Paint(Paint.ANTI_ALIAS_FLAG);
        foot.setColor(SUB);
        foot.setTextSize(26);
        foot.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        if (footer != null) c.drawText(footer, MARGIN, 1822, foot);
    }

    /** 在 (x, top) 画一颗实心小心形，size 是外框边长。 */
    private static void drawHeart(Canvas c, float x, float y, float size, int color) {
        Path p = new Path();
        float s = size / 100f;
        p.moveTo(x + 50 * s, y + 88 * s);
        p.cubicTo(x + 10 * s, y + 55 * s, x + 5 * s, y + 30 * s, x + 20 * s, y + 12 * s);
        p.cubicTo(x + 35 * s, y + 2 * s, x + 50 * s, y + 16 * s, x + 50 * s, y + 16 * s);
        p.cubicTo(x + 50 * s, y + 16 * s, x + 65 * s, y + 2 * s, x + 80 * s, y + 12 * s);
        p.cubicTo(x + 95 * s, y + 30 * s, x + 90 * s, y + 55 * s, x + 50 * s, y + 88 * s);
        p.close();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawPath(p, paint);
    }

    private static Paint enPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(SUB);
        p.setTextSize(30);
        p.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        return p;
    }

    /** 正文按长度自适应：太长就缩小一档字号。 */
    private static ZhStyle pickZhStyle(String zh) {
        ZhStyle s54 = new ZhStyle(54, 88);
        if (wrap(zh, s54.paint, W - MARGIN * 2).size() <= 9) return s54;
        ZhStyle s48 = new ZhStyle(48, 78);
        if (wrap(zh, s48.paint, W - MARGIN * 2).size() <= 11) return s48;
        return new ZhStyle(44, 72);
    }

    private static final class ZhStyle {
        final Paint paint;
        final float lineH;

        ZhStyle(float size, float lineH) {
            this.lineH = lineH;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(TEXT);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        }
    }

    /** 先断行，返回行列表（不画）。 */
    private static List<String> wrap(String text, Paint p, float maxW) {
        List<String> lines = new ArrayList<String>();
        if (text == null) return lines;
        List<String> tokens = tokenize(text);
        StringBuilder line = new StringBuilder();
        for (String t : tokens) {
            String probe = line.toString() + t;
            if (p.measureText(probe) > maxW && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
                if (t.equals(" ")) continue;
            }
            line.append(t);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    /** 按基线画多行，返回下一行的基线 y。 */
    private static float drawLines(Canvas c, List<String> lines, Paint p, float x, float y, float lineH) {
        for (String line : lines) {
            c.drawText(line, x, y, p);
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
