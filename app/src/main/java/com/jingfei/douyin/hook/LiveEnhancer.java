package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * 直播增强（douyin_live）Hook 子系统。已配置读取；画质/人数/清爽/语音等 Hook 依赖抖音直播内部视图，暂无稳定 API 依据，保持 TODO。
 */
final class LiveEnhancer {
    private static volatile SharedPreferences prefs;

    private LiveEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_live");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            // TODO: 直播画质/人数/清爽/语音 Hook
            module.log(Log.INFO, DouyinModule.TAG,
                    "LiveEnhancer installed (douyin_live)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "LiveEnhancer skipped: " + error);
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
