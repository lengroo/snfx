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
 * 把一篇文案渲染成 1080x1920 的分享图，九套模板。
 *
 * 版式（九套共用）：顶部一颗小心形 + 间距拉开的品牌字，右边日期；正文是衬线大字、
 * 行距宽松，整块文字在卡片里垂直居中；正文后一条短线，再接英文译文；
 * 底部分隔线 + 品牌页脚。外圈一根细描边。
 * 深色杂志：纵向渐变深底 + 右上角淡粉色光晕。
 * 浅色纸感：暖米色纸底 + 右上角淡玫红光晕，墨色正文。
 * 红橙黄绿蓝靛紫：同色系渐变底 + 淡色光晕，白色正文（黄色用墨色正文）。
 */
public final class ShareCard {

    /** 模板：深色杂志。 */
    public static final int TEMPLATE_DARK = 0;
    /** 模板：浅色纸感。 */
    public static final int TEMPLATE_LIGHT = 1;
    /** 模板：红。 */
    public static final int TEMPLATE_RED = 2;
    /** 模板：橙。 */
    public static final int TEMPLATE_ORANGE = 3;
    /** 模板：黄。 */
    public static final int TEMPLATE_YELLOW = 4;
    /** 模板：绿。 */
    public static final int TEMPLATE_GREEN = 5;
    /** 模板：蓝。 */
    public static final int TEMPLATE_BLUE = 6;
    /** 模板：靛。 */
    public static final int TEMPLATE_INDIGO = 7;
    /** 模板：紫。 */
    public static final int TEMPLATE_VIOLET = 8;

    private static final int W = 1080;
    private static final int H = 1920;

    private static final int MARGIN = 120;          // 左右边距
    private static final int FRAME_INSET = 44;      // 描边距边缘
    private static final int CONTENT_TOP = 300;     // 正文区上沿
    private static final int CONTENT_BOTTOM = 1690; // 正文区下沿

    /** 一套模板的全部配色。 */
    private static final class Palette {
        final int bgTop, bgBottom;
        final int glowColor, glowAlpha;
        final int frame, hairline, text, brand, sub, accent, quoteMark;

        Palette(int bgTop, int bgBottom, int glowColor, int glowAlpha,
                int frame, int hairline, int text, int brand, int sub,
                int accent, int quoteMark) {
            this.bgTop = bgTop;
            this.bgBottom = bgBottom;
            this.glowColor = glowColor;
            this.glowAlpha = glowAlpha;
            this.frame = frame;
            this.hairline = hairline;
            this.text = text;
            this.brand = brand;
            this.sub = sub;
            this.accent = accent;
            this.quoteMark = quoteMark;
        }
    }

    private static final Palette DARK = new Palette(
            0xFF17171D, 0xFF0A0A0D,             // 背景渐变
            0xFFFF5A7A, 26,                     // 光晕：粉色
            0x14FFFFFF, 0x1AFFFFFF,             // 描边 / 分隔线：白色 8% / 10%
            0xFFF2EFEA, 0xFFD8D8E0, 0xFF9C9CA8, // 正文暖白 / 品牌浅灰 / 英文页脚中灰
            0xFFFF5A7A, 0x38FF5A7A);            // 点缀粉 / 大引号粉 22%

    private static final Palette LIGHT = new Palette(
            0xFFF7F2E8, 0xFFEEE7D8,             // 背景渐变：暖米纸
            0xFFD84069, 22,                     // 光晕：玫红
            0x1A000000, 0x1F000000,             // 描边 / 分隔线：墨色 10% / 12%
            0xFF241F19, 0xFF3A342C, 0xFF8A8175, // 正文墨色 / 品牌深褐 / 英文页脚暖灰
            0xFFD84069, 0x2ED84069);            // 点缀玫红 / 大引号玫红 18%

    // 彩虹七色：同色系渐变底 + 淡色光晕，白色正文（黄色太亮，用墨色正文）。
    private static final Palette RED = new Palette(
            0xFFD94A4A, 0xFFB02020,
            0xFFFF8A80, 30,
            0x2EFFFFFF, 0x33FFFFFF,
            0xFFFFFFFF, 0xFFF5E6E6, 0xFFD9B8B8,
            0xFFFFFFFF, 0x3DFFFFFF);

    private static final Palette ORANGE = new Palette(
            0xFFF08C46, 0xFFDD5E00,
            0xFFFFCC80, 30,
            0x2EFFFFFF, 0x33FFFFFF,
            0xFFFFFFFF, 0xFFFFF0E0, 0xFFE8C4A8,
            0xFFFFFFFF, 0x3DFFFFFF);

    private static final Palette YELLOW = new Palette(
            0xFFF6C945, 0xFFE8A90C,
            0xFFFFF176, 28,
            0x1A000000, 0x1F000000,
            0xFF241F19, 0xFF3A342C, 0xFF7A6A2A,
            0xFF8A6D00, 0x2E8A6D00);

    private static final Palette GREEN = new Palette(
            0xFF5CAF78, 0xFF2E7D46,
            0xFFA5D6A7, 30,
            0x2EFFFFFF, 0x33FFFFFF,
            0xFFFFFFFF, 0xFFE8F5E9, 0xFFB7D9BE,
            0xFFFFFFFF, 0x3DFFFFFF);

    private static final Palette BLUE = new Palette(
            0xFF5B8DEF, 0xFF1565C0,
            0xFF90CAF9, 30,
            0x2EFFFFFF, 0x33FFFFFF,
            0xFFFFFFFF, 0xFFE3F2FD, 0xFFB8D0F0,
            0xFFFFFFFF, 0x3DFFFFFF);

    private static final Palette INDIGO = new Palette(
            0xFF6C72DC, 0xFF3949AB,
            0xFF9FA8DA, 30,
            0x2EFFFFFF, 0x33FFFFFF,
            0xFFFFFFFF, 0xFFE8EAF6, 0xFFB9C0E8,
            0xFFFFFFFF, 0x3DFFFFFF);

    private static final Palette VIOLET = new Palette(
            0xFF9B6BD6, 0xFF6A1B9A,
            0xFFCE93D8, 30,
            0x2EFFFFFF, 0x33FFFFFF,
            0xFFFFFFFF, 0xFFF3E5F5, 0xFFD0B4DE,
            0xFFFFFFFF, 0x3DFFFFFF);

    private static Palette paletteFor(int template) {
        switch (template) {
            case TEMPLATE_LIGHT: return LIGHT;
            case TEMPLATE_RED: return RED;
            case TEMPLATE_ORANGE: return ORANGE;
            case TEMPLATE_YELLOW: return YELLOW;
            case TEMPLATE_GREEN: return GREEN;
            case TEMPLATE_BLUE: return BLUE;
            case TEMPLATE_INDIGO: return INDIGO;
            case TEMPLATE_VIOLET: return VIOLET;
            default: return DARK;
        }
    }

    private ShareCard() {
    }

    /** 默认用深色杂志模板。 */
    public static File render(File dir, String zh, String en, String footer) throws Exception {
        return render(dir, zh, en, footer, TEMPLATE_DARK);
    }

    public static File render(File dir, String zh, String en, String footer, int template) throws Exception {
        Palette p = paletteFor(template);
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        drawBackground(c, p);
        drawFrame(c, p);
        drawBrand(c, p);

        // --- 先排版，再把正文 + 英文整块垂直居中 ---
        ZhStyle zhStyle = pickZhStyle(zh, p);
        List<String> zhLines = wrap(zh, zhStyle.paint, W - MARGIN * 2);
        float zhH = zhLines.isEmpty() ? 0 : zhLines.size() * zhStyle.lineH;

        Paint enPaint = enPaint(p);
        List<String> enLines = wrap(en, enPaint, W - MARGIN * 2);
        float enH = enLines.isEmpty() ? 0 : enLines.size() * 50;

        boolean hasEn = !enLines.isEmpty();
        float gapRule = hasEn ? 44 + 5 + 36 : 0; // 正文到短线 + 短线到英文
        float blockH = zhH + gapRule + enH;
        float top = CONTENT_TOP + (CONTENT_BOTTOM - CONTENT_TOP - blockH) / 2;

        drawQuoteMark(c, p, top);
        float y = top;
        y = drawLines(c, zhLines, zhStyle.paint, MARGIN, y, zhStyle.lineH);
        if (hasEn) {
            y += 44;
            Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
            rule.setColor(p.accent);
            c.drawRoundRect(new RectF(MARGIN, y, MARGIN + 72, y + 5), 2.5f, 2.5f, rule);
            y += 5 + 36;
            y = drawLines(c, enLines, enPaint, MARGIN, y, 50);
        }

        drawFooter(c, p, footer);

        File out = new File(dir, "share.png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return out;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Throwable ignored) {
                }
            }
            bmp.recycle();
        }
    }

    /** 纵向渐变底 + 右上角一团淡色光晕。 */
    private static void drawBackground(Canvas c, Palette p) {
        Paint bg = new Paint();
        bg.setShader(new LinearGradient(0, 0, 0, H, p.bgTop, p.bgBottom, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, bg);

        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new RadialGradient(W * 0.78f, 240, 560,
                Color.argb(p.glowAlpha, Color.red(p.glowColor),
                        Color.green(p.glowColor), Color.blue(p.glowColor)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, glow);
    }

    /** 外圈细描边，圆角。 */
    private static void drawFrame(Canvas c, Palette p) {
        Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(2);
        frame.setColor(p.frame);
        c.drawRoundRect(new RectF(FRAME_INSET, FRAME_INSET, W - FRAME_INSET, H - FRAME_INSET),
                48, 48, frame);
    }

    /** 顶部：左边小心形 + 间距拉开的品牌字，右边日期。 */
    private static void drawBrand(Canvas c, Palette p) {
        float cy = 168; // 基线
        drawHeart(c, MARGIN, cy - 26, 30, p.accent);

        Paint brand = new Paint(Paint.ANTI_ALIAS_FLAG);
        brand.setColor(p.brand);
        brand.setTextSize(30);
        brand.setLetterSpacing(0.42f);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        c.drawText("每日一遍", MARGIN + 46, cy, brand);

        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(p.sub);
        datePaint.setTextSize(26);
        datePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        String date = new SimpleDateFormat("yyyy.MM.dd", Locale.CHINA).format(new Date());
        c.drawText(date, W - MARGIN - datePaint.measureText(date), cy, datePaint);
    }

    /** 正文左上角一枚很大的淡色引号，衬在第一行后面。 */
    private static void drawQuoteMark(Canvas c, Palette p, float quoteTop) {
        Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mark.setColor(p.quoteMark);
        mark.setTextSize(300);
        mark.setTypeface(Typeface.create("serif", Typeface.BOLD));
        c.drawText("\u201C", 92, quoteTop + 40, mark);
    }

    /** 底部：分隔线 + 品牌页脚。 */
    private static void drawFooter(Canvas c, Palette p, String footer) {
        Paint line = new Paint();
        line.setColor(p.hairline);
        c.drawRect(MARGIN, 1760, W - MARGIN, 1761.5f, line);

        Paint foot = new Paint(Paint.ANTI_ALIAS_FLAG);
        foot.setColor(p.sub);
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

    private static Paint enPaint(Palette p) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(p.sub);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        return paint;
    }

    /** 正文按长度自适应：太长就缩小一档字号。 */
    private static ZhStyle pickZhStyle(String zh, Palette p) {
        ZhStyle s54 = new ZhStyle(54, 88, p);
        if (wrap(zh, s54.paint, W - MARGIN * 2).size() <= 9) return s54;
        ZhStyle s48 = new ZhStyle(48, 78, p);
        if (wrap(zh, s48.paint, W - MARGIN * 2).size() <= 11) return s48;
        return new ZhStyle(44, 72, p);
    }

    private static final class ZhStyle {
        final Paint paint;
        final float lineH;

        ZhStyle(float size, float lineH, Palette p) {
            this.lineH = lineH;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(p.text);
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
