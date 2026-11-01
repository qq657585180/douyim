package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * purify 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class PurifyPrefs {
    public static final String NAME = "douyin_purify";

    /** 清爽模式（clean） */
    public static final String KEY_CLEAN = "clean";

    /** 小游戏免广告领奖（miniapp） */
    public static final String KEY_MINIAPP = "miniapp";

    /** 短剧广告（playlet_ad） */
    public static final String KEY_PLAYLET_AD = "playlet_ad";

    /** 跳过短剧广告（skipad） */
    public static final String KEY_SKIPAD = "skipad";

    /** 弹幕增强（danmaku） */
    public static final String KEY_DANMAKU = "danmaku";

    /** 直播清爽（live_clean） */
    public static final String KEY_LIVE_CLEAN = "live_clean";

    /** 内容过滤（feed_filter） */
    public static final String KEY_FEED_FILTER = "feed_filter";

    /** 搜索页过滤（search.642083e6c9） */
    public static final String KEY_SEARCH_642083E6C9 = "search.642083e6c9";

    /** 隐藏搜索页右下角 AI 抖音（search.5ce1e5947f） */
    public static final String KEY_SEARCH_5CE1E5947F = "search.5ce1e5947f";

    /** 隐藏设置页 AI 搜索（search.2c5104df81） */
    public static final String KEY_SEARCH_2C5104DF81 = "search.2c5104df81";

    /** 启用内容过滤（feed_filter.f42350811b） */
    public static final String KEY_FEED_FILTER_F42350811B = "feed_filter.f42350811b";

    /** 文案关键词（feed_filter.df653a2bf7） */
    public static final String KEY_FEED_FILTER_DF653A2BF7 = "feed_filter.df653a2bf7";

    /** 文案正则（feed_filter.regex） */
    public static final String KEY_FEED_FILTER_REGEX = "feed_filter.regex";

    /** 仅过滤推荐流（feed_filter.scope） */
    public static final String KEY_FEED_FILTER_SCOPE = "feed_filter.scope";

    /** 屏蔽作者（feed_filter.2cb429cc12） */
    public static final String KEY_FEED_FILTER_2CB429CC12 = "feed_filter.2cb429cc12";

    /** 作者白名单（feed_filter.55dec76d5c） */
    public static final String KEY_FEED_FILTER_55DEC76D5C = "feed_filter.55dec76d5c";

    /** 其他过滤设置（feed_filter.9e9332deda） */
    public static final String KEY_FEED_FILTER_9E9332DEDA = "feed_filter.9e9332deda";

    /** 最低点赞数（feed_thresholds.a2065cac44） */
    public static final String KEY_FEED_THRESHOLDS_A2065CAC44 = "feed_thresholds.a2065cac44";

    /** 最短视频时长（秒）（feed_thresholds.min_duration） */
    public static final String KEY_FEED_THRESHOLDS_MIN_DURATION = "feed_thresholds.min_duration";

    /** 最长视频时长（秒）（feed_thresholds.max_duration） */
    public static final String KEY_FEED_THRESHOLDS_MAX_DURATION = "feed_thresholds.max_duration";

    /** 最低评论数（feed_thresholds.d2fc778560） */
    public static final String KEY_FEED_THRESHOLDS_D2FC778560 = "feed_thresholds.d2fc778560";

    /** 最低收藏数（feed_thresholds.6ae0300933） */
    public static final String KEY_FEED_THRESHOLDS_6AE0300933 = "feed_thresholds.6ae0300933";

    /** 最低分享数（feed_thresholds.e885e7e1b6） */
    public static final String KEY_FEED_THRESHOLDS_E885E7E1B6 = "feed_thresholds.e885e7e1b6";

    /** 仅保留最近几天（feed_thresholds.61b4880c99） */
    public static final String KEY_FEED_THRESHOLDS_61B4880C99 = "feed_thresholds.61b4880c99";

    private PurifyPrefs() {
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