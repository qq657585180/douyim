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
        // 启用控件隐藏总开关（controls.e5681e7ff3）：关闭时强制所有控件隐藏项不生效
        boolean controlsEnabled = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_CONTROLS_E5681E7FF3, true);
        Boolean ctrlClean = boolIfPresent(AppearancePrefs.KEY_CONTROLS_D4F77759DC);
        Boolean ctrlAdCard = boolIfPresent(AppearancePrefs.KEY_CONTROLS_AD_CARD);
        Boolean ctrlReportWarn = boolIfPresent(AppearancePrefs.KEY_CONTROLS_REPORT_WARN);
        if (!controlsEnabled) {
            ctrlClean = false;
            ctrlAdCard = false;
            ctrlReportWarn = false;
        }
        // 抖X外观可见性组 → ImmersiveUi（仅显式配置过才覆盖）
        ImmersiveUi.applyUiVisibilityOverrides(
                boolIfPresent(AppearancePrefs.KEY_HOME_2E0DDFDE95),   // 播放时隐藏系统状态栏
                boolIfPresent(AppearancePrefs.KEY_HOME_7616BC18D9),   // 隐藏系统导航栏
                boolIfPresent(AppearancePrefs.KEY_HOME_BOTTOM_HIDDEN),// 隐藏整个首页底栏
                ctrlClean,                                            // 清爽模式
                ctrlAdCard,                                           // 隐藏广告卡片
                ctrlReportWarn                                        // 隐藏作者声明
        );
        // 隐藏底栏时保留占位（home.bottom_keep_space）→ ImmersiveUi
        ImmersiveUi.setHomeBottomBarKeepSpace(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_BOTTOM_KEEP_SPACE, false));
        // 右侧显示真实数量（controls.9aa386bb74）→ ExactNumbers（受总开关控制）
        boolean rightEnabled = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_CONTROLS_9AA386BB74, false);
        ExactNumbers.setRightEnabled(controlsEnabled && rightEnabled);
        // 隐藏首页搜索入口（home.d23c896621）→ ImmersiveUi
        ImmersiveUi.setHideHomeSearch(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_D23C896621, false));
        // 隐藏消息角标（home.3fe35ca5c8）→ ImmersiveUi
        ImmersiveUi.setHideMessageBadge(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_3FE35CA5C8, false));
        // 隐藏首页底栏皮肤（home.ba08b02cee）→ ImmersiveUi
        ImmersiveUi.setHideBottomBarSkin(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_BA08B02CEE, false));
        // 首页按钮刷新前确认（home.6f099dad58）→ ImmersiveUi
        ImmersiveUi.setRefreshConfirmEnabled(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_6F099DAD58, false));
        // 音乐封面随右侧栏移动（controls.a98f8afb28）→ ImmersiveUi
        ImmersiveUi.setMusicDiscDragEnabled(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_CONTROLS_A98F8AFB28, false));
        // 底栏账号头像（glass.avatar）→ ImmersiveUi
        ImmersiveUi.setGlassAvatarEnabled(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_AVATAR, false));
        // 顶栏双列浏览入口（glass.double）→ ImmersiveUi
        ImmersiveUi.setGlassDoubleEnabled(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_DOUBLE, false));
        // 顶栏实时帧率（glass.fps）→ ImmersiveUi
        ImmersiveUi.setGlassFpsEnabled(AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_FPS, false));
        // 视频右侧栏样式（style__right.23~.28）→ ImmersiveUi
        int rightOpacity = AppearancePrefs.getInt(
                prefs, AppearancePrefs.KEY_STYLE_RIGHT_OPACITY, 100);
        int rightPausedOpacity = AppearancePrefs.getInt(
                prefs, AppearancePrefs.KEY_STYLE_RIGHT_PAUSED_OPACITY, 100);
        boolean rightPausedSeparate = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_STYLE_RIGHT_PAUSED_SEPARATE, false);
        int rightDx = AppearancePrefs.getInt(
                prefs, AppearancePrefs.KEY_STYLE_RIGHT_DX, 0);
        int rightDy = AppearancePrefs.getInt(
                prefs, AppearancePrefs.KEY_STYLE_RIGHT_DY, 0);
        float rightScale = parseFloatPref(
                prefs, AppearancePrefs.KEY_STYLE_RIGHT_SCALE, 1.0f);
        ImmersiveUi.setRightBarStyle(rightOpacity, rightPausedOpacity,
                rightPausedSeparate, rightDx, rightDy, rightScale);
        // 播放背景（controls.1cdcf3fb52）→ ImmersiveUi 视频背景色
        String playbackBackground = prefs.getString(
                AppearancePrefs.KEY_CONTROLS_1CDCF3FB52, null);
        if (playbackBackground != null && !playbackBackground.isEmpty()) {
            ImmersiveUi.setVideoBackgroundColor(playbackBackground);
        }
        // 透明系统导航栏（home.9902412e18）→ ImmersiveUi
        // 透明导航栏（home.9902412e18）/ 底栏沉浸（home.7c24a0e2fc）
        boolean transparentNav = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_9902412E18, false)
                || AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_HOME_7C24A0E2FC, false);
        ImmersiveUi.setTransparentNavBar(transparentNav);
        // 视频背景颜色（background_video.52 启用，.53 颜色）→ ImmersiveUi
        if (AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_BACKGROUND_VIDEO_ENABLE, false)) {
            ImmersiveUi.setVideoBackgroundColor(prefs.getString(
                    AppearancePrefs.KEY_BACKGROUND_VIDEO_COLOR, null));
        } else {
            ImmersiveUi.setVideoBackgroundColor(null);
        }
        // 图文背景（background_photo.59/.60）→ ImmersiveUi
        if (AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_BACKGROUND_PHOTO_ENABLE, false)) {
            ImmersiveUi.setPhotoBackgroundColor(prefs.getString(
                    AppearancePrefs.KEY_BACKGROUND_PHOTO_COLOR, null));
        } else {
            ImmersiveUi.setPhotoBackgroundColor(null);
        }
        // 实况背景（background_livephoto.66/.67）→ ImmersiveUi
        if (AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_BACKGROUND_LIVEPHOTO_ENABLE, false)) {
            ImmersiveUi.setLivePhotoBackgroundColor(prefs.getString(
                    AppearancePrefs.KEY_BACKGROUND_LIVEPHOTO_COLOR, null));
        } else {
            ImmersiveUi.setLivePhotoBackgroundColor(null);
        }
        // 液态玻璃（glass.comment / glass.bottom / glass.module）→ GlassUi
        boolean commentGlass = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_COMMENT, false);
        GlassUi.setCommentGlass(commentGlass,
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_COMMENT_BLUR, 25),
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_COMMENT_OPACITY, 50));
        boolean bottomGlass = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_BOTTOM, false);
        GlassUi.setBottomGlass(bottomGlass,
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_BOTTOM_BLUR, 25),
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_BOTTOM_OPACITY, 50));
        boolean moduleGlass = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_MODULE, false);
        GlassUi.setModuleGlass(moduleGlass,
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_MODULE_BLUR, 25),
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_MODULE_OPACITY, 50));
        // 顶部导航液态玻璃（glass.top）与右侧栏液态玻璃（glass.right）：复用底部 blur/opacity 参数
        boolean topGlass = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_TOP, false);
        GlassUi.setTopGlass(topGlass,
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_BOTTOM_BLUR, 25),
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_BOTTOM_OPACITY, 50));
        boolean rightGlass = AppearancePrefs.getBoolean(
                prefs, AppearancePrefs.KEY_GLASS_RIGHT, false);
        GlassUi.setRightGlass(rightGlass,
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_BOTTOM_BLUR, 25),
                AppearancePrefs.getInt(prefs,
                        AppearancePrefs.KEY_GLASS_BOTTOM_OPACITY, 50));
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

    /** 读取浮点设置：优先 getString 解析，失败回退 getFloat。 */
    private static float parseFloatPref(SharedPreferences prefs, String key, float def) {
        if (prefs == null) {
            return def;
        }
        String s = prefs.getString(key, null);
        if (s != null && !s.isEmpty()) {
            try {
                return Float.parseFloat(s.trim());
            } catch (NumberFormatException ignored) {
                // 继续尝试数值存储
            }
        }
        return prefs.getFloat(key, def);
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
