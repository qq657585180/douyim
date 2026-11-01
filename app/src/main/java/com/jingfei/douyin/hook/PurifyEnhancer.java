package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * 净化过滤（douyin_purify）Hook 子系统骨架。
 * 由抖X移植：配置读取已就绪，具体 Hook 逻辑在此逐步补充。
 */
final class PurifyEnhancer {
    private static volatile SharedPreferences prefs;

    private PurifyEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_purify");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            // TODO: 弹幕/清爽/广告跳过/feed阈值/小程序广告 Hook
            module.log(Log.INFO, DouyinModule.TAG,
                    "PurifyEnhancer installed (douyin_purify)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "PurifyEnhancer skipped: " + error);
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
