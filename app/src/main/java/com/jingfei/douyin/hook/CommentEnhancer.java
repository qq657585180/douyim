package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.jingfei.douyin.prefs.CommentPrefs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

    /** 评论区子项的时间/操作文本特征，用于区分评论项与视频项 */
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "刚刚|分钟前|小时前|天前|昨天|回复|赞");

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
                    // 禁用点击评论回复（抖X comment_interaction.fb9a1f8613）
                    if (bool(CommentPrefs.KEY_COMMENT_INTERACTION_FB9A1F8613, false)) {
                        guardReplyClick(activity, panel);
                    }
                    // 评论区外观：文字颜色 / 自定义背景（抖X comment_appearance.*）
                    applyCommentAppearance(panel);
                } else {
                    commentPanelOpen = false;
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

    /** 禁用点击评论回复：在评论列表 RecyclerView 上拦截单击，
     *  命中评论项时消费事件，阻止展开回复（抖X comment_interaction.fb9a1f8613）。 */
    private static void guardReplyClick(Activity activity, View panel) {
        if (activity == null || panel == null) {
            return;
        }
        View rv = findRecyclerView(panel);
        if (rv == null) {
            return;
        }
        if (Boolean.TRUE.equals(rv.getTag())) {
            return;
        }
        rv.setTag(Boolean.TRUE);
        GestureDetector detector = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return findHitComment(rv, e) != null;
                    }
                });
        rv.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
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
