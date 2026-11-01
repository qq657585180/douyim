package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * profile 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class ProfilePrefs {
    public static final String NAME = "douyin_profile";

    /** 个人主页（page.profile） */
    public static final String KEY_PAGE_PROFILE = "page.profile";

    /** 个人主页作品视图（profile_style） */
    public static final String KEY_PROFILE_STYLE = "profile_style";

    /** 主页精确数量（profile.exact_counts） */
    public static final String KEY_PROFILE_EXACT_COUNTS = "profile.exact_counts";

    /** 作品网格精确点赞（profile.exact_grid_likes） */
    public static final String KEY_PROFILE_EXACT_GRID_LIKES = "profile.exact_grid_likes";

    /** 默认打开作品页（profile.7279f9e6d6） */
    public static final String KEY_PROFILE_7279F9E6D6 = "profile.7279f9e6d6";

    /** 本地访客开启标识（glass_video） */
    public static final String KEY_GLASS_VIDEO = "glass_video";

    private ProfilePrefs() {
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