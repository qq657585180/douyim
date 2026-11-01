package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * playback 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class PlaybackPrefs {
    public static final String NAME = "douyin_playback";

    /** 播放与交互策略（playback_strategy） */
    public static final String KEY_PLAYBACK_STRATEGY = "playback_strategy";

    /** 默认播放画质（quality） */
    public static final String KEY_QUALITY = "quality";

    /** 手势设置（gestures） */
    public static final String KEY_GESTURES = "gestures";

    /** 播放画质（page.quality） */
    public static final String KEY_PAGE_QUALITY = "page.quality";

    /** 长按播放（longpress_playback） */
    public static final String KEY_LONGPRESS_PLAYBACK = "longpress_playback";

    /** 自动连播（autoplay） */
    public static final String KEY_AUTOPLAY = "autoplay";

    /** 默认倍速（speed） */
    public static final String KEY_SPEED = "speed";

    /** 常驻进度条（progress.20ee30e2ae） */
    public static final String KEY_PROGRESS_20EE30E2AE = "progress.20ee30e2ae";

    /** 时间独立常显（playback.time_independent） */
    public static final String KEY_PLAYBACK_TIME_INDEPENDENT = "playback.time_independent";

    /** 时间位置（playback.time_position） */
    public static final String KEY_PLAYBACK_TIME_POSITION = "playback.time_position";

    /** 时间字号（sp）（playback.time_size） */
    public static final String KEY_PLAYBACK_TIME_SIZE = "playback.time_size";

    /** 时间颜色（playback.time_color） */
    public static final String KEY_PLAYBACK_TIME_COLOR = "playback.time_color";

    /** 时间透明度（%）（playback.time_opacity） */
    public static final String KEY_PLAYBACK_TIME_OPACITY = "playback.time_opacity";

    /** 时间水平边距（dp）（playback.time_margin） */
    public static final String KEY_PLAYBACK_TIME_MARGIN = "playback.time_margin";

    /** 时间底部边距（dp）（playback.time_bottom） */
    public static final String KEY_PLAYBACK_TIME_BOTTOM = "playback.time_bottom";

    /** 显示播放时间（playback.progress_time） */
    public static final String KEY_PLAYBACK_PROGRESS_TIME = "playback.progress_time";

    /** 自动跳过章节广告（extras.d2a3ea57f1） */
    public static final String KEY_EXTRAS_D2A3EA57F1 = "extras.d2a3ea57f1";

    /** 离线缓存数量（extras.739e412c3a） */
    public static final String KEY_EXTRAS_739E412C3A = "extras.739e412c3a";

    /** 清爽模式设置（gestures.37b88fc1fb） */
    public static final String KEY_GESTURES_37B88FC1FB = "gestures.37b88fc1fb";

    /** 长按（gestures.a22fbb4c39） */
    public static final String KEY_GESTURES_A22FBB4C39 = "gestures.a22fbb4c39";

    /** 双击（gestures.26d7c24f77） */
    public static final String KEY_GESTURES_26D7C24F77 = "gestures.26d7c24f77";

    /** 临时倍速（gestures.be54e501cb） */
    public static final String KEY_GESTURES_BE54E501CB = "gestures.be54e501cb";

    /** 预加载策略（extras.preload） */
    public static final String KEY_EXTRAS_PRELOAD = "extras.preload";

    /** 预加载策略（preload） */
    public static final String KEY_PRELOAD = "preload";

    /** 预加载数量（extras.preload_count） */
    public static final String KEY_EXTRAS_PRELOAD_COUNT = "extras.preload_count";

    /** 预加载清晰度（extras.preload_quality） */
    public static final String KEY_EXTRAS_PRELOAD_QUALITY = "extras.preload_quality";

    /** 保护当前缓存作品（extras.recall） */
    public static final String KEY_EXTRAS_RECALL = "extras.recall";

    /** 保留关注流缓存作品（extras.keep_follow） */
    public static final String KEY_EXTRAS_KEEP_FOLLOW = "extras.keep_follow";

    /** 原画本地代理（quality.proxy） */
    public static final String KEY_QUALITY_PROXY = "quality.proxy";

    /** 原画本地代理（proxy） */
    public static final String KEY_PROXY = "proxy";

    /** 小视频整文件缓存（quality.whole_cache） */
    public static final String KEY_QUALITY_WHOLE_CACHE = "quality.whole_cache";

    /** 播放器诊断信息（quality.dd11313c92） */
    public static final String KEY_QUALITY_DD11313C92 = "quality.dd11313c92";

    /** 原画仅 Wi-Fi（quality.4360cc0065） */
    public static final String KEY_QUALITY_4360CC0065 = "quality.4360cc0065";

    /** 启用弹幕增强（danmaku.83d6b31d9a） */
    public static final String KEY_DANMAKU_83D6B31D9A = "danmaku.83d6b31d9a";

    /** 屏蔽关键词（danmaku.1d6c3a1841） */
    public static final String KEY_DANMAKU_1D6C3A1841 = "danmaku.1d6c3a1841";

    /** 重复弹幕去重（danmaku.01c0117c23） */
    public static final String KEY_DANMAKU_01C0117C23 = "danmaku.01c0117c23";

    /** 解除抖音屏蔽词 30 条限制（danmaku.a1ce67ce92） */
    public static final String KEY_DANMAKU_A1CE67CE92 = "danmaku.a1ce67ce92";

    /** 打开评论区时保留弹幕（danmaku.ea1dc8805e） */
    public static final String KEY_DANMAKU_EA1DC8805E = "danmaku.ea1dc8805e";

    /** 弹幕颜色（danmaku.9daa16e0ab） */
    public static final String KEY_DANMAKU_9DAA16E0AB = "danmaku.9daa16e0ab";

    /** 隐藏视频进度条（progress.hidden） */
    public static final String KEY_PROGRESS_HIDDEN = "progress.hidden";

    /** 自动连播与完成动作（page.autoplay） */
    public static final String KEY_PAGE_AUTOPLAY = "page.autoplay";

    private PlaybackPrefs() {
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