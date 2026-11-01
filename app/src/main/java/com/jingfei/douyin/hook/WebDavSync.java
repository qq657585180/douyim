package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * WebDAV备份（douyin_system）Hook 子系统。已配置读取；WebDAV 同步/诊断/日志/问题报告等 Hook 依赖抖音数据格式，暂无稳定 API 依据，保持 TODO。
 */
final class WebDavSync {
    private static volatile SharedPreferences prefs;

    private WebDavSync() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_system");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            // TODO: 诊断/日志/WebDAV备份/问题报告 Hook
            module.log(Log.INFO, DouyinModule.TAG,
                    "WebDavSync installed (douyin_system)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "WebDavSync skipped: " + error);
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
