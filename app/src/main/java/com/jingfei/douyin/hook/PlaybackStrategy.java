package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

import com.jingfei.douyin.prefs.PlaybackPrefs;

/**
 * 播放策略（douyin_playback）Hook 子系统。
 * 由抖X移植：将抖X播放设置桥接到沉浸式 UI 与播放器。
 * 已实现：自动连播 → ImmersiveUi。
 * 待补充：倍速/画质/常驻进度条/播放策略（参考 PlayerHooks）等 Hook。
 */
final class PlaybackStrategy {
    private static volatile SharedPreferences prefs;

    private PlaybackStrategy() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_playback");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            applyPlaybackBridge();
            module.log(Log.INFO, DouyinModule.TAG,
                    "PlaybackStrategy installed (douyin_playback)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "PlaybackStrategy skipped: " + error);
        }
    }

    /** 播放策略总桥接：自动连播 + 完成暂停 + 倍速。 */
    private static void applyPlaybackBridge() {
        if (prefs == null) {
            return;
        }
        boolean autoplay =
                PlaybackPrefs.getBoolean(prefs, PlaybackPrefs.KEY_AUTOPLAY, true)
                        || PlaybackPrefs.getBoolean(
                        prefs, PlaybackPrefs.KEY_PAGE_AUTOPLAY, true);
        ImmersiveUi.setAutoPlayEnabled(autoplay);
        // 抖X"播放完成暂停"：关闭自动连播时，视频播完主动暂停而不是自动切下一条。
        PlaybackState.setPauseOnCompletion(!autoplay);
        // 抖X"默认倍速"：KEY_SPEED 以 String（"1.0"/"1.5"/"2.0"）或 float 存储。
        float speed = 1f;
        String speedText = prefs.getString(PlaybackPrefs.KEY_SPEED, null);
        if (speedText != null) {
            try {
                speed = Float.parseFloat(speedText);
            } catch (NumberFormatException ignored) {
                speed = 1f;
            }
        } else {
            speed = prefs.getFloat(PlaybackPrefs.KEY_SPEED, 1f);
        }
        PlayerHooks.setPlaybackSpeed(speed);
        // 抖X"弹幕增强-打开评论区时保留弹幕" → ImmersiveUi.showDanmaku。
        // 仅当用户在抖X里显式配置过弹幕增强或保留弹幕时才覆盖。
        if (prefs.contains(PlaybackPrefs.KEY_DANMAKU_83D6B31D9A)
                || prefs.contains(PlaybackPrefs.KEY_DANMAKU_EA1DC8805E)) {
            boolean danmakuEnabled = prefs.getBoolean(
                    PlaybackPrefs.KEY_DANMAKU_83D6B31D9A, true);
            boolean keepDanmaku = prefs.getBoolean(
                    PlaybackPrefs.KEY_DANMAKU_EA1DC8805E, true);
            ImmersiveUi.applyAppearanceOverrides(
                    null, null, null, danmakuEnabled && keepDanmaku, null);
        }
        // 抖X"双击"手势 → ImmersiveUi 拦截双击点赞（仅显式配置过才覆盖）。
        if (prefs.contains(PlaybackPrefs.KEY_GESTURES_26D7C24F77)) {
            boolean blockDoubleTap = PlaybackPrefs.getBoolean(
                    prefs, PlaybackPrefs.KEY_GESTURES_26D7C24F77, false);
            ImmersiveUi.applyAppearanceOverrides(null, blockDoubleTap, null, null, null);
        }
        // 抖X"清爽模式设置"（播放页手势）→ 清爽模式（隐藏多余控件）。
        if (prefs.contains(PlaybackPrefs.KEY_GESTURES_37B88FC1FB)) {
            boolean clean = PlaybackPrefs.getBoolean(
                    prefs, PlaybackPrefs.KEY_GESTURES_37B88FC1FB, false);
            ImmersiveUi.applyUiVisibilityOverrides(null, null, null, clean, null, null);
        }
        // 抖X"显示播放时间"（playback.progress_time）+ 样式组（time_*）。
        if (prefs.contains(PlaybackPrefs.KEY_PLAYBACK_PROGRESS_TIME)) {
            boolean showTime = PlaybackPrefs.getBoolean(
                    prefs, PlaybackPrefs.KEY_PLAYBACK_PROGRESS_TIME, false);
            if (showTime) {
                String pos = prefs.getString(
                        PlaybackPrefs.KEY_PLAYBACK_TIME_POSITION, "right");
                int size = prefs.getInt(PlaybackPrefs.KEY_PLAYBACK_TIME_SIZE, 14);
                int color = parseColor(
                        prefs.getString(PlaybackPrefs.KEY_PLAYBACK_TIME_COLOR, null),
                        prefs.getInt(PlaybackPrefs.KEY_PLAYBACK_TIME_COLOR, 0xFFFFFF));
                int opacity = prefs.getInt(
                        PlaybackPrefs.KEY_PLAYBACK_TIME_OPACITY, 80);
                int margin = prefs.getInt(
                        PlaybackPrefs.KEY_PLAYBACK_TIME_MARGIN, 12);
                int bottom = prefs.getInt(
                        PlaybackPrefs.KEY_PLAYBACK_TIME_BOTTOM, 48);
                PlaybackTimeOverlay.applyPrefs(
                        true, pos, size, color, opacity, margin, bottom);
            } else {
                PlaybackTimeOverlay.applyPrefs(
                        false, null, 0, 0, 0, 0, 0);
            }
        }
        // 抖X"弹幕增强"：屏蔽关键词（danmaku.1d6c3a1841）+ 弹幕颜色（danmaku.9daa16e0ab）。
        boolean danmakuFilter = PlaybackPrefs.getBoolean(
                prefs, PlaybackPrefs.KEY_DANMAKU_83D6B31D9A, false)
                && prefs.contains(PlaybackPrefs.KEY_DANMAKU_1D6C3A1841);
        String danmakuKeywords = prefs.getString(
                PlaybackPrefs.KEY_DANMAKU_1D6C3A1841, "");
        int danmakuColor = 0;
        String colorText = prefs.getString(
                PlaybackPrefs.KEY_DANMAKU_9DAA16E0AB, null);
        if (colorText != null && !colorText.isEmpty()) {
            try {
                danmakuColor = android.graphics.Color.parseColor(colorText);
            } catch (IllegalArgumentException ignored) {
                // 非法颜色串忽略
            }
        } else if (prefs.contains(PlaybackPrefs.KEY_DANMAKU_9DAA16E0AB)) {
            danmakuColor = prefs.getInt(
                    PlaybackPrefs.KEY_DANMAKU_9DAA16E0AB, 0);
        }
        DanmakuEnhancer.applyPrefs(danmakuFilter, danmakuKeywords, danmakuColor);
    }

    /** 解析颜色字符串（#RRGGBB），失败时回退默认值。 */
    private static int parseColor(String text, int fallback) {
        if (text != null && !text.isEmpty()) {
            try {
                return android.graphics.Color.parseColor(text);
            } catch (IllegalArgumentException ignored) {
                // 非法颜色串回退默认
            }
        }
        return fallback;
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
                    applyPlaybackBridge();
                } catch (RuntimeException ignored) {
                    // 桥接失败不影响主流程
                }
            };
}
