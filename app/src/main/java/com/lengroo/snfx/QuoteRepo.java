package com.lengroo.snfx;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 文案与本地记录：assets 读取、已看/收藏序列化、每日翻篇统计。 */
public final class QuoteRepo {

    public static final String SEP = "\n@@@\n";

    private QuoteRepo() {
    }

    public static String[] load(Context c, String name) {
        InputStream in = null;
        try {
            in = c.getAssets().open(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            String all = new String(bos.toByteArray(), "UTF-8").trim();
            if (all.length() > 0) return all.split(SEP);
        } catch (Throwable t) {
            return new String[0];
        } finally {
            if (in != null) try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
        return new String[0];
    }

    public static void readSeen(SharedPreferences prefs, String key, Set<Integer> into) {
        into.clear();
        String s = prefs.getString(key, "");
        if (s.length() == 0) return;
        String[] parts = s.split(",");
        for (int i = 0; i < parts.length; i++) {
            try {
                into.add(Integer.parseInt(parts[i].trim()));
            } catch (Throwable ignored) {
            }
        }
    }

    public static String writeSeen(Set<Integer> set) {
        StringBuilder sb = new StringBuilder();
        for (Integer v : set) {
            if (sb.length() > 0) sb.append(',');
            sb.append(v.intValue());
        }
        return sb.toString();
    }

    public static String dayKey() {
        return dayKey(java.util.Calendar.getInstance());
    }

    /** 把 "daily"（yyyyMMdd:count;…）一次解析成 Map；调用方复用它，别反复 split。 */
    public static LinkedHashMap<String, Integer> dailyMap(SharedPreferences prefs) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        String raw = prefs.getString("daily", "");
        for (String part : raw.split(";")) {
            int c = part.indexOf(':');
            if (c <= 0) continue;
            try {
                map.put(part.substring(0, c), Integer.parseInt(part.substring(c + 1)));
            } catch (Throwable ignored) {
            }
        }
        return map;
    }

    public static String writeDaily(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    public static String dayKey(java.util.Calendar d) {
        int y = d.get(java.util.Calendar.YEAR);
        int m = d.get(java.util.Calendar.MONTH) + 1;
        int day = d.get(java.util.Calendar.DAY_OF_MONTH);
        StringBuilder sb = new StringBuilder(8);
        sb.append(y);
        if (m < 10) sb.append('0');
        sb.append(m);
        if (day < 10) sb.append('0');
        sb.append(day);
        return sb.toString();
    }

    public static void recordFlip(SharedPreferences prefs) {
        SharedPreferences.Editor ed = prefs.edit();
        recordFlipInto(prefs, ed);
        ed.apply();
    }

    /** 把今天的翻篇数 +1 后写进给定的 Editor，由调用方一次 apply（省一次落盘）。 */
    public static void recordFlipInto(SharedPreferences prefs, SharedPreferences.Editor ed) {
        LinkedHashMap<String, Integer> map = dailyMap(prefs);
        String today = dayKey();
        Integer v = map.get(today);
        map.put(today, v == null ? 1 : v + 1);
        while (map.size() > 40) {
            Iterator<String> it = map.keySet().iterator();
            it.next();
            it.remove();
        }
        ed.putString("daily", writeDaily(map));
    }

    public static int dailyCount(SharedPreferences prefs, String key) {
        Integer v = dailyMap(prefs).get(key);
        return v == null ? 0 : v.intValue();
    }

    /** 连续天数：从今天（或昨天）往前数，遇到 0 就停。 */
    public static int streak(SharedPreferences prefs) {
        return streak(dailyMap(prefs));
    }

    /** 同上，但复用调用方已经解析好的 Map（回顾弹窗一次解析，7 天 + 连续天数共用）。 */
    public static int streak(Map<String, Integer> daily) {
        int n = 0;
        java.util.Calendar c = java.util.Calendar.getInstance();
        boolean first = true;
        for (int i = 0; i < 400; i++) {
            java.util.Calendar d = (java.util.Calendar) c.clone();
            d.add(java.util.Calendar.DAY_OF_YEAR, -i);
            Integer v = daily.get(dayKey(d));
            if (v != null && v.intValue() > 0) {
                n++;
                first = false;
            } else if (first) {
                first = false;
                continue;
            } else {
                break;
            }
        }
        return n;
    }

    public static String[] loadComfort(Context c) {
        try {
            InputStream in = c.getAssets().open("comfort.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append((char) 10);
            br.close();
            return sb.toString().split("@@@");
        } catch (Throwable t) {
            return new String[]{"歇一会儿吧。"};
        }
    }
}
