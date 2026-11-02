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
        // 抖X外观可见性组 → ImmersiveUi（仅显式配置过才覆盖）
        ImmersiveUi.applyUiVisibilityOverrides(
                boolIfPresent(AppearancePrefs.KEY_HOME_2E0DDFDE95),   // 播放时隐藏系统状态栏
                boolIfPresent(AppearancePrefs.KEY_HOME_7616BC18D9),   // 隐藏系统导航栏
                boolIfPresent(AppearancePrefs.KEY_HOME_BOTTOM_HIDDEN),// 隐藏整个首页底栏
                boolIfPresent(AppearancePrefs.KEY_CONTROLS_D4F77759DC),// 清爽模式
                boolIfPresent(AppearancePrefs.KEY_CONTROLS_AD_CARD),  // 隐藏广告卡片
                boolIfPresent(AppearancePrefs.KEY_CONTROLS_REPORT_WARN) // 隐藏作者声明
        );
    }

    /** prefs 中存在该 key 才返回布尔值，否则返回 null（不覆盖）。 */
    private static Boolean boolIfPresent(String key) {
        if (prefs != null && prefs.contains(key)) {
            return AppearancePrefs.getBoolean(prefs, key, false);
        }
        return null;
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
