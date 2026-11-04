package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * appearance 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class AppearancePrefs {
    public static final String NAME = "douyin_appearance";

    /** 提交问题报告（report_issue） */
    public static final String KEY_REPORT_ISSUE = "report_issue";

    /** 参考分组底栏（glass.bottom.reference） */
    public static final String KEY_GLASS_BOTTOM_REFERENCE = "glass.bottom.reference";

    /** 顶部导航液态玻璃（glass.top） */
    public static final String KEY_GLASS_TOP = "glass.top";

    /** 视频按钮液态玻璃（glass.right） */
    public static final String KEY_GLASS_RIGHT = "glass.right";

    /** 发布入口独立成圆（glass.bottom.detail.0） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_0 = "glass.bottom.detail.0";

    /** 融合玻璃进度条（glass.bottom.detail.1） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_1 = "glass.bottom.detail.1";

    /** 玻璃进度条点击跳转（glass.bottom.detail.2） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_2 = "glass.bottom.detail.2";

    /** 底栏背景模糊（glass.bottom.detail.3） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_3 = "glass.bottom.detail.3";

    /** 底栏只显示图标（glass.bottom.detail.4） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_4 = "glass.bottom.detail.4";

    /** 姿态感知高光（glass.bottom.detail.5） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_5 = "glass.bottom.detail.5";

    /** 底栏缩放（glass.bottom.detail.6） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_6 = "glass.bottom.detail.6";

    /** 底栏位置偏移（glass.bottom.detail.7） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_7 = "glass.bottom.detail.7";

    /** 底栏图标大小（glass.bottom.detail.8） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_8 = "glass.bottom.detail.8";

    /** 边缘折射强度（glass.bottom.detail.9） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_9 = "glass.bottom.detail.9";

    /** 边缘高光强度（glass.bottom.detail.10） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_10 = "glass.bottom.detail.10";

    /** 按压色散强度（glass.bottom.detail.11） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_11 = "glass.bottom.detail.11";

    /** 按压内阴影强度（glass.bottom.detail.12） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_12 = "glass.bottom.detail.12";

    /** 玻璃进度条厚度（glass.bottom.thickness） */
    public static final String KEY_GLASS_BOTTOM_THICKNESS = "glass.bottom.thickness";

    /** 背景采样间隔（glass.bottom.detail.13） */
    public static final String KEY_GLASS_BOTTOM_DETAIL_13 = "glass.bottom.detail.13";

    /** 模块弹窗液态玻璃 · 模糊半径（glass.module.blur） */
    public static final String KEY_GLASS_MODULE_BLUR = "glass.module.blur";

    /** 模块弹窗液态玻璃 · 透明度（glass.module.opacity） */
    public static final String KEY_GLASS_MODULE_OPACITY = "glass.module.opacity";

    /** 首页底栏液态玻璃 · 模糊半径（glass.bottom.blur） */
    public static final String KEY_GLASS_BOTTOM_BLUR = "glass.bottom.blur";

    /** 首页底栏液态玻璃 · 透明度（glass.bottom.opacity） */
    public static final String KEY_GLASS_BOTTOM_OPACITY = "glass.bottom.opacity";

    /** 评论面板液态玻璃 · 模糊半径（glass.comment.blur） */
    public static final String KEY_GLASS_COMMENT_BLUR = "glass.comment.blur";

    /** 评论面板液态玻璃 · 透明度（glass.comment.opacity） */
    public static final String KEY_GLASS_COMMENT_OPACITY = "glass.comment.opacity";

    /** 播放与暂停样式（styles_video） */
    public static final String KEY_STYLES_VIDEO = "styles_video";

    /** 底栏图标与文字（styles_home） */
    public static final String KEY_STYLES_HOME = "styles_home";

    /** 顶部导航样式（style__top） */
    public static final String KEY_STYLE_TOP = "style__top";

    /** 底部导航 · 透明度（style__bottom） */
    public static final String KEY_STYLE_BOTTOM = "style__bottom";

    /** 视频覆盖控件样式（style__overlay） */
    public static final String KEY_STYLE_OVERLAY = "style__overlay";

    /** 视频右侧栏样式（style__right） */
    public static final String KEY_STYLE_RIGHT = "style__right";

    /** 视频右侧栏 · 透明度（style__right.23） */
    public static final String KEY_STYLE_RIGHT_OPACITY = "style__right.23";

    /** 视频右侧栏 · 暂停透明度（style__right.24） */
    public static final String KEY_STYLE_RIGHT_PAUSED_OPACITY = "style__right.24";

    /** 视频右侧栏 · 暂停时使用独立透明度（style__right.25） */
    public static final String KEY_STYLE_RIGHT_PAUSED_SEPARATE = "style__right.25";

    /** 视频右侧栏 · 左右移动 dp（style__right.26） */
    public static final String KEY_STYLE_RIGHT_DX = "style__right.26";

    /** 视频右侧栏 · 上下移动 dp（style__right.27） */
    public static final String KEY_STYLE_RIGHT_DY = "style__right.27";

    /** 视频右侧栏 · 缩放 0.1–3 倍（style__right.28） */
    public static final String KEY_STYLE_RIGHT_SCALE = "style__right.28";

    /** 视频背景（background_video） */
    public static final String KEY_BACKGROUND_VIDEO = "background_video";

    /** 视频背景 · 启用视频背景（background_video.52） */
    public static final String KEY_BACKGROUND_VIDEO_ENABLE = "background_video.52";

    /** 视频背景 · 背景颜色（background_video.53，#RRGGBB / #AARRGGBB） */
    public static final String KEY_BACKGROUND_VIDEO_COLOR = "background_video.53";

    /** 图文背景 · 启用（background_photo.59） */
    public static final String KEY_BACKGROUND_PHOTO_ENABLE = "background_photo.59";

    /** 图文背景 · 背景颜色（background_photo.60，#RRGGBB / #AARRGGBB） */
    public static final String KEY_BACKGROUND_PHOTO_COLOR = "background_photo.60";

    /** 实况背景 · 启用（background_livephoto.66） */
    public static final String KEY_BACKGROUND_LIVEPHOTO_ENABLE = "background_livephoto.66";

    /** 实况背景 · 背景颜色（background_livephoto.67，#RRGGBB / #AARRGGBB） */
    public static final String KEY_BACKGROUND_LIVEPHOTO_COLOR = "background_livephoto.67";

    /** 图文背景（background_photo） */
    public static final String KEY_BACKGROUND_PHOTO = "background_photo";

    /** 实况背景（background_livephoto） */
    public static final String KEY_BACKGROUND_LIVEPHOTO = "background_livephoto";

    /** 清透玻璃（page.glass） */
    public static final String KEY_PAGE_GLASS = "page.glass";

    /** 底栏显示账号头像（glass.avatar） */
    public static final String KEY_GLASS_AVATAR = "glass.avatar";

    /** 顶栏双列浏览入口（glass.double） */
    public static final String KEY_GLASS_DOUBLE = "glass.double";

    /** 顶栏实时帧率（glass.fps） */
    public static final String KEY_GLASS_FPS = "glass.fps";

    /** 模块弹窗液态玻璃（glass.module） */
    public static final String KEY_GLASS_MODULE = "glass.module";

    /** 首页底栏液态玻璃（glass.bottom） */
    public static final String KEY_GLASS_BOTTOM = "glass.bottom";

    /** 评论面板液态玻璃（glass.comment） */
    public static final String KEY_GLASS_COMMENT = "glass.comment";

    /** 首页整理（home） */
    public static final String KEY_HOME = "home";

    /** 视频控件（controls） */
    public static final String KEY_CONTROLS = "controls";

    /** 播放背景（background） */
    public static final String KEY_BACKGROUND = "background";

    /** 作品信息显示（post_info） */
    public static final String KEY_POST_INFO = "post_info";

    /** 顶部标签居中（home.top_center） */
    public static final String KEY_HOME_TOP_CENTER = "home.top_center";

    /** 隐藏整个首页底栏（home.bottom_hidden） */
    public static final String KEY_HOME_BOTTOM_HIDDEN = "home.bottom_hidden";

    /** 隐藏底栏时保留占位（home.bottom_keep_space） */
    public static final String KEY_HOME_BOTTOM_KEEP_SPACE = "home.bottom_keep_space";

    /** 启用首页整理（home.c25b735dcd） */
    public static final String KEY_HOME_C25B735DCD = "home.c25b735dcd";

    /** 底栏图标与文字（home.cd1bda47ff） */
    public static final String KEY_HOME_CD1BDA47FF = "home.cd1bda47ff";

    /** 底栏沉浸（home.7c24a0e2fc） */
    public static final String KEY_HOME_7C24A0E2FC = "home.7c24a0e2fc";

    /** 双列与 AI 搜索入口（home.065750266b） */
    public static final String KEY_HOME_065750266B = "home.065750266b";

    /** 顶部标签（home.c4c8063945） */
    public static final String KEY_HOME_C4C8063945 = "home.c4c8063945";

    /** 底部菜单（home.3e1d8ed19a） */
    public static final String KEY_HOME_3E1D8ED19A = "home.3e1d8ed19a";

    /** 沉浸显示（home.6c47740864） */
    public static final String KEY_HOME_6C47740864 = "home.6c47740864";

    /** 透明系统导航栏（home.9902412e18） */
    public static final String KEY_HOME_9902412E18 = "home.9902412e18";

    /** 播放时隐藏系统状态栏（home.2e0ddfde95） */
    public static final String KEY_HOME_2E0DDFDE95 = "home.2e0ddfde95";

    /** 隐藏系统导航栏（home.7616bc18d9） */
    public static final String KEY_HOME_7616BC18D9 = "home.7616bc18d9";

    /** 显示双列入口（home.71d978e90b） */
    public static final String KEY_HOME_71D978E90B = "home.71d978e90b";

    /** 显示 AI 搜索入口（home.c8c0dc2845） */
    public static final String KEY_HOME_C8C0DC2845 = "home.c8c0dc2845";

    /** 隐藏侧栏入口（home.d9b79bb5a2） */
    public static final String KEY_HOME_D9B79BB5A2 = "home.d9b79bb5a2";

    /** 隐藏首页搜索（home.d23c896621） */
    public static final String KEY_HOME_D23C896621 = "home.d23c896621";

    /** 隐藏消息角标（home.3fe35ca5c8） */
    public static final String KEY_HOME_3FE35CA5C8 = "home.3fe35ca5c8";

    /** 首页按钮刷新前确认（home.6f099dad58） */
    public static final String KEY_HOME_6F099DAD58 = "home.6f099dad58";

    /** 左滑侧栏：模块设置与插眼（home.sidebar_tools） */
    public static final String KEY_HOME_SIDEBAR_TOOLS = "home.sidebar_tools";

    /** 隐藏首页底栏皮肤（home.ba08b02cee） */
    public static final String KEY_HOME_BA08B02CEE = "home.ba08b02cee";

    /** 启用控件隐藏（controls.e5681e7ff3） */
    public static final String KEY_CONTROLS_E5681E7FF3 = "controls.e5681e7ff3";

    /** 播放与暂停样式（controls.24a33047b2） */
    public static final String KEY_CONTROLS_24A33047B2 = "controls.24a33047b2";

    /** 右侧显示真实数量（controls.9aa386bb74） */
    public static final String KEY_CONTROLS_9AA386BB74 = "controls.9aa386bb74";

    /** 音乐封面随右侧栏移动（controls.a98f8afb28） */
    public static final String KEY_CONTROLS_A98F8AFB28 = "controls.a98f8afb28";

    /** 选择隐藏控件（controls.fae145829a） */
    public static final String KEY_CONTROLS_FAE145829A = "controls.fae145829a";

    /** 隐藏应用广告卡片（controls.ad_card） */
    public static final String KEY_CONTROLS_AD_CARD = "controls.ad_card";

    /** 隐藏作者声明／风险提示（controls.report_warn） */
    public static final String KEY_CONTROLS_REPORT_WARN = "controls.report_warn";

    /** 清爽模式（controls.d4f77759dc） */
    public static final String KEY_CONTROLS_D4F77759DC = "controls.d4f77759dc";

    /** 控件外观（controls.6f9739b634） */
    public static final String KEY_CONTROLS_6F9739B634 = "controls.6f9739b634";

    /** 播放背景（controls.1cdcf3fb52） */
    public static final String KEY_CONTROLS_1CDCF3FB52 = "controls.1cdcf3fb52";

    /** 可视化隐藏控件（controls.1f40c9cbb8） */
    public static final String KEY_CONTROLS_1F40C9CBB8 = "controls.1f40c9cbb8";

    /** 作品信息条（post_bar） */
    public static final String KEY_POST_BAR = "post_bar";

    private AppearancePrefs() {
    }

    public static boolean getBoolean(SharedPreferences prefs, String key, boolean def) {
        return prefs != null && prefs.getBoolean(key, def);
    }

    public static int getInt(SharedPreferences prefs, String key, int def) {
        return prefs == null ? def : prefs.getInt(key, def);
    }

    public static String getString(SharedPreferences prefs, String key, String def) {
        return prefs == null ? def : prefs.getString(key, def);
    }

    public static void putBoolean(SharedPreferences prefs, String key, boolean value) {
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
    }

    public static void putInt(SharedPreferences prefs, String key, int value) {
        if (prefs != null) prefs.edit().putInt(key, value).apply();
    }

    public static void putString(SharedPreferences prefs, String key, String value) {
        if (prefs != null) prefs.edit().putString(key, value).apply();
    }
}