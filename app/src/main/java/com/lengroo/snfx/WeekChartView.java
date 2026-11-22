package com.lengroo.snfx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 回顾弹窗里的 7 天柱状图：每天一根柱子，今天那根是粉色。 */
public final class WeekChartView extends View {

    private final int[] counts; // 7 个，从 6 天前到今天
    private final String[] labels;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WeekChartView(Context c, int[] counts, String[] labels) {
        super(c);
        this.counts = counts;
        this.labels = labels;
        barPaint.setColor(ThemeKit.LINE);
        todayPaint.setColor(ThemeKit.ACCENT);
        valuePaint.setColor(ThemeKit.SUB);
        valuePaint.setTextSize(ThemeKit.dp(11));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(ThemeKit.SUB);
        labelPaint.setTextSize(ThemeKit.dp(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        todayLabelPaint.setColor(ThemeKit.ACCENT);
        todayLabelPaint.setTextSize(ThemeKit.dp(11));
        todayLabelPaint.setTextAlign(Paint.Align.CENTER);
        basePaint.setColor(ThemeKit.LINE);
        basePaint.setStrokeWidth(Math.max(1f, ThemeKit.dp(1f)));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, (int) ThemeKit.dp(148));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float top = ThemeKit.dp(20);    // 上方留给数字
        float bottom = ThemeKit.dp(22); // 下方留给星期
        float chartH = h - top - bottom;
        float baseY = top + chartH;

        int max = 1;
        for (int n : counts) max = Math.max(max, n);

        float slot = w / 7f;
        float barW = slot * 0.52f;
        float radius = ThemeKit.dp(3);

        // 底线
        canvas.drawLine(0, baseY, w, baseY, basePaint);

        for (int i = 0; i < 7; i++) {
            float cx = slot * i + slot / 2;
            float barH = chartH * counts[i] / max;
            if (counts[i] > 0 && barH < ThemeKit.dp(3)) barH = ThemeKit.dp(3);
            float left = cx - barW / 2;
            float topY = baseY - barH;
            Paint bar = i == 6 ? todayPaint : barPaint;
            if (counts[i] > 0) {
                canvas.drawRoundRect(new RectF(left, topY, left + barW, baseY), radius, radius, bar);
            } else {
                // 0 篇也留一小段底座，看得出这个位置
                canvas.drawRoundRect(new RectF(left, baseY - ThemeKit.dp(3), left + barW, baseY),
                        radius, radius, bar);
            }
            Paint value = i == 6 ? todayLabelPaint : valuePaint;
            canvas.drawText(String.valueOf(counts[i]), cx, topY - ThemeKit.dp(6), value);
            Paint label = i == 6 ? todayLabelPaint : labelPaint;
            canvas.drawText(labels[i], cx, h - ThemeKit.dp(4), label);
        }
    }
}
