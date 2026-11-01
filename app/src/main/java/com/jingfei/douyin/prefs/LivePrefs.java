package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * live 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class LivePrefs {
    public static final String NAME = "douyin_live";

    /** 直播增强（page.live） */
    public static final String KEY_PAGE_LIVE = "page.live";

    /** 直播语音替换（live.voice_replace） */
    public static final String KEY_LIVE_VOICE_REPLACE = "live.voice_replace";

    /** 直播具体人数（live.0d1ad71357） */
    public static final String KEY_LIVE_0D1AD71357 = "live.0d1ad71357";

    /** 直播默认画质（live.41fb1b555b） */
    public static final String KEY_LIVE_41FB1B555B = "live.41fb1b555b";

    /** 直播信息入口（live.964ffe7b61） */
    public static final String KEY_LIVE_964FFE7B61 = "live.964ffe7b61";

    /** 当前直播信息（live.0e64c35bc2） */
    public static final String KEY_LIVE_0E64C35BC2 = "live.0e64c35bc2";

    /** 直播清爽（live.7420f518ca） */
    public static final String KEY_LIVE_7420F518CA = "live.7420f518ca";

    /** 移动网络后台续播（live.737df7edc2） */
    public static final String KEY_LIVE_737DF7EDC2 = "live.737df7edc2";

    /** 自动续火（comment_media） */
    public static final String KEY_COMMENT_MEDIA = "comment_media";

    private LivePrefs() {
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