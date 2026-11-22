package com.lengroo.snfx;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/** 备份：收藏、已看、次数、设置 → 一个 JSON 字符串。 */
public final class Backup {

    private Backup() {
    }

    private static JSONArray arr(Set<Integer> set) {
        JSONArray a = new JSONArray();
        for (Integer v : set) a.put(v.intValue());
        return a;
    }

    public static String toJson(SharedPreferences p, Set<Integer> seenOwn, Set<Integer> seenHot,
                                Set<Integer> favOwn, Set<Integer> favHot) {
        try {
            JSONObject o = new JSONObject();
            o.put("v", 1);
            o.put("app", "meiriyibian");
            o.put("mode", p.getInt("mode", 0));
            o.put("countOwn", p.getInt("count_own", 0));
            o.put("countHot", p.getInt("count_hot", 0));
            o.put("speakEn", p.getBoolean("speak_en", false));
            o.put("textSize", p.getInt("text_size", 1));
            o.put("lineSpace", p.getInt("line_space", 1));
            o.put("comfort", p.getBoolean("comfort_on", true));
            o.put("continuous", p.getBoolean("continuous", false));
            o.put("daily", p.getString("daily", ""));
            o.put("seenOwn", arr(seenOwn));
            o.put("seenHot", arr(seenHot));
            o.put("favOwn", arr(favOwn));
            o.put("favHot", arr(favHot));
            return o.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static void fill(Set<Integer> into, JSONArray a) {
        into.clear();
        if (a == null) return;
        for (int i = 0; i < a.length(); i++) {
            into.add(a.optInt(i));
        }
    }

    /** 返回 true 表示解析并写入成功。 */
    public static boolean apply(String json, SharedPreferences.Editor ed,
                               Set<Integer> seenOwn, Set<Integer> seenHot,
                               Set<Integer> favOwn, Set<Integer> favHot) {
        try {
            JSONObject o = new JSONObject(json);
            if (o.optInt("v", 0) != 1) return false;
            ed.putInt("mode", o.optInt("mode", 0));
            ed.putInt("count_own", o.optInt("countOwn", 0));
            ed.putInt("count_hot", o.optInt("countHot", 0));
            ed.putBoolean("speak_en", o.optBoolean("speakEn", false));
            ed.putInt("text_size", o.optInt("textSize", 1));
            ed.putInt("line_space", o.optInt("lineSpace", 1));
            ed.putBoolean("comfort_on", o.optBoolean("comfort", true));
            ed.putBoolean("continuous", o.optBoolean("continuous", false));
            ed.putString("daily", o.optString("daily", ""));
            fill(seenOwn, o.optJSONArray("seenOwn"));
            fill(seenHot, o.optJSONArray("seenHot"));
            fill(favOwn, o.optJSONArray("favOwn"));
            fill(favHot, o.optJSONArray("favHot"));
            ed.putString("seen_own", QuoteRepo.writeSeen(seenOwn));
            ed.putString("seen_hot", QuoteRepo.writeSeen(seenHot));
            ed.putString("fav_own", QuoteRepo.writeSeen(favOwn));
            ed.putString("fav_hot", QuoteRepo.writeSeen(favHot));
            ed.apply();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
