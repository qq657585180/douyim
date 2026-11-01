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
            applyAutoplayBridge();
            module.log(Log.INFO, DouyinModule.TAG,
                    "PlaybackStrategy installed (douyin_playback)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "PlaybackStrategy skipped: " + error);
        }
    }

    /** 桥接抖X自动连播到 ImmersiveUi；任一开关开启即启用。 */
    private static void applyAutoplayBridge() {
        if (prefs == null) {
            return;
        }
        boolean autoplay =
                PlaybackPrefs.getBoolean(prefs, PlaybackPrefs.KEY_AUTOPLAY, true)
                        || PlaybackPrefs.getBoolean(
                        prefs, PlaybackPrefs.KEY_PAGE_AUTOPLAY, true);
        ImmersiveUi.setAutoPlayEnabled(autoplay);
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
                    applyAutoplayBridge();
                } catch (RuntimeException ignored) {
                    // 桥接失败不影响主流程
                }
            };
}
