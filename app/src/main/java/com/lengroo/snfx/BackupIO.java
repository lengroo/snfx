package com.lengroo.snfx;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

/** 备份文件的读写：用系统文件选择器（SAF），不需要存储权限。 */
public final class BackupIO {

    public static final int REQ_EXPORT = 1002;
    public static final int REQ_IMPORT = 1003;

    private BackupIO() {
    }

    public static void export(final MainActivity a) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "meiriyibian-backup.json");
        try {
            a.startActivityForResult(i, REQ_EXPORT);
        } catch (Throwable t) {
            Toast.makeText(a, "打不开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    public static void importFrom(final MainActivity a) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            a.startActivityForResult(i, REQ_IMPORT);
        } catch (Throwable t) {
            Toast.makeText(a, "打不开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    /** 返回 true 表示这个 requestCode 由本类处理。 */
    public static boolean handle(MainActivity a, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_EXPORT && requestCode != REQ_IMPORT) return false;
        if (data == null || data.getData() == null) return true;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) write(a, uri);
        else read(a, uri);
        return true;
    }

    private static void write(MainActivity a, Uri uri) {
        OutputStream out = null;
        try {
            out = a.getContentResolver().openOutputStream(uri);
            out.write(a.buildBackupJson().getBytes("UTF-8"));
            out.flush();
            Toast.makeText(a, "备份已导出", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "导出失败：" + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        } finally {
            if (out != null) try {
                out.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void read(MainActivity a, Uri uri) {
        InputStream in = null;
        try {
            in = a.getContentResolver().openInputStream(uri);
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            String json = new String(bos.toByteArray(), "UTF-8");
            if (a.applyBackupJson(json)) {
                Toast.makeText(a, "备份已恢复", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(a, "这个文件不是本应用的备份", Toast.LENGTH_LONG).show();
            }
        } catch (Throwable t) {
            Toast.makeText(a, "恢复失败：" + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        } finally {
            if (in != null) try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
