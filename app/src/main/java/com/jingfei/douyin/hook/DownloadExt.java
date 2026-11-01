package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

import com.jingfei.douyin.prefs.DownloadPrefs;

/**
 * 下载增强（douyin_download）Hook 子系统。
 * 由抖X移植：将抖X下载路径设置桥接到 VideoDownloader。
 * 已实现：保存子目录 / 保存位置 → VideoDownloader。
 * 待补充：分享栏/长按菜单/插眼/token书签等 Hook。
 */
final class DownloadExt {
    private static volatile SharedPreferences prefs;

    private DownloadExt() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_download");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            applyPathBridge();
            module.log(Log.INFO, DouyinModule.TAG,
                    "DownloadExt installed (douyin_download)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "DownloadExt skipped: " + error);
        }
    }

    /** 桥接抖X的"保存子目录/保存位置"到 VideoDownloader。 */
    private static void applyPathBridge() {
        if (prefs == null) {
            return;
        }
        String subdirectory = DownloadPrefs.getString(
                prefs, DownloadPrefs.KEY_DOWNLOAD_3B44F4C721, "");
        if (subdirectory == null || subdirectory.isEmpty()) {
            subdirectory = DownloadPrefs.getString(
                    prefs, DownloadPrefs.KEY_PATHS_0B7E287692, "");
        }
        VideoDownloader.setDownloadSubdirectory(subdirectory);
    }

    static boolean bool(String key, boolean def) {
        return prefs != null && prefs.getBoolean(key, def);
    }

    static int integer(String key, int def) {
        return prefs == null ? def : prefs.getInt(key, def);
    }

    static String string(String key, String def) {
        return prefs == null ? def : prefs.getString(key, def);
    }

    private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER =
            (preferences, key) -> {
                prefs = preferences;
                try {
                    applyPathBridge();
                } catch (RuntimeException ignored) {
                    // 桥接失败不影响主流程
                }
            };
}
