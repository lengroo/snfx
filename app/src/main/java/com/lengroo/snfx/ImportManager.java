package com.lengroo.snfx;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 导入管理：从 MainActivity 拆出（第 3 步）。 */
public final class ImportManager {

    private ImportManager() {
    }

    private static float dp(float v) {
        return ThemeKit.dp(v);
    }

    public static void importFromUri(final MainActivity a, Uri uri) {
        try {
            InputStream in = a.getContentResolver().openInputStream(uri);
            String all = a.readAll(in);
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
                Toast.makeText(a, "没读到可用文案，看看导入格式说明", Toast.LENGTH_LONG).show();
                return;
            }
            List<String> cz = new ArrayList<String>();
            List<String> ce = new ArrayList<String>();
            String[] srcZ = a.importPool();
            String[] srcE = a.importPoolEn();
            for (int i = 0; i < srcZ.length; i++) {
                cz.add(srcZ[i]);
                ce.add(i < srcE.length ? srcE[i] : "");
            }
            a.ensurePools(0);
            a.ensurePools(1);
            a.rebuildPools();
            Set<String> have = new HashSet<String>();
            for (String z : a.own()) have.add(z.trim());
            for (String z : a.hot()) have.add(z.trim());
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
                Toast.makeText(a, "这些文案已经在里面了", Toast.LENGTH_SHORT).show();
                return;
            }
            if (a.mode() == 0) {
                a.writeCustom("custom_own.txt", cz);
                a.writeCustom("custom_own_en.txt", ce);
            } else {
                a.writeCustom("custom_hot.txt", cz);
                a.writeCustom("custom_hot_en.txt", ce);
            }
            a.reloadImported();
            a.rebuildPools();
            a.showNext();
            Toast.makeText(a, "已导入 " + added + " 条到" + (a.mode() == 0 ? "原创池" : "热门池"),
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "导入失败：" + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    public static void pickTxt(final MainActivity a) {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/plain");
            a.startActivityForResult(i, MainActivity.REQ_IMPORT);
        } catch (Throwable t) {
            try {
                Intent i2 = new Intent(Intent.ACTION_GET_CONTENT);
                i2.setType("text/plain");
                a.startActivityForResult(i2, MainActivity.REQ_IMPORT);
            } catch (Throwable t2) {
                Toast.makeText(a, "这台设备没有可用的文件选择器", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void showImportHelp(final MainActivity a) {
        String NL = String.valueOf((char) 10);
        String head = "导入你自己的 txt 到" + (a.mode() == 0 ? "原创池" : "热门池") + "（当前这个池子）";
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
        TextView tip = new TextView(a);
        tip.setText(sb.toString());
        tip.setTextColor(ThemeKit.TEXT);
        tip.setTextSize(13);
        tip.setLineSpacing(dp(5), 1f);
        tip.setPadding((int) dp(20), (int) dp(16), (int) dp(20), (int) dp(8));
        TextView dlgTitle = new TextView(a);
        dlgTitle.setText("导入 txt");
        dlgTitle.setTextColor(ThemeKit.TEXT);
        dlgTitle.setTextSize(17);
        dlgTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        dlgTitle.setPadding((int) dp(20), (int) dp(18), (int) dp(20), 0);

        ScrollView sv = new ScrollView(a);
        sv.addView(tip);

        LinearLayout dlgBox = new LinearLayout(a);
        dlgBox.setOrientation(LinearLayout.VERTICAL);
        dlgBox.addView(dlgTitle);
        dlgBox.addView(sv);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(dlgBox)
                .setPositiveButton("选择文件", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        pickTxt(a);
                    }
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("管理已导入", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        showImportManager(a);
                    }
                })
                .show();
        ThemeKit.styleDialog(dlg);
    }

    public static void deleteImported(final MainActivity a, int pos) {
        String[] src = a.importPool();
        String[] srcEn = a.importPoolEn();
        if (pos < 0 || pos >= src.length) return;
        int baseLen = (a.basePool()).length;
        int globalIdx = baseLen + pos;
        List<String> z = new ArrayList<String>();
        List<String> e = new ArrayList<String>();
        for (int i = 0; i < src.length; i++) {
            if (i == pos) continue;
            z.add(src[i]);
            e.add(i < srcEn.length ? srcEn[i] : "");
        }
        try {
            if (a.mode() == 0) {
                a.writeCustom("custom_own.txt", z);
                a.writeCustom("custom_own_en.txt", e);
            } else {
                a.writeCustom("custom_hot.txt", z);
                a.writeCustom("custom_hot_en.txt", e);
            }
        } catch (Throwable t) {
            Toast.makeText(a, "删除失败", Toast.LENGTH_SHORT).show();
            return;
        }
        a.shiftAllSetsAfterDelete(globalIdx);
        a.persistAllSets();
        a.reloadImported();
        a.rebuildPools();
        if (a.currentIndex() > globalIdx) a.setCurrentIndex(a.currentIndex() - 1);
        else if (a.currentIndex() == globalIdx) a.setCurrentIndex(Math.max(0, globalIdx - 1));
        a.renderIndex(a.currentIndex());
        Toast.makeText(a, "已删除这条导入的文案", Toast.LENGTH_SHORT).show();
    }

    public static void clearImported(final MainActivity a) {
        int baseLen = (a.basePool()).length;
        try {
            if (a.mode() == 0) {
                a.writeCustom("custom_own.txt", new ArrayList<String>());
                a.writeCustom("custom_own_en.txt", new ArrayList<String>());
            } else {
                a.writeCustom("custom_hot.txt", new ArrayList<String>());
                a.writeCustom("custom_hot_en.txt", new ArrayList<String>());
            }
        } catch (Throwable t) {
            Toast.makeText(a, "清空失败", Toast.LENGTH_SHORT).show();
            return;
        }
        a.removeFromAllSets(baseLen);
        a.persistAllSets();
        a.reloadImported();
        a.rebuildPools();
        a.setCurrentIndex(0);
        a.renderIndex(0);
        Toast.makeText(a, "已清空导入的文案", Toast.LENGTH_SHORT).show();
    }

    public static void showImportManager(final MainActivity a) {
        final String[] src = a.importPool();
        if (src.length == 0) {
            Toast.makeText(a, "还没有导入过文案", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> items = new ArrayList<String>();
        for (int i = 0; i < src.length; i++) {
            String t = src[i].replace(String.valueOf((char) 10), " ").trim();
            if (t.length() > 26) t = t.substring(0, 26) + "…";
            items.add((i + 1) + ".  " + t);
        }
        ArrayAdapter<String> ad = new ArrayAdapter<String>(a,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(ThemeKit.TEXT);
                tv.setTextSize(13);
                tv.setPadding((int) dp(18), (int) dp(12), (int) dp(18), (int) dp(12));
                tv.setSingleLine(true);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return tv;
            }
        };
        ListView lv = new ListView(a);
        lv.setAdapter(ad);
        lv.setBackgroundColor(ThemeKit.CARD);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setCacheColorHint(0);

        TextView title = new TextView(a);
        title.setText("我导入的 · 共 " + src.length + " 篇");
        title.setTextColor(ThemeKit.TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding((int) dp(20), (int) dp(18), (int) dp(20), (int) dp(4));

        TextView hint = new TextView(a);
        hint.setText("点开跳到那篇，长按删除这一条");
        hint.setTextColor(0xFF6E6E78);
        hint.setTextSize(11);
        hint.setPadding((int) dp(20), 0, (int) dp(20), (int) dp(8));

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.addView(title);
        box.addView(hint);
        box.addView(lv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(420)));

        final AlertDialog[] holder = new AlertDialog[1];
        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setPositiveButton("清空全部", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        confirmClearImported(a);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
        holder[0] = dlg;
        ThemeKit.styleDialog(dlg);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int baseLen = (a.basePool()).length;
                a.jumpTo(baseLen + position);
                if (holder[0] != null) holder[0].dismiss();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                deleteImported(a, position);
                if (holder[0] != null) holder[0].dismiss();
                showImportManager(a);
                return true;
            }
        });
    }

    public static void confirmClearImported(final MainActivity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(ThemeKit.CARD);
        box.setPadding((int) dp(22), (int) dp(20), (int) dp(22), (int) dp(6));

        TextView t = new TextView(a);
        t.setText("清空导入的文案？");
        t.setTextColor(ThemeKit.TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(t);

        TextView m = new TextView(a);
        m.setText("会把当前池子里你自己导入的 "
                + (a.importPool().length) + " 条全部删掉，内置文案不受影响。");
        m.setTextColor(ThemeKit.SUB);
        m.setTextSize(13);
        m.setLineSpacing(dp(5), 1f);
        m.setPadding(0, (int) dp(10), 0, 0);
        box.addView(m);

        AlertDialog dlg = new AlertDialog.Builder(a)
                .setView(box)
                .setPositiveButton("清空", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        clearImported(a);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        ThemeKit.styleDialog(dlg);
    }
}
