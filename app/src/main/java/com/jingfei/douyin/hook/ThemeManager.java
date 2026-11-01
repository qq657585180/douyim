package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

import com.jingfei.douyin.prefs.AppearancePrefs;

/**
 * 外观主题（douyin_appearance）Hook 子系统。
 * 由抖X移植：将抖X外观设置桥接到沉浸式 UI。
 * 已实现：沉浸显示（home.6c47740864）→ ImmersiveUi。
 * 待补充：主题/图标包/背景图/材质/玻璃外观（glass.*）等 Hook。
 */
final class ThemeManager {
    private static volatile SharedPreferences prefs;

    private ThemeManager() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_appearance");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            applyImmersiveBridge();
            module.log(Log.INFO, DouyinModule.TAG,
                    "ThemeManager installed (douyin_appearance)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "ThemeManager skipped: " + error);
        }
    }

    /** 桥接"沉浸显示"开关到 ImmersiveUi；仅当用户在抖X设置里显式配置过才覆盖。 */
    private static void applyImmersiveBridge() {
        if (prefs == null) {
            return;
        }
        if (prefs.contains(AppearancePrefs.KEY_HOME_6C47740864)) {
            boolean immersive = AppearancePrefs.getBoolean(
                    prefs, AppearancePrefs.KEY_HOME_6C47740864, true);
            ImmersiveUi.applyAppearanceOverrides(null, null, immersive, null, null);
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
                    applyImmersiveBridge();
                } catch (RuntimeException ignored) {
                    // 桥接失败不影响主流程
                }
            };
}
