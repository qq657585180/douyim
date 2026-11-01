package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * system 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class SystemPrefs {
    public static final String NAME = "douyin_system";

    /** 界面风格（module_theme） */
    public static final String KEY_MODULE_THEME = "module_theme";

    /** 主题图标包（iconpacks） */
    public static final String KEY_ICONPACKS = "iconpacks";

    /** 账号（webdav_connection） */
    public static final String KEY_WEBDAV_CONNECTION = "webdav_connection";

    /** 评论增强（comments） */
    public static final String KEY_COMMENTS = "comments";

    /** 搜索页整理（search） */
    public static final String KEY_SEARCH = "search";

    /** 兼容性检查（diagnostics） */
    public static final String KEY_DIAGNOSTICS = "diagnostics";

    /** 运行日志（logs） */
    public static final String KEY_LOGS = "logs";

    /** 我的插眼（bookmarks） */
    public static final String KEY_BOOKMARKS = "bookmarks";

    /** 配置备份与恢复（config） */
    public static final String KEY_CONFIG = "config";

    /** WebDAV备份与恢复（webdav） */
    public static final String KEY_WEBDAV = "webdav";

    /** 画质与缓存（page.extras） */
    public static final String KEY_PAGE_EXTRAS = "page.extras";

    private SystemPrefs() {
    }

    public static boolean getBoolean(SharedPreferences prefs, String key, boolean def) {
        return prefs != null && prefs.getBoolean(key, def);
    }

    public static int getInt(SharedPreferences prefs, String key, int def) {
        return prefs == null ? def : prefs.getInt(key, def);
    }

    public static String getString(SharedPreferences prefs, String key, String def) {
        return prefs == null ? def : prefs.getString(key, def);
    }

    public static void putBoolean(SharedPreferences prefs, String key, boolean value) {
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
    }

    public static void putInt(SharedPreferences prefs, String key, int value) {
        if (prefs != null) prefs.edit().putInt(key, value).apply();
    }

    public static void putString(SharedPreferences prefs, String key, String value) {
        if (prefs != null) prefs.edit().putString(key, value).apply();
    }
}