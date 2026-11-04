package com.jingfei.douyin.hook;

import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * 液态玻璃（glass 组）简化实现。
 * 对评论面板 / 首页底栏 / 模块弹窗设置半透明深色背景，Android 12+ 追加 RenderEffect 模糊。
 * 完整毛玻璃（背景采样/折射/高光）依赖抖音内部渲染管线，此处提供可用的近似效果。
 */
public final class GlassUi {
    private static boolean commentEnabled;
    private static int commentBlur;
    private static int commentOpacity = 50;

    private static boolean bottomEnabled;
    private static int bottomBlur;
    private static int bottomOpacity = 50;

    private static boolean moduleEnabled;
    private static int moduleBlur;
    private static int moduleOpacity = 50;

    private static boolean topEnabled;
    private static int topBlur;
    private static int topOpacity = 50;

    private static boolean rightEnabled;
    private static int rightBlur;
    private static int rightOpacity = 50;

    private GlassUi() {
    }

    /** 评论面板液态玻璃参数（由 ThemeManager 注入 glass.comment 系列）。 */
    public static void setCommentGlass(boolean enabled, int blur, int opacity) {
        commentEnabled = enabled;
        commentBlur = blur;
        if (opacity >= 0) {
            commentOpacity = opacity;
        }
    }

    /** 首页底栏液态玻璃参数（由 ThemeManager 注入 glass.bottom 系列）。 */
    public static void setBottomGlass(boolean enabled, int blur, int opacity) {
        bottomEnabled = enabled;
        bottomBlur = blur;
        if (opacity >= 0) {
            bottomOpacity = opacity;
        }
    }

    /** 模块弹窗液态玻璃参数（由 ThemeManager 注入 glass.module 系列）。 */
    public static void setModuleGlass(boolean enabled, int blur, int opacity) {
        moduleEnabled = enabled;
        moduleBlur = blur;
        if (opacity >= 0) {
            moduleOpacity = opacity;
        }
    }

    /** 顶部导航液态玻璃参数（由 ThemeManager 注入 glass.top 系列）。 */
    public static void setTopGlass(boolean enabled, int blur, int opacity) {
        topEnabled = enabled;
        topBlur = blur;
        if (opacity >= 0) {
            topOpacity = opacity;
        }
    }

    /** 视频右侧栏液态玻璃参数（由 ThemeManager 注入 glass.right 系列）。 */
    public static void setRightGlass(boolean enabled, int blur, int opacity) {
        rightEnabled = enabled;
        rightBlur = blur;
        if (opacity >= 0) {
            rightOpacity = opacity;
        }
    }

    public static boolean isCommentEnabled() {
        return commentEnabled;
    }

    /** 对评论面板应用液态玻璃效果。 */
    public static void applyCommentGlass(View panel) {
        if (commentEnabled && panel != null) {
            applyGlass(panel, commentBlur, commentOpacity);
        }
    }

    /** 评论面板动态模糊背景（comment_appearance.2d7b8ec9a3）：不检查开关强制应用。 */
    public static void applyCommentGlassForce(View panel) {
        if (panel != null) {
            applyGlass(panel, commentBlur, commentOpacity);
        }
    }

    /** 对首页底栏应用液态玻璃效果。 */
    public static void applyBottomGlass(View bar) {
        if (bottomEnabled && bar != null) {
            applyGlass(bar, bottomBlur, bottomOpacity);
        }
    }

    /** 对模块弹窗应用液态玻璃效果。 */
    public static void applyModuleGlass(View panel) {
        if (moduleEnabled && panel != null) {
            applyGlass(panel, moduleBlur, moduleOpacity);
        }
    }

    /** 对顶部导航应用液态玻璃效果（抖X glass.top）。 */
    public static void applyTopGlass(View bar) {
        if (topEnabled && bar != null) {
            applyGlass(bar, topBlur, topOpacity);
        }
    }

    /** 对视频右侧栏应用液态玻璃效果（抖X glass.right）。 */
    public static void applyRightGlass(View bar) {
        if (rightEnabled && bar != null) {
            applyGlass(bar, rightBlur, rightOpacity);
        }
    }

    private static void applyGlass(View view, int blur, int opacity) {
        if (view == null) {
            return;
        }
        int alpha = Math.max(0, Math.min(100, opacity)) * 255 / 100;
        try {
            view.setBackgroundColor(Color.argb(alpha, 18, 18, 22));
        } catch (Throwable ignored) {
            // 视图状态异常时跳过背景设置
        }
        if (Build.VERSION.SDK_INT >= 31 && blur > 0) {
            try {
                RenderEffect effect = RenderEffect.createBlurEffect(
                        blur, blur, Shader.TileMode.CLAMP);
                view.setRenderEffect(effect);
            } catch (Throwable ignored) {
                // 低端设备不支持 RenderEffect 时仅保留半透明背景
            }
        }
    }
}
