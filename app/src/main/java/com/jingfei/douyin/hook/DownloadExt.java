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
        // 页面总开关（page.download）：关闭时恢复默认下载行为
        boolean pageEnabled = DownloadPrefs.getBoolean(
                prefs, DownloadPrefs.KEY_PAGE_DOWNLOAD, true);
        if (!pageEnabled) {
            VideoDownloader.setDownloadSubdirectory(null);
            VideoDownloader.setPathTemplate(null);
            VideoDownloader.setPathDateFormat(null);
            VideoDownloader.setSaveToSystemAlbum(false);
            VideoDownloader.setShowDownloadNotification(true);
            VideoDownloader.setConfirmFileNameBeforeSave(false);
            VideoDownloader.setCheckNotificationPermission(false);
            VideoDownloader.setPreferredFormat(null);
            return;
        }
        String subdirectory = DownloadPrefs.getString(
                prefs, DownloadPrefs.KEY_DOWNLOAD_3B44F4C721, "");
        if (subdirectory == null || subdirectory.isEmpty()) {
            subdirectory = DownloadPrefs.getString(
                    prefs, DownloadPrefs.KEY_PATHS_0B7E287692, "");
        }
        VideoDownloader.setDownloadSubdirectory(subdirectory);
        // 抖X“路径模板（不含扩展名）”（paths.d094b9e3ec）与“日期格式”（paths.7530fa1a19）。
        VideoDownloader.setPathTemplate(DownloadPrefs.getString(
                prefs, DownloadPrefs.KEY_PATHS_D094B9E3EC, null));
        VideoDownloader.setPathDateFormat(DownloadPrefs.getString(
                prefs, DownloadPrefs.KEY_PATHS_7530FA1A19, null));
        boolean systemAlbum = DownloadPrefs.getBoolean(
                prefs, DownloadPrefs.KEY_PATHS_1B6CFE60D5, false);
        VideoDownloader.setSaveToSystemAlbum(systemAlbum);
        boolean showNotification = DownloadPrefs.getBoolean(
                prefs, DownloadPrefs.KEY_DOWNLOAD_INTERACTION_2C25D425DA, true);
        VideoDownloader.setShowDownloadNotification(showNotification);
        boolean confirmFileName = DownloadPrefs.getBoolean(
                prefs, DownloadPrefs.KEY_DOWNLOAD_INTERACTION_60B3A39899, false);
        VideoDownloader.setConfirmFileNameBeforeSave(confirmFileName);
        boolean checkNotification = DownloadPrefs.getBoolean(
                prefs, DownloadPrefs.KEY_DOWNLOAD_INTERACTION_B3668C01D7, false);
        VideoDownloader.setCheckNotificationPermission(checkNotification);
        VideoDownloader.setPreferredFormat(DownloadPrefs.getString(
                prefs, DownloadPrefs.KEY_DOWNLOAD_E4E86807EA, null));
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
