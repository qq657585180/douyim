package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * 播放策略（douyin_playback）Hook 子系统骨架。
 * 由抖X移植：配置读取已就绪，具体 Hook 逻辑在此逐步补充。
 */
final class PlaybackStrategy {
    private static volatile SharedPreferences prefs;

    private PlaybackStrategy() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_playback");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            // TODO: 倍速/进度条/连播/画质/播放策略 Hook（参考 PlayerHooks）
            module.log(Log.INFO, DouyinModule.TAG,
                    "PlaybackStrategy installed (douyin_playback)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "PlaybackStrategy skipped: " + error);
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
                // 配置变更后无需额外处理：Hook 实时读取 getBoolean/getInt/getString
            };
}
