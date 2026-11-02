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
