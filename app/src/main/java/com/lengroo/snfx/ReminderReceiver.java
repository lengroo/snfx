package com.lengroo.snfx;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * 每天定时提醒：到点推一条今日一句，点开进 App。
 * 闹钟存在 AlarmManager 里，响过一次就约明天的；重启后靠 BOOT_COMPLETED 重新约。
 */
public final class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "daily_reminder";
    private static final int REQ_ALARM = 2001;
    private static final int NOTIF_ID = 3001;

    public static final String ACTION_CAPSULE = "com.lengroo.snfx.CAPSULE";
    private static final int REQ_CAPSULE = 2002;
    private static final int NOTIF_CAPSULE = 3002;
    private static final int CAPSULE_HOUR = 9;

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            SharedPreferences p = c.getSharedPreferences("dby", Context.MODE_PRIVATE);
            if (p.getBoolean("reminder_on", false)) schedule(c);
            scheduleCapsule(c);
            return;
        }
        if (ACTION_CAPSULE.equals(action)) {
            showCapsule(c);
            return;
        }
        showToday(c);
        schedule(c);
    }

    /** 到胶囊日期当天的 9:00 推一条，提醒你回来看自己埋下的话。 */
    public static void scheduleCapsule(Context c) {
        SharedPreferences prefs = c.getSharedPreferences("dby", Context.MODE_PRIVATE);
        String text = prefs.getString("capsule_text", "");
        String day = prefs.getString("capsule_date", "");
        if (text.length() == 0 || day.length() != 8 || prefs.getBoolean("capsule_sent", false)) {
            cancelCapsule(c);
            return;
        }
        Calendar when = Calendar.getInstance();
        try {
            when.set(Integer.parseInt(day.substring(0, 4)),
                    Integer.parseInt(day.substring(4, 6)) - 1,
                    Integer.parseInt(day.substring(6, 8)),
                    CAPSULE_HOUR, 0, 0);
            when.set(Calendar.MILLISECOND, 0);
        } catch (Throwable t) {
            return;
        }
        if (when.getTimeInMillis() <= System.currentTimeMillis()) {
            cancelCapsule(c);
            return;
        }
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = capsuleIntent(c);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
        } else if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
        }
    }

    public static void cancelCapsule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(capsuleIntent(c));
    }

    private static PendingIntent capsuleIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION_CAPSULE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, REQ_CAPSULE, i, flags);
    }

    private static void showCapsule(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "每日提醒",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("每天定时推一句今日文案，以及时间胶囊到期提醒");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(c, 1, open, flags);
        int icon = c.getResources().getIdentifier("ic_launcher", "mipmap", c.getPackageName());
        if (icon == 0) icon = android.R.drawable.ic_dialog_info;
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
        }
        b.setSmallIcon(icon)
                .setContentTitle("那天埋下的话，今天到期了")
                .setContentText("点开看看，是你自己写给今天的")
                .setStyle(new Notification.BigTextStyle()
                        .bigText("那天埋下的话，今天到期了。点开看看，是你自己写给今天的。"))
                .setContentIntent(content)
                .setAutoCancel(true);
        nm.notify(NOTIF_CAPSULE, b.build());
    }

    /** 按 prefs 里的开关和时间，约下一次提醒。 */
    public static void schedule(Context c) {
        SharedPreferences prefs = c.getSharedPreferences("dby", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("reminder_on", false)) return;
        int hour = prefs.getInt("reminder_hour", 22);
        int min = prefs.getInt("reminder_min", 0);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, min);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // 用户关了精确闹钟权限就退回普通闹钟，时间可能差几分钟
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        } else if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }

    /** 取消已约的提醒。 */
    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(alarmIntent(c));
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, REQ_ALARM, i, flags);
    }

    /** 今日一句：和 App 里「今日」按钮同一个算法，同一天固定同一篇。 */
    private static void showToday(Context c) {
        SharedPreferences prefs = c.getSharedPreferences("dby", Context.MODE_PRIVATE);
        int mode = prefs.getInt("mode", 0);
        String[] pool = QuoteRepo.load(c, mode == 0 ? "quotes.txt" : "hot.txt");
        if (pool.length == 0) return;
        Calendar cal = Calendar.getInstance();
        long seed = cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR);
        int idx = (int) Math.abs((seed * 2654435761L) % pool.length) % pool.length;
        String quote = pool[idx].trim();
        if (quote.length() > 120) quote = quote.substring(0, 120) + "…";

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "每日提醒",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("每天定时推一句今日文案");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(c, 0, open, flags);
        // 通知小图标用 App 自己的心形图标（本工程不生成 R.java，按名字找）。
        int icon = c.getResources().getIdentifier("ic_launcher", "mipmap", c.getPackageName());
        if (icon == 0) icon = android.R.drawable.ic_dialog_info;
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
        }
        b.setSmallIcon(icon)
                .setContentTitle("今日一遍")
                .setContentText(quote)
                .setStyle(new Notification.BigTextStyle().bigText(quote))
                .setContentIntent(content)
                .setAutoCancel(true);
        nm.notify(NOTIF_ID, b.build());
    }
}
