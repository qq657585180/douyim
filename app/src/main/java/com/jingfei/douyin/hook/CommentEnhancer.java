package com.jingfei.douyin.hook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jingfei.douyin.prefs.CommentPrefs;

import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 评论增强（douyin_comment）Hook 子系统。
 * 已实现：评论关键词过滤 / 屏蔽用户 / 白名单 / 广告评论过滤；
 * 评论区 UI 隐藏：顶部广告卡片、相关搜索、底部输入栏、回复按钮。
 * 通过 Activity 生命周期驱动主线程定时扫描评论区视图，命中规则直接隐藏。
 */
final class CommentEnhancer {
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;
    private static volatile WeakReference<View> cachedPanel;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Runnable SCAN = CommentEnhancer::scanComments;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile boolean scanning;
    /** 评论面板当前是否处于打开状态（用于检测打开瞬间）。 */
    private static volatile boolean commentPanelOpen;

    /** 剪贴板守卫：复制评论时去除艾特前缀（抖X comments.b213d022be）。 */
    private static volatile ClipboardManager clipboard;
    private static volatile ClipboardManager.OnPrimaryClipChangedListener clipGuard;
    private static volatile String lastClipText;

    /** 评论区子项的时间/操作文本特征，用于区分评论项与视频项 */
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "刚刚|分钟前|小时前|天前|昨天|回复|赞");

    /** 评论时间文本特征（仅时间，用于隐藏评论时间控件，避免误伤含"回复/赞"的正文） */
    private static final Pattern TIME_ONLY_PATTERN = Pattern.compile(
            "刚刚|分钟前|小时前|天前|昨天");

    /** 评论时间格式化完成标记的 View tag key（任意不冲突的 int） */
    private static final int COMMENT_TIME_TAG_KEY = 0x2d0a5e01;

    /** 评论图片长按保存的 View tag key */
    private static final int COMMENT_IMAGE_LONGPRESS_TAG_KEY = 0x2d0a5e02;

    /** 内置广告评论特征词 */
    private static final String[] AD_HINTS = {
            "加v", "加vx", "加微信", "vx:", "vx：", "微信", "qq群", "扣扣",
            "淘宝", "拼多多", "刷单", "兼职", "代购", "返利", "点赞关注",
            "关注我", "私信我", "主页有", "橱窗", "低价", "限时优惠"
    };

    private CommentEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_comment");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            // 评论准确点赞数（comments.70151eae18）→ ExactNumbers
            ExactNumbers.setCommentEnabled(CommentPrefs.getBoolean(
                    p, CommentPrefs.KEY_COMMENTS_70151EAE18, false));
            module.log(Log.INFO, DouyinModule.TAG,
                    "CommentEnhancer installed (douyin_comment)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "CommentEnhancer skipped: " + error);
        }
    }

    /** 由 DouyinModule 的 Activity onResume 拦截调用 */
    static void onActivityResumed(Activity activity) {
        if (activity == null) {
            return;
        }
        currentActivity = new WeakReference<>(activity);
        if (!scanning) {
            scanning = true;
            HANDLER.postDelayed(SCAN, 500L);
        }
    }

    /** 由 DouyinModule 的 Activity onPause 拦截调用 */
    static void onActivityPaused(Activity activity) {
        if (currentActivity != null && currentActivity.get() == activity) {
            currentActivity = null;
        }
        commentPanelOpen = false;
        unregisterClipboardGuard();
    }

    /** 注册剪贴板守卫：评论面板打开期间，复制文本去除艾特前缀。 */
    private static void registerClipboardGuard(Activity activity) {
        if (activity == null || clipGuard != null) {
            return;
        }
        clipboard = (ClipboardManager) activity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipGuard = () -> {
            try {
                if (!commentPanelOpen) {
                    return;
                }
                CharSequence cs = clipboard.getPrimaryClip() == null
                        || clipboard.getPrimaryClip().getItemCount() == 0
                        ? null
                        : clipboard.getPrimaryClip().getItemAt(0)
                                .coerceToText(clipboard.getContext());
                if (cs == null || cs.length() == 0) {
                    return;
                }
                String text = cs.toString();
                if (text.equals(lastClipText)) {
                    return;
                }
                String stripped = stripAtPrefix(text);
                if (stripped == null || stripped.equals(text)) {
                    lastClipText = text;
                    return;
                }
                lastClipText = stripped;
                clipboard.setPrimaryClip(
                        ClipData.newPlainText("comment", stripped));
            } catch (Throwable ignored) {
                // 剪贴板权限或异常时忽略
            }
        };
        clipboard.addPrimaryClipChangedListener(clipGuard);
    }

    private static void unregisterClipboardGuard() {
        if (clipboard != null && clipGuard != null) {
            try {
                clipboard.removePrimaryClipChangedListener(clipGuard);
            } catch (Throwable ignored) {
                // 忽略注销异常
            }
        }
        clipboard = null;
        clipGuard = null;
        lastClipText = null;
    }

    /** 去除评论复制文本开头的艾特前缀：@昵称 正文 → 正文。 */
    private static String stripAtPrefix(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("@")) {
            return text;
        }
        int i = 1;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r'
                    || c == '：' || c == ':') {
                break;
            }
            i++;
        }
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r'
                    && c != '：' && c != ':') {
                break;
            }
            i++;
        }
        String result = trimmed.substring(i);
        return result.isEmpty() ? text : result;
    }

    private static void scanComments() {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scanning = false;
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                View panel = resolveCommentPanel(decor);
                if (panel != null) {
                    // 检测评论面板打开瞬间：暂停视频（抖X comment_interaction.6ef5a30dbc）
                    if (!commentPanelOpen) {
                        commentPanelOpen = true;
                        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_6EF5A30DBC, false)) {
                            PlaybackState.pauseForComments();
                        }
                    }
                    // 空评论不自动弹出输入框（抖X comment_interaction.c0ce370b93）
                    if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_C0CE370B93, false)
                            && !hasCommentItems(panel)) {
                        suppressEmptyInput(activity, panel);
                    }
                    // 打开评论不缩放视频（抖X comment_interaction.6346a6d3d4）
                    if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_6346A6D3D4, false)) {
                        resetVideoScale(decor);
                    }
                    // 打开评论作品不平移（抖X comment_interaction.disable_shift）
                    if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_DISABLE_SHIFT, false)) {
                        resetShift(decor);
                    }
                    // 评论点击守卫：禁用回复 + 点赞/点踩前确认（抖X fb9a1f8613 / f89dd25379 / 9d018d123f）
                    installCommentGuards(activity, panel);
                    // 评论区外观：文字颜色 / 自定义背景（抖X comment_appearance.*）
                    applyCommentAppearance(panel);
                    // 评论准确点赞数（抖X comments.70151eae18）
                    ExactNumbers.scanComments(panel);
                    // 评论时间格式（抖X comments.96c0e404c6 / comment.time）
                    applyCommentTimeFormat(panel);
                    // 评论图片长按保存（抖X comments.image_original）
                    installCommentImageLongClick(panel);
                    // 评论面板高度（抖X comment_appearance.60c73f44b9）
                    applyPanelHeight(panel);
                    // 评论图片缩放（抖X comment_appearance.37dc6e6be5）
                    applyCommentImageScale(panel);
                    // 评论控件隐藏（抖X comment.control.*）
                    applyCommentControlVisibility(panel);
                    // 长按查看 IP 属地（抖X comment.region）
                    installRegionLongClick(panel);
                    // 评论项长按菜单（抖X comments.cc7c36fa92）
                    installCommentLongClickMenu(panel);
                    // 复制评论去除艾特前缀（抖X comments.b213d022be）
                    registerClipboardGuard(activity);
                } else {
                    commentPanelOpen = false;
                    unregisterClipboardGuard();
                }
                View target = panel != null ? panel : decor;
                applyUiVisibility(target);
                scanView(target);
            }
        } catch (Throwable ignored) {
            // 视图结构变化或权限异常时跳过本轮
        }
        HANDLER.postDelayed(SCAN, SCAN_INTERVAL_MS);
    }

    /** 复用缓存的评论面板；失效时重新在视图树中定位 */
    private static View resolveCommentPanel(View decor) {
        if (cachedPanel != null) {
            View cached = cachedPanel.get();
            if (cached != null && cached.isShown()) {
                return cached;
            }
        }
        View found = findCommentPanel(decor);
        if (found != null) {
            cachedPanel = new WeakReference<>(found);
        }
        return found;
    }

    /** 启发式定位评论面板：同时包含输入框与列表，或含"评论"文本与列表 */
    private static View findCommentPanel(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            boolean hasInput = containsInstanceOf(group, EditText.class);
            boolean hasRecycler = containsRecyclerView(group);
            boolean hasCommentTitle = containsText(group, "评论");
            if ((hasInput && hasRecycler) || (hasCommentTitle && hasRecycler)) {
                return view;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findCommentPanel(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 评论区 UI 隐藏：广告/相关搜索/底部输入栏/回复按钮/分页加载/赞踩按钮 */
    private static void applyUiVisibility(View panel) {
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_EA45E2F234, false)) {
            hideByText(panel, "广告", 2, true);
            hideByText(panel, "相关搜索", 1, true);
        }
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_AE313F9A5C, false)) {
            hideInputBar(panel);
        }
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_51E3399312, false)) {
            hideByText(panel, "回复", 1, false);
        }
        // 评论分页隐藏（抖X comments.84f54a63d8）：隐藏“查看更多/加载更多/查看全部评论”
        if (bool(CommentPrefs.KEY_COMMENTS_84F54A63D8, false)) {
            hideByText(panel, "查看更多", 1, true);
            hideByText(panel, "加载更多", 1, true);
            hideByText(panel, "查看全部评论", 1, true);
        }
        // 隐藏点赞/点踩按钮（抖X comment_interaction.1ffb4cb3f8 / 008507accf）
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_1FFB4CB3F8, false)) {
            hideByText(panel, "赞", 1, false);
        }
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_008507ACCF, false)) {
            hideByText(panel, "踩", 1, false);
        }
        // 隐藏“我的评论”入口（抖X comments.753d08c43d）
        if (bool(CommentPrefs.KEY_COMMENTS_753D08C43D, false)) {
            hideByText(panel, "我的评论", 1, false);
        }
        // 隐藏宿主隐藏的小表情（抖X comment.show_hidden_emoji）
        if (bool(CommentPrefs.KEY_COMMENT_SHOW_HIDDEN_EMOJI, false)) {
            hideSmallEmojiViews(panel);
        }
        // 发布蓝字评论（抖X comment_interaction.ea615e3e1d）：发布按钮文字变蓝
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_EA615E3E1D, false)) {
            setPublishButtonBlue(panel);
        }
        // 始终显示位置入口（抖X comment_interaction.73cc3f4abf）
        if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_73CC3F4ABF, false)) {
            showLocationEntry(panel);
        }
    }

    /** 发布按钮文字变蓝。 */
    private static void setPublishButtonBlue(View root) {
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            CharSequence cs = tv.getText();
            if (cs != null && cs.toString().trim().equals("发布")) {
                tv.setTextColor(0xFF1E90FF);
                return;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                setPublishButtonBlue(g.getChildAt(i));
            }
        }
    }

    /** 显示输入栏的位置入口按钮（默认可能隐藏）。 */
    private static void showLocationEntry(View root) {
        CharSequence desc = root.getContentDescription();
        if (desc != null && desc.toString().contains("位置")
                && root.getVisibility() != View.VISIBLE) {
            root.setVisibility(View.VISIBLE);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                showLocationEntry(g.getChildAt(i));
            }
        }
    }

    /** 隐藏评论项中的小表情视图：评论面板内 10-32dp 的方形小 ImageView。 */
    private static void hideSmallEmojiViews(View root) {
        if (root instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) root;
            int w = iv.getWidth();
            int h = iv.getHeight();
            if (w > 0 && h > 0) {
                float density = iv.getResources().getDisplayMetrics().density;
                int minPx = Math.round(10 * density);
                int maxPx = Math.round(32 * density);
                if (w >= minPx && w <= maxPx && h >= minPx && h <= maxPx
                        && Math.abs(w - h) <= Math.max(2, Math.round(2 * density))) {
                    // 避免隐藏头像：头像一般在评论项左侧且尺寸更大，方形小图视为表情
                    if (iv.getVisibility() == View.VISIBLE) {
                        iv.setVisibility(View.GONE);
                    }
                }
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                hideSmallEmojiViews(g.getChildAt(i));
            }
        }
    }

    /** 评论区外观：评论文字颜色 + 自定义评论背景（颜色/透明度）。
     *  仅当用户在抖X显式开启对应设置时生效，每轮扫描实时应用。 */
    private static void applyCommentAppearance(View panel) {
        if (panel == null) {
            return;
        }
        // 评论文字颜色（comment_appearance.503606eb6d）
        String colorText = string(CommentPrefs.KEY_COMMENT_APPEARANCE_503606EB6D, null);
        if (colorText != null && !colorText.isEmpty()) {
            try {
                setCommentTextColor(panel, Color.parseColor(colorText));
            } catch (IllegalArgumentException ignored) {
                // 非法颜色串忽略
            }
        } else if (prefs != null
                && prefs.contains(CommentPrefs.KEY_COMMENT_APPEARANCE_503606EB6D)) {
            int color = prefs.getInt(
                    CommentPrefs.KEY_COMMENT_APPEARANCE_503606EB6D, 0);
            if (color != 0) {
                setCommentTextColor(panel, color);
            }
        }
        // 自定义评论背景（comment_appearance.ebbd320f5f + 882fe2e842 + 9eaa61edf0）
        if (bool(CommentPrefs.KEY_COMMENT_APPEARANCE_EBBD320F5F, false)) {
            int bgColor = 0;
            String bgText = string(
                    CommentPrefs.KEY_COMMENT_APPEARANCE_882FE2E842, null);
            if (bgText != null && !bgText.isEmpty()) {
                try {
                    bgColor = Color.parseColor(bgText);
                } catch (IllegalArgumentException ignored) {
                    // 非法颜色串忽略
                }
            } else {
                bgColor = integer(
                        CommentPrefs.KEY_COMMENT_APPEARANCE_882FE2E842, 0);
            }
            int opacity = integer(
                    CommentPrefs.KEY_COMMENT_APPEARANCE_9EAA61EDF0, 100);
            if (opacity >= 0 && opacity <= 100) {
                bgColor = (bgColor & 0x00FFFFFF) | ((opacity * 255 / 100) << 24);
            }
            if (bgColor != 0) {
                panel.setBackgroundColor(bgColor);
            }
        }
        // 评论面板透明度（抖X comment.control.panel_opacity，0-100）
        int panelOpacity = integer(CommentPrefs.KEY_COMMENT_CONTROL_PANEL_OPACITY, -1);
        if (panelOpacity >= 0 && panelOpacity <= 100) {
            android.graphics.drawable.Drawable bg = panel.getBackground();
            if (bg != null) {
                bg.setAlpha(panelOpacity * 255 / 100);
            } else {
                panel.setAlpha(panelOpacity / 100f);
            }
        }
        // 评论面板液态玻璃（glass.comment）：最后应用，覆盖自定义背景
        GlassUi.applyCommentGlass(panel);
        // 动态模糊背景（comment_appearance.2d7b8ec9a3）：独立于 glass.comment 强制模糊
        if (bool(CommentPrefs.KEY_COMMENT_APPEARANCE_2D7B8EC9A3, false)) {
            GlassUi.applyCommentGlassForce(panel);
        }
    }

    /** 评论面板高度（抖X comment_appearance.60c73f44b9，值为屏幕高度百分比）。 */
    private static void applyPanelHeight(View panel) {
        if (panel == null) {
            return;
        }
        int pct = integer(CommentPrefs.KEY_COMMENT_APPEARANCE_60C73F44B9, 0);
        if (pct <= 0 || pct > 100) {
            return;
        }
        ViewGroup.LayoutParams lp = panel.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int screenHeight = panel.getResources().getDisplayMetrics().heightPixels;
        lp.height = screenHeight * pct / 100;
        panel.setLayoutParams(lp);
    }

    /** 评论图片缩放（抖X comment_appearance.37dc6e6be5，值为百分比）。 */
    private static void applyCommentImageScale(View panel) {
        if (panel == null) {
            return;
        }
        int pct = integer(CommentPrefs.KEY_COMMENT_APPEARANCE_37DC6E6BE5, 0);
        if (pct <= 0 || pct == 100) {
            return;
        }
        int screenWidth = panel.getResources().getDisplayMetrics().widthPixels;
        scaleImagesRecursive(panel, pct, screenWidth);
    }

    /** 递归缩放评论大图（宽度超过屏幕 30% 的 ImageView，头像等小图不动）。 */
    private static void scaleImagesRecursive(View view, int pct, int screenWidth) {
        if (view instanceof ImageView) {
            ImageView image = (ImageView) view;
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > 0 && height > 0
                    && width > screenWidth * 0.3f) {
                ViewGroup.LayoutParams lp = image.getLayoutParams();
                if (lp != null) {
                    int newWidth = width * pct / 100;
                    int newHeight = height * pct / 100;
                    if (newWidth > 0 && newHeight > 0
                            && (lp.width != newWidth || lp.height != newHeight)) {
                        lp.width = newWidth;
                        lp.height = newHeight;
                        image.setLayoutParams(lp);
                    }
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scaleImagesRecursive(group.getChildAt(i), pct, screenWidth);
            }
        }
    }

    /** 评论时间格式（抖X comments.96c0e404c6 模板 + comment.time 完整时间）。
     *  通过 RecyclerView#getChildViewHolder 反射拿到评论 ViewHolder 的
     *  getComment()/getCreateTime()，把"x分钟前"替换为完整时间。 */
    private static void applyCommentTimeFormat(View panel) {
        if (panel == null) {
            return;
        }
        String template = string(CommentPrefs.KEY_COMMENTS_96C0E404C6, null);
        boolean fullTime = bool(CommentPrefs.KEY_COMMENT_TIME_DOT, false);
        if ((template == null || template.isEmpty()) && !fullTime) {
            return;
        }
        String pattern = (template != null && !template.isEmpty())
                ? template : "yyyy-MM-dd HH:mm";
        final SimpleDateFormat sdf;
        try {
            sdf = new SimpleDateFormat(pattern, Locale.CHINA);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        try {
            Method getChildViewHolder =
                    rv.getClass().getMethod("getChildViewHolder", View.class);
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child == null || child.getVisibility() != View.VISIBLE
                        || child.getTag(COMMENT_TIME_TAG_KEY) != null) {
                    continue;
                }
                Object holder = getChildViewHolder.invoke(rv, child);
                long createTime = readCommentCreateTime(holder);
                if (createTime <= 0) {
                    continue;
                }
                TextView timeView = findTimeTextView(child);
                if (timeView == null) {
                    continue;
                }
                long millis = createTime;
                if (millis < 1_000_000_000_000L) {
                    millis *= 1000L;
                }
                timeView.setText(sdf.format(new Date(millis)));
                child.setTag(COMMENT_TIME_TAG_KEY, Boolean.TRUE);
            }
        } catch (Throwable ignored) {
            // 反射失败（评论结构变化）时本轮跳过
        }
    }

    /** 反射调用 ViewHolder.getComment() 再取 getCreateTime()（秒）。 */
    private static long readCommentCreateTime(Object holder) {
        if (holder == null) {
            return 0L;
        }
        try {
            Method getComment = holder.getClass().getMethod("getComment");
            Object comment = getComment.invoke(holder);
            if (comment == null) {
                return 0L;
            }
            Method getCreateTime = comment.getClass().getMethod("getCreateTime");
            Object value = getCreateTime.invoke(comment);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** 在评论项内查找时间文本控件（"刚刚/x分钟前/昨天"）。 */
    private static TextView findTimeTextView(View view) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            CharSequence cs = tv.getText();
            if (cs != null && TIME_ONLY_PATTERN.matcher(cs.toString()).find()) {
                return tv;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTimeTextView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 评论图片长按保存到相册（抖X comments.image_original）。
     *  对评论大图设置长按监听，弹出确认后把当前显示的 Bitmap 写入系统相册。 */
    private static void installCommentImageLongClick(View panel) {
        if (panel == null
                || !bool(CommentPrefs.KEY_COMMENTS_IMAGE_ORIGINAL, false)) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child != null && child.getVisibility() == View.VISIBLE) {
                applyImageLongClick(child);
            }
        }
    }

    private static void applyImageLongClick(View view) {
        if (view instanceof ImageView) {
            ImageView image = (ImageView) view;
            if (image.getTag(COMMENT_IMAGE_LONGPRESS_TAG_KEY) != null) {
                return;
            }
            int width = image.getWidth();
            int screenWidth = image.getResources()
                    .getDisplayMetrics().widthPixels;
            if (width <= 0 || width < screenWidth * 0.3f) {
                return; // 只处理评论大图，头像等小图不动
            }
            image.setTag(COMMENT_IMAGE_LONGPRESS_TAG_KEY, Boolean.TRUE);
            image.setOnLongClickListener(v -> {
                saveCommentImage(v);
                return true;
            });
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyImageLongClick(group.getChildAt(i));
            }
        }
    }

    private static void saveCommentImage(View v) {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || !(v instanceof ImageView)) {
            return;
        }
        android.graphics.drawable.Drawable drawable =
                ((ImageView) v).getDrawable();
        if (!(drawable instanceof BitmapDrawable)) {
            return;
        }
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap == null) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("保存评论图片")
                .setMessage("把这张图片保存到系统相册吗？")
                .setPositiveButton("保存", (d, w) ->
                        saveBitmapToGallery(activity, bitmap))
                .setNegativeButton("取消", null)
                .show();
    }

    private static void saveBitmapToGallery(Activity activity, Bitmap bitmap) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                    "douyin_comment_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            Uri uri = activity.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = activity.getContentResolver()
                        .openOutputStream(uri);
                if (os != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os);
                    os.close();
                }
            }
        } catch (Throwable ignored) {
            // 写入相册失败（权限/存储异常）时静默忽略
        }
    }

    /** 评论控件隐藏（抖X comment.control.*）：
     *  在评论列表的每个评论项内，按开关隐藏头像/昵称/正文/时间/IP 属地。 */
    private static void applyCommentControlVisibility(View panel) {
        if (panel == null) {
            return;
        }
        boolean hideAvatar = bool(CommentPrefs.KEY_COMMENT_CONTROL_AVATAR, false);
        boolean hideNickname = bool(CommentPrefs.KEY_COMMENT_CONTROL_NICKNAME, false);
        boolean hideText = bool(CommentPrefs.KEY_COMMENT_CONTROL_TEXT, false);
        boolean hideTime = bool(CommentPrefs.KEY_COMMENT_CONTROL_TIME, false);
        boolean hideIp = bool(CommentPrefs.KEY_COMMENT_CONTROL_IP, false);
        if (!hideAvatar && !hideNickname && !hideText && !hideTime && !hideIp) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child != null && child.getVisibility() == View.VISIBLE
                    && isCommentItem(child)) {
                applyControlOnItem(child,
                        hideAvatar, hideNickname, hideText, hideTime, hideIp);
            }
        }
    }

    /** 长按查看 IP 属地（抖X comment.region）：长按评论属地文本弹出完整属地并可复制。 */
    private static void installRegionLongClick(View panel) {
        if (panel == null || !bool(CommentPrefs.KEY_COMMENT_REGION, false)) {
            return;
        }
        // 属地已被"隐藏 IP 属地"隐藏时无需安装
        if (bool(CommentPrefs.KEY_COMMENT_CONTROL_IP, false)) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child != null && child.getVisibility() == View.VISIBLE
                    && isCommentItem(child)) {
                applyRegionLongClickOnItem(child);
            }
        }
    }

    /** 为单个评论项中的属地文本安装长按监听。 */
    private static void applyRegionLongClickOnItem(View item) {
        if (!(item instanceof ViewGroup)) {
            return;
        }
        List<View> textViews = new ArrayList<>();
        collectTextViews(item, textViews);
        for (View tv : textViews) {
            if (tv.getVisibility() != View.VISIBLE) {
                continue;
            }
            String text = ((TextView) tv).getText().toString();
            if (isLocationText(text)) {
                tv.setOnLongClickListener(v -> {
                    showRegionDialog(tv.getContext(), text);
                    return true;
                });
            }
        }
    }

    /** 弹出属地对话框，支持一键复制。 */
    private static void showRegionDialog(Context context, String region) {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("IP 属地")
                .setMessage(region)
                .setPositiveButton("复制", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("IP属地", region));
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    /** 单个评论项内按开关隐藏对应控件。 */
    private static void applyControlOnItem(View item,
                                           boolean hideAvatar, boolean hideNickname,
                                           boolean hideText, boolean hideTime,
                                           boolean hideIp) {
        if (!(item instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) item;
        List<View> textViews = new ArrayList<>();
        collectTextViews(group, textViews);
        List<View> imageViews = new ArrayList<>();
        collectImageViews(group, imageViews);

        // 昵称：评论项内第一个文本控件（通常位于头像右侧）
        if (hideNickname && !textViews.isEmpty()) {
            setControlVisibility(textViews.get(0), View.GONE);
        }
        // 时间 / IP 属地：按文本特征匹配
        if (hideTime || hideIp) {
            for (View tv : textViews) {
                if (tv.getVisibility() != View.VISIBLE) {
                    continue;
                }
                String text = ((TextView) tv).getText().toString();
                if (hideTime && TIME_ONLY_PATTERN.matcher(text).find()) {
                    setControlVisibility(tv, View.GONE);
                } else if (hideIp && isLocationText(text)) {
                    setControlVisibility(tv, View.GONE);
                }
            }
        }
        // 正文：评论项内最宽的可见文本控件
        if (hideText) {
            View best = null;
            int bestWidth = -1;
            for (View tv : textViews) {
                if (tv.getVisibility() != View.VISIBLE) {
                    continue;
                }
                if (tv.getWidth() > bestWidth) {
                    bestWidth = tv.getWidth();
                    best = tv;
                }
            }
            if (best != null) {
                setControlVisibility(best, View.GONE);
            }
        }
        // 头像：评论项内第一个图片控件（通常为圆形头像）
        if (hideAvatar && !imageViews.isEmpty()) {
            setControlVisibility(imageViews.get(0), View.GONE);
        }
    }

    /** 是否属地文本：来自xx / IP属地 / 含"·"的位置后缀（如"来自 广东"）。 */
    private static boolean isLocationText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        return t.contains("来自") || t.contains("IP属地")
                || t.contains("属地") || t.startsWith("·");
    }

    private static void setControlVisibility(View view, int visibility) {
        if (view != null && view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    private static void collectTextViews(View view, List<View> out) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            out.add(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTextViews(group.getChildAt(i), out);
            }
        }
    }

    private static void collectImageViews(View view, List<View> out) {
        if (view == null) {
            return;
        }
        if (view instanceof ImageView) {
            out.add(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectImageViews(group.getChildAt(i), out);
            }
        }
    }

    /** 递归设置评论项内所有文字控件的颜色 */
    private static void setCommentTextColor(View view, int color) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setCommentTextColor(group.getChildAt(i), color);
            }
        }
    }

    /** 隐藏评论底部输入栏：找到 EditText，向上定位包含发送按钮的容器隐藏 */
    private static void hideInputBar(View panel) {
        View edit = findInstance(panel, EditText.class);
        if (edit == null) {
            return;
        }
        View cur = edit;
        while (cur.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) cur.getParent();
            if (countClickable(parent) >= 2) {
                if (parent.getVisibility() == View.VISIBLE) {
                    parent.setVisibility(View.GONE);
                }
                return;
            }
            cur = parent;
        }
        ViewGroup direct = (ViewGroup) edit.getParent();
        if (direct != null && direct.getVisibility() == View.VISIBLE) {
            direct.setVisibility(View.GONE);
        }
    }

    private static int countClickable(View view) {
        int count = view.isClickable() ? 1 : 0;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countClickable(group.getChildAt(i));
            }
        }
        return count;
    }

    /** 评论区是否存在评论项（用于“空评论不自动弹出输入框”判断）。 */
    private static boolean hasCommentItems(View view) {
        if (isRecyclerView(view)) {
            ViewGroup rv = (ViewGroup) view;
            for (int i = 0; i < rv.getChildCount(); i++) {
                View child = rv.getChildAt(i);
                if (child != null && isCommentItem(child)) {
                    return true;
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasCommentItems(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 打开评论不缩放视频：将评论区打开时被缩放的视图恢复为原始比例。
     *  仅当用户开启 comment_interaction.6346a6d3d4 时每轮强制重置，
     *  使评论区打开期间视频保持不缩放。 */
    private static void resetVideoScale(View view) {
        if (view == null) {
            return;
        }
        if (view.getScaleX() < 1.0f || view.getScaleY() < 1.0f) {
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                resetVideoScale(group.getChildAt(i));
            }
        }
    }

    /** 打开评论作品不平移：将评论区打开时上移的作品视图恢复原位。
     *  仅当用户开启 comment_interaction.disable_shift 时每轮强制重置。 */
    private static void resetShift(View view) {
        if (view == null) {
            return;
        }
        if (view.getTranslationY() != 0f) {
            view.setTranslationY(0f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                resetShift(group.getChildAt(i));
            }
        }
    }

    /** 评论项长按菜单（抖X comments.cc7c36fa92）。
     *  长按评论项弹出操作菜单：复制评论文字 / 复制评论链接 / 屏蔽该用户 / 复制昵称。 */
    private static final int COMMENT_MENU_TAG_KEY = 0x2d0a5e03;

    private static void installCommentLongClickMenu(View panel) {
        if (panel == null
                || !bool(CommentPrefs.KEY_COMMENTS_CC7C36FA92, false)) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        for (int i = 0; i < group.getChildCount(); i++) {
            final View child = group.getChildAt(i);
            if (child == null || child.getTag(COMMENT_MENU_TAG_KEY) != null) {
                continue;
            }
            child.setTag(COMMENT_MENU_TAG_KEY, Boolean.TRUE);
            child.setOnLongClickListener(v -> {
                showCommentMenu(child);
                return true;
            });
        }
    }

    private static void showCommentMenu(final View commentView) {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || commentView == null) {
            return;
        }
        final String text = findCommentText(commentView);
        final String nickname = findNicknameText(commentView);
        final List<String> menu = new ArrayList<>();
        if (text.length() > 0) {
            menu.add("复制评论");
        }
        if (nickname.length() > 0) {
            menu.add("复制昵称");
        }
        menu.add("屏蔽该用户");
        menu.add("复制评论链接");
        final String[] items = menu.toArray(new String[0]);
        new AlertDialog.Builder(activity)
                .setTitle("评论操作")
                .setItems(items, (d, which) -> {
                    String chosen = items[which];
                    if ("复制评论".equals(chosen)) {
                        copyText(activity, text);
                        toast(activity, "评论已复制");
                    } else if ("复制昵称".equals(chosen)) {
                        copyText(activity, nickname);
                        toast(activity, "昵称已复制");
                    } else if ("屏蔽该用户".equals(chosen)) {
                        blockCommentUser(nickname);
                        toast(activity, "已屏蔽该用户");
                    } else if ("复制评论链接".equals(chosen)) {
                        copyText(activity, buildCommentLink(commentView));
                        toast(activity, "链接已复制");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 找评论项中的正文文本：取宽度最大的 TextView。 */
    private static String findCommentText(View view) {
        if (view == null) {
            return "";
        }
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            return cs == null ? "" : cs.toString().trim();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            String best = "";
            int bestWidth = -1;
            for (int i = 0; i < group.getChildCount(); i++) {
                String s = findCommentText(group.getChildAt(i));
                if (s.length() > 0 && group.getChildAt(i).getWidth() > bestWidth) {
                    best = s;
                    bestWidth = group.getChildAt(i).getWidth();
                }
            }
            return best;
        }
        return "";
    }

    /** 找评论项中的昵称：第一个短文本 TextView（通常位于评论正文上方/左侧）。 */
    private static String findNicknameText(View view) {
        if (view == null) {
            return "";
        }
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            String s = cs == null ? "" : cs.toString().trim();
            if (s.length() > 0 && s.length() <= 32
                    && view.getHeight() > 0 && view.getHeight() < 200) {
                return s;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                String s = findNicknameText(group.getChildAt(i));
                if (s.length() > 0) {
                    return s;
                }
            }
        }
        return "";
    }

    /** 屏蔽用户：把昵称追加到评论屏蔽列表（逗号分隔）。 */
    private static void blockCommentUser(String nickname) {
        if (prefs == null || nickname == null || nickname.length() == 0) {
            return;
        }
        String existing = prefs.getString(
                CommentPrefs.KEY_COMMENT_FILTER_C28BBE3819, "");
        if (existing != null && existing.contains(nickname)) {
            return;
        }
        String merged = existing == null || existing.trim().length() == 0
                ? nickname : existing + "," + nickname;
        prefs.edit().putString(
                CommentPrefs.KEY_COMMENT_FILTER_C28BBE3819, merged).apply();
    }

    /** 构造评论链接：从 ViewHolder 的 Comment 反射取 awemeId/cid。 */
    private static String buildCommentLink(View child) {
        try {
            Object comment = getCommentFromHolder(child);
            if (comment != null) {
                String aid = callCommentGetter(comment, "getAwemeId");
                String cid = callCommentGetter(comment, "getCid");
                if (aid.length() > 0) {
                    return "https://www.douyin.com/video/" + aid
                            + (cid.length() > 0 ? "?comment_id=" + cid : "");
                }
            }
        } catch (Throwable ignored) {
        }
        return "https://www.douyin.com/";
    }

    private static Object getCommentFromHolder(View child) {
        View rv = child;
        while (rv != null) {
            if (rv.getClass().getName().contains("RecyclerView")) {
                break;
            }
            rv = rv.getParent() instanceof View
                    ? (View) rv.getParent() : null;
        }
        if (rv == null) {
            return null;
        }
        try {
            Method m = rv.getClass()
                    .getMethod("getChildViewHolder", View.class);
            Object holder = m.invoke(rv, child);
            if (holder == null) {
                return null;
            }
            Method getComment = holder.getClass().getMethod("getComment");
            return getComment.invoke(holder);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String callCommentGetter(Object comment, String name) {
        try {
            Method m = comment.getClass().getMethod(name);
            Object v = m.invoke(comment);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void copyText(Activity activity, String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        ClipboardManager cm = (ClipboardManager) activity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("douyin_comment", text));
        }
    }

    private static void toast(Activity activity, String message) {
        try {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    /** 禁用点击评论回复：在评论列表 RecyclerView 上拦截单击，
     *  命中评论项时消费事件，阻止展开回复（抖X comment_interaction.fb9a1f8613）。 */
    private static final Object GUARD_TAG = new Object();

    /** 评论点击守卫：禁用点击回复 + 点赞/点踩前确认（一次安装统一触摸监听）。 */
    private static void installCommentGuards(Activity activity, View panel) {
        if (activity == null || panel == null) {
            return;
        }
        boolean guardReply = bool(
                CommentPrefs.KEY_COMMENT_INTERACTION_FB9A1F8613, false);
        boolean confirmLike = bool(
                CommentPrefs.KEY_COMMENT_INTERACTION_F89DD25379, false);
        boolean confirmDislike = bool(
                CommentPrefs.KEY_COMMENT_INTERACTION_9D018D123F, false);
        if (!guardReply && !confirmLike && !confirmDislike) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (rv == null || GUARD_TAG.equals(rv.getTag())) {
            return;
        }
        rv.setTag(GUARD_TAG);
        GestureDetector detector = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        if (confirmLike || confirmDislike) {
                            View button = findActionButton(rv, e,
                                    confirmLike, confirmDislike);
                            if (button != null) {
                                confirmCommentAction(activity, button);
                                return true;
                            }
                        }
                        return guardReply && findHitComment(rv, e) != null;
                    }
                });
        rv.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
    }

    /** 命中坐标内的点赞/点踩按钮（ImageView，contentDescription 含"赞"/"踩"）。 */
    private static View findActionButton(View rv, MotionEvent e,
                                         boolean wantLike, boolean wantDislike) {
        if (!(rv instanceof ViewGroup)) {
            return null;
        }
        int[] rvLoc = new int[2];
        rv.getLocationOnScreen(rvLoc);
        int absX = rvLoc[0] + (int) e.getX();
        int absY = rvLoc[1] + (int) e.getY();
        return findActionButtonIn(rv, absX, absY, wantLike, wantDislike);
    }

    private static View findActionButtonIn(View view, int absX, int absY,
                                           boolean wantLike, boolean wantDislike) {
        if (view instanceof android.widget.ImageView) {
            CharSequence desc = ((android.widget.ImageView) view)
                    .getContentDescription();
            if (desc != null && desc.length() > 0) {
                String d = desc.toString().toLowerCase(Locale.ROOT);
                boolean like = d.contains("赞") || d.contains("like");
                boolean dislike = d.contains("踩") || d.contains("dislike");
                if ((like && wantLike) || (dislike && wantDislike)) {
                    int[] loc = new int[2];
                    view.getLocationOnScreen(loc);
                    if (absX >= loc[0] && absX <= loc[0] + view.getWidth()
                            && absY >= loc[1] && absY <= loc[1] + view.getHeight()) {
                        return view;
                    }
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findActionButtonIn(group.getChildAt(i),
                        absX, absY, wantLike, wantDislike);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 弹确认框：确认后执行按钮原有点击逻辑。 */
    private static void confirmCommentAction(Activity activity, View button) {
        if (activity == null || button == null) {
            return;
        }
        CharSequence desc = (button instanceof android.widget.ImageView)
                ? ((android.widget.ImageView) button).getContentDescription()
                : null;
        String action = (desc != null
                && desc.toString().toLowerCase(Locale.ROOT).contains("踩"))
                ? "点踩" : "点赞";
        new AlertDialog.Builder(activity)
                .setTitle("评论" + action + "确认")
                .setMessage("确定要" + action + "这条评论吗？")
                .setPositiveButton("确定", (d, w) -> button.performClick())
                .setNegativeButton("取消", null)
                .show();
    }

    private static View findRecyclerView(View view) {
        if (isRecyclerView(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findRecyclerView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 判断评论列表上的点击是否落在某个评论项内 */
    private static View findHitComment(View rv, MotionEvent e) {
        if (!(rv instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) rv;
        int x = (int) e.getX();
        int y = (int) e.getY();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (x >= child.getLeft() && x <= child.getRight()
                    && y >= child.getTop() && y <= child.getBottom()
                    && isCommentItem(child)) {
                return child;
            }
        }
        return null;
    }

    /** 空评论时抑制输入框自动弹出：清除焦点并收起软键盘。 */
    private static void suppressEmptyInput(Activity activity, View panel) {
        View edit = findInstance(panel, EditText.class);
        if (edit == null || !edit.hasFocus()) {
            return;
        }
        edit.clearFocus();
        Object service = activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (service instanceof android.view.inputmethod.InputMethodManager manager) {
            manager.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        }
    }

    /** 按文本隐藏视图：找到文本命中项后向上提升 levels 层隐藏容器 */
    private static boolean hideByText(View root, String target, int levels, boolean contains) {
        if (root instanceof TextView) {
            CharSequence cs = ((TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            boolean hit = contains ? text.contains(target) : text.equals(target);
            if (hit) {
                View cur = root;
                for (int i = 0; i < levels && cur.getParent() instanceof ViewGroup; i++) {
                    ViewGroup parent = (ViewGroup) cur.getParent();
                    if (parent == null) {
                        return true;
                    }
                    cur = parent;
                }
                if (cur != root && cur.getVisibility() == View.VISIBLE) {
                    cur.setVisibility(View.GONE);
                    return true;
                }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hideByText(group.getChildAt(i), target, levels, contains)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsRecyclerView(View view) {
        if (view instanceof ViewGroup && view.getClass().getName().contains("RecyclerView")) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsRecyclerView(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsText(View view, String target) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null && cs.toString().contains(target)) {
                return true;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsInstanceOf(View view, Class<?> cls) {
        if (cls.isInstance(view)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsInstanceOf(group.getChildAt(i), cls)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static View findInstance(View view, Class<?> cls) {
        if (cls.isInstance(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findInstance(group.getChildAt(i), cls);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void scanView(View view) {
        if (view == null) {
            return;
        }
        if (isRecyclerView(view)) {
            ViewGroup rv = (ViewGroup) view;
            for (int i = 0; i < rv.getChildCount(); i++) {
                View child = rv.getChildAt(i);
                if (child == null || child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                if (isCommentItem(child)) {
                    filterCommentItem(child);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scanView(group.getChildAt(i));
            }
        }
    }

    private static boolean isRecyclerView(View view) {
        return view instanceof ViewGroup
                && view.getClass().getName().contains("RecyclerView");
    }

    /** 启发式判断评论项：至少两个文本控件，且含时间/回复/点赞特征 */
    private static boolean isCommentItem(View item) {
        List<String> texts = collectTexts(item);
        if (texts.size() < 2) {
            return false;
        }
        for (String text : texts) {
            if (text != null && TIME_PATTERN.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static void filterCommentItem(View item) {
        if (!bool(CommentPrefs.KEY_COMMENT_FILTER_5E6DB5D12C, false)) {
            return;
        }
        List<String> texts = collectTexts(item);
        if (texts.isEmpty()) {
            return;
        }
        String nickname = texts.get(0) == null ? "" : texts.get(0).trim();
        String body = join(texts);

        Set<String> whitelist = parseList(string(
                CommentPrefs.KEY_COMMENT_FILTER_6F055918E2, ""));
        if (whitelist.contains(nickname)) {
            return;
        }

        boolean hide = false;

        String keywords = string(CommentPrefs.KEY_COMMENT_FILTER_2ECFEAF382, "");
        for (String keyword : parseList(keywords)) {
            if (!keyword.isEmpty() && containsIgnoreCase(body, keyword)) {
                hide = true;
                break;
            }
        }

        if (!hide) {
            Set<String> blocked = parseList(string(
                    CommentPrefs.KEY_COMMENT_FILTER_C28BBE3819, ""));
            if (blocked.contains(nickname)) {
                hide = true;
            }
        }

        if (!hide && bool(CommentPrefs.KEY_COMMENT_FILTER_CD2238182A, false)) {
            for (String hint : AD_HINTS) {
                if (containsIgnoreCase(body, hint)) {
                    hide = true;
                    break;
                }
            }
        }

        if (hide) {
            item.setVisibility(View.GONE);
        }
    }

    private static List<String> collectTexts(View view) {
        List<String> texts = new ArrayList<>();
        collectTexts(view, texts);
        return texts;
    }

    private static void collectTexts(View view, List<String> texts) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null && cs.length() > 0) {
                texts.add(cs.toString());
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTexts(group.getChildAt(i), texts);
            }
        }
    }

    private static String join(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (String text : texts) {
            if (text != null) {
                sb.append(text).append('\n');
            }
        }
        return sb.toString();
    }

    private static Set<String> parseList(String raw) {
        Set<String> result = new HashSet<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String part : raw.split("[,，;；、\\n/|]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT)
                .contains(needle.toLowerCase(Locale.ROOT));
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
                // 配置变更后无需额外处理：扫描时实时读取
            };
}
