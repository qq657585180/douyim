package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class ImmersiveUi {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long SCAN_INTERVAL_MS = 100L;
    private static final long FALLBACK_CONTENT_CHECK_INTERVAL_MS = 900L;
    private static final long UI_TRANSITION_HOLD_MS = 3_000L;
    private static final Map<View, SavedView> HIDDEN =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<View> GESTURE_PATHS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<View, Integer> ROOT_SYSTEM_UI =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, ViewGroup.LayoutParams> EXPANDED_VIEWPORTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ViewTreeObserver.OnPreDrawListener HIDDEN_VIEW_GUARD =
            () -> {
                reassertHiddenViews();
                return true;
            };
    private static WeakReference<View> hiddenGuardRoot = new WeakReference<>(null);
    private static volatile boolean showDanmaku;
    private static volatile boolean openCommentsOnDoubleTap;
    private static volatile boolean moduleEnabled = true;
    private static volatile boolean blockDoubleTap;
    private static volatile boolean immersiveEnabled = true;
    private static volatile boolean autoPlayEnabled = true;
    private static volatile Boolean appearanceModuleEnabled;
    private static volatile Boolean appearanceBlockDoubleTap;
    private static volatile Boolean appearanceImmersiveEnabled;
    private static volatile Boolean appearanceShowDanmaku;
    private static volatile Boolean appearanceDoubleTapComments;
    // === 抖X外观桥接（douyin_appearance）===
    // 播放时隐藏系统状态栏（home.2e0ddfde95）
    private static volatile boolean hideStatusBar;
    // 隐藏系统导航栏（home.7616bc18d9）
    private static volatile boolean hideNavBar;
    // 隐藏整个首页底栏（home.bottom_hidden）
    private static volatile boolean hideHomeBottomBar;
    // 隐藏底栏时保留占位（home.bottom_keep_space）
    private static volatile boolean keepHomeBottomBarSpace;
    // 清爽模式：隐藏非视频悬浮控件（controls.d4f77759dc）
    private static volatile boolean cleanMode;
    // 隐藏广告卡片（controls.ad_card）
    private static volatile boolean hideAdCard;
    // 隐藏作者声明/风险提示（controls.report_warn）
    private static volatile boolean hideAuthorNotice;
    private static volatile Boolean appearanceHideStatusBar;
    private static volatile Boolean appearanceHideNavBar;
    private static volatile Boolean appearanceHideHomeBottomBar;
    private static volatile Boolean appearanceCleanMode;
    private static volatile Boolean appearanceHideAdCard;
    private static volatile Boolean appearanceHideAuthorNotice;
    private static SharedPreferences immersivePreferences;
    private static final SharedPreferences.OnSharedPreferenceChangeListener
            PREFERENCE_LISTENER = (preferences, key) -> {
                refreshPreferences(preferences);
            };

    private static WeakReference<Activity> active = new WeakReference<>(null);
    private static WeakReference<View> activeRoot = new WeakReference<>(null);
    private static boolean scanScheduled;
    private static boolean swipeRunning;
    private static long swipeToken;
    private static boolean videoMissingLogged;
    private static boolean activityResolveErrorLogged;
    private static long lastScanFailureAt;
    private static long transitionBoostUntil;
    private static float touchDownX;
    private static float touchDownY;
    private static long touchDownAt;
    private static long touchGestureToken;
    private static boolean touchInProgress;
    private static boolean touchDownImmersive;
    private static String touchDownAid;
    private static String touchDownObservedAid;
    private static Object touchDownEngine;
    private static int touchDownEngineState;
    private static WeakReference<TextView> downloadButton =
            new WeakReference<>(null);
    private static long lastHandledTouchDownTime;
    private static long contentCheckNotBefore;
    private static long contentCheckUntil;
    private static long lastContentCheckAt;
    private static long lastFilteredSwipeAt;
    private static long lastSwipeAt;
    private static String filterCandidateAid;
    private static String filterCandidateReason;
    private static int filterCandidateCount;
    private static long filterCandidateAt;
    private static String lastAcceptedAid;
    private static int lastDanmakuCount = -1;
    private static long lastCommentOpenAt;
    private static long commentsOpenedAt;
    private static volatile long syntheticTapUntil;
    private static long filterBurstStart;
    private static int filterBurstCount;
    private static long filterSuspendedUntil;
    /** 隐藏视频进度条（抖X progress.hidden）。 */
    private static volatile boolean hideProgressEnabled;
    /** 隐藏首页搜索入口（抖X home.d23c896621）。 */
    private static volatile boolean hideHomeSearch;

    // 仅过滤推荐流（抖X feed_filter.scope）
    private static boolean feedScopeRecommendOnly;
    /** 隐藏消息角标（抖X home.3fe35ca5c8）。 */
    private static volatile boolean hideMessageBadge;
    /** 隐藏首页底栏皮肤（抖X home.ba08b02cee）。 */
    private static volatile boolean hideBottomBarSkin;
    /** 首页按钮刷新前确认（抖X home.6f099dad58）。 */
    private static volatile boolean refreshConfirmEnabled;
    /** 音乐封面随右侧栏移动（抖X controls.a98f8afb28）。 */
    private static volatile boolean musicDiscDragEnabled;
    /** 底栏账号头像（抖X glass.avatar）。 */
    private static volatile boolean glassAvatarEnabled;
    /** 顶栏双列浏览入口（抖X glass.double）。 */
    private static volatile boolean glassDoubleEnabled;
    /** 顶栏实时帧率（抖X glass.fps）。 */
    private static volatile boolean glassFpsEnabled;

    // 视频右侧栏样式（抖X style__right.*）：透明度/暂停透明度/独立暂停透明度/位移/缩放
    private static int rightBarOpacity = 100;
    private static int rightBarPausedOpacity = 100;
    private static boolean rightBarPausedSeparate;
    private static int rightBarDx;
    private static int rightBarDy;
    private static float rightBarScale = 1.0f;
    private static float dragDx;
    private static float dragDy;
    /** 透明系统导航栏（抖X home.9902412e18）。 */
    private static volatile boolean transparentNavBar;
    /** 自动点击广告跳过/解锁按钮（抖X skipad / playlet_ad / miniapp）。 */
    private static volatile boolean autoSkipAd;
    /** 自动跳过章节广告（抖X extras.d2a3ea57f1）。 */
    private static volatile boolean autoSkipChapterAd;
    private static long lastAdSkipAt;
    /** 视频背景颜色（抖X background_video.52/.53）。 */
    private static volatile String videoBackgroundColor;
    private static volatile String lastAppliedVideoBackground;
    /** 图文背景颜色（抖X background_photo.59/.60）。 */
    private static volatile String photoBackgroundColor;
    /** 实况背景颜色（抖X background_livephoto.66/.67）。 */
    private static volatile String livePhotoBackgroundColor;
    /** 常驻进度条（抖X progress.20ee30e2ae）：进度条不自动隐藏。 */
    private static volatile boolean keepProgressVisible;
    /** 长按临时倍速（抖X gestures.be54e501cb）。 */
    private static volatile boolean tempSpeedEnabled;
    private static final android.os.Handler TEMP_SPEED_HANDLER =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private static final Runnable TEMP_SPEED_ACTIVATE =
            () -> PlayerHooks.setTemporarySpeedActive(true);

    private ImmersiveUi() {
    }

    static synchronized void configurePreferences(SharedPreferences preferences) {
        if (immersivePreferences != null && immersivePreferences != preferences) {
            try {
                immersivePreferences.unregisterOnSharedPreferenceChangeListener(
                        PREFERENCE_LISTENER
                );
            } catch (RuntimeException ignored) {
                // A dead remote preference bridge will be replaced below.
            }
        }
        immersivePreferences = preferences;
        refreshPreferences(preferences);
        if (preferences != null) {
            preferences.registerOnSharedPreferenceChangeListener(PREFERENCE_LISTENER);
        }
    }

    static boolean isModuleEnabled() {
        return moduleEnabled;
    }

    static boolean shouldBlockDoubleTap() {
        return moduleEnabled && blockDoubleTap;
    }

    static boolean shouldOpenCommentsOnDoubleTap() {
        return moduleEnabled && openCommentsOnDoubleTap;
    }

    /**
     * Double tap opens the native comment panel. The panel itself is host UI, so the tap is
     * forwarded to the real comment button instead of synthesizing a second single tap; the
     * gesture is still consumed so the like/heart animation never runs.
     */
    static void onDoubleTapForComments() {
        if (!moduleEnabled || !openCommentsOnDoubleTap) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastCommentOpenAt < 800L) {
            // Three hooked callbacks (interceptDoubleClick + onDoubleTap + onDoubleTapEvent)
            // report the same gesture; clicking three times toggled the panel back shut.
            return;
        }
        lastCommentOpenAt = now;
        attemptOpenComments(0);
    }

    private static void attemptOpenComments(int attempt) {
        MAIN.postDelayed(() -> {
            Activity activity = activeActivity();
            View decor = activeDecor(activity);
            if (decor == null) {
                return;
            }
            if (attempt > 0 && isCommentPanelVisible(decor)) {
                scheduleScan(0L);
                return;
            }
            List<View> candidates = new ArrayList<>();
            collectCommentCandidates(decor, decor, candidates);
            if (candidates.isEmpty()) {
                // The first video after a cold start has no right action bar yet; searching
                // again a moment later is what makes the very first double tap work.
                if (attempt < 2) {
                    attemptOpenComments(attempt + 1);
                } else {
                    Log.w(DouyinModule.TAG, "double tap: comment entry not found");
                }
                return;
            }
            clickCommentCandidate(decor, candidates, 0);
        }, attempt == 0 ? 0L : 350L);
    }

    /**
     * Click the container that owns the click listener. Digging into the smallest clickable
     * child landed on the comment counter (TextSwitcher), which only animates the number.
     */
    private static View clickableTarget(View node) {
        View current = node;
        for (int level = 0; level < 4 && current != null; level++) {
            if (current.isClickable() && current.hasOnClickListeners()) {
                return current;
            }
            current = current.getParent() instanceof View parent ? parent : null;
        }
        return node;
    }

    private static void clickCommentCandidate(
            View decor,
            List<View> candidates,
            int index
    ) {
        if (index >= candidates.size() || index >= 3) {
            Log.w(DouyinModule.TAG, "double tap: no candidate opened the comment panel");
            return;
        }
        View target = clickableTarget(candidates.get(index));
        Log.i(DouyinModule.TAG, "double tap opens comments: "
                + target.getClass().getName()
                + " id=" + target.getId()
                + " desc=" + target.getContentDescription()
                + " listeners=" + target.hasOnClickListeners()
                + " candidate=" + index);
        if (!tapView(decor, target)) {
            target.performClick();
        }
        commentsOpenedAt = SystemClock.uptimeMillis();
        scheduleScan(0L);
        MAIN.postDelayed(() -> {
            if (isCommentPanelVisible(decor)) {
                scheduleScan(0L);
                return;
            }
            Log.i(DouyinModule.TAG, "candidate " + index + " did not open comments, trying next");
            clickCommentCandidate(decor, candidates, index + 1);
        }, 500L);
    }


    private static void collectCommentCandidates(
            View node,
            View decor,
            List<View> out
    ) {
        if (node == null || !node.isAttachedToWindow() || !node.isShown()) {
            return;
        }
        CharSequence description = node.getContentDescription();
        String label = description == null ? "" : description.toString();
        boolean mentionsComment = label.contains("评论")
                || label.toLowerCase(java.util.Locale.ROOT).contains("comment");
        String className = node.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if (className.startsWith("com.ss.android.ugc.aweme.comment.")
                || className.contains("commenttopaction")
                || className.contains("commentlist")
                || className.contains("commentinput")) {
            // Inside the opened comment panel: never click panel internals.
            return;
        }
        boolean looksLikeCommentView = className.contains("comment");
        Rect visible = new Rect();
        boolean rightSide = node.getGlobalVisibleRect(visible)
                && visible.centerX() > decor.getWidth() * 0.6f
                && visible.width() > 0
                && visible.width() <= decor.getWidth() * 0.35f
                && visible.height() > 0
                && visible.height() <= decor.getHeight() * 0.25f;
        if ((mentionsComment || (looksLikeCommentView && rightSide))
                && node.isClickable()
                && rightSide) {
            out.add(node);
        }
        if (node instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                collectCommentCandidates(group.getChildAt(index), decor, out);
            }
        }
    }

    private static void refreshPreferences(SharedPreferences preferences) {
        boolean enabled = com.jingfei.douyin.FilterPreferences.readModuleEnabled(preferences);
        boolean changed = moduleEnabled != enabled;
        moduleEnabled = enabled;
        blockDoubleTap = com.jingfei.douyin.FilterPreferences.readBlockDoubleTap(preferences);
        immersiveEnabled = com.jingfei.douyin.FilterPreferences.readImmersiveEnabled(preferences);
        showDanmaku = com.jingfei.douyin.FilterPreferences.readShowDanmaku(preferences);
        openCommentsOnDoubleTap =
                com.jingfei.douyin.FilterPreferences.readDoubleTapComments(preferences);
        applyAppearanceOverridesToFields();
        MAIN.post(() -> {
            if (changed) {
                touchGestureToken++;
                touchInProgress = false;
                transitionBoostUntil = 0L;
                resetFilterCandidate();
                PlaybackState.clearModuleIntents();
                Log.i(DouyinModule.TAG, "module enabled=" + moduleEnabled);
            }
            if (!moduleEnabled) {
                removeDownloadButton();
                VideoDownloader.dismissChooser();
                Activity activity = activeActivity();
                restoreAll(activity, activeDecor(activity));
            } else {
                if (!immersiveEnabled) {
                    Activity activity = activeActivity();
                    restoreAll(activity, activeDecor(activity));
                }
                armContentFilter(1_000L);
            }
            scheduleScan(0L);
        });
    }

    /** 由 PlaybackStrategy 注入抖X自动连播设置（douyin_playback）。 */
    static void setAutoPlayEnabled(boolean enabled) {
        autoPlayEnabled = enabled;
    }

    /** 抖X progress.hidden：隐藏视频进度条。 */
    static void setHideProgressEnabled(boolean enabled) {
        hideProgressEnabled = enabled;
    }

    /** 抖X home.d23c896621：隐藏首页搜索入口。 */
    static void setHideHomeSearch(boolean enabled) {
        hideHomeSearch = enabled;
    }

    /** 仅过滤推荐流开关（抖X feed_filter.scope）。 */
    static void setFeedScopeRecommendOnly(boolean enabled) {
        feedScopeRecommendOnly = enabled;
    }

    /** 抖X home.3fe35ca5c8：隐藏首页消息角标。 */
    static void setHideMessageBadge(boolean enabled) {
        hideMessageBadge = enabled;
    }

    /** 抖X home.ba08b02cee：隐藏首页底栏皮肤。 */
    static void setHideBottomBarSkin(boolean enabled) {
        hideBottomBarSkin = enabled;
    }

    /** 抖X home.6f099dad58：首页按钮刷新前确认。 */
    static void setRefreshConfirmEnabled(boolean enabled) {
        refreshConfirmEnabled = enabled;
    }

    /** 抖X controls.a98f8afb28：音乐封面随右侧栏移动。 */
    /** 底栏账号头像（glass.avatar）开关。 */
    static void setGlassAvatarEnabled(boolean enabled) {
        if (glassAvatarEnabled == enabled) {
            return;
        }
        glassAvatarEnabled = enabled;
        if (!enabled) {
            removeAvatarOverlay();
        }
    }

    /** 顶栏双列浏览入口（glass.double）开关。 */
    static void setGlassDoubleEnabled(boolean enabled) {
        if (glassDoubleEnabled == enabled) {
            return;
        }
        glassDoubleEnabled = enabled;
        if (!enabled) {
            removeDoubleOverlay();
        }
    }

    /** 顶栏实时帧率（glass.fps）开关。 */
    static void setGlassFpsEnabled(boolean enabled) {
        if (glassFpsEnabled == enabled) {
            return;
        }
        glassFpsEnabled = enabled;
        if (!enabled) {
            removeFpsOverlay();
        }
    }

    static void setMusicDiscDragEnabled(boolean enabled) {
        musicDiscDragEnabled = enabled;
    }

    /** 抖X style__right：视频右侧栏样式（透明度/暂停透明度/独立暂停透明度/位移/缩放）。 */
    static void setRightBarStyle(int opacity, int pausedOpacity, boolean pausedSeparate,
                                 int dx, int dy, float scale) {
        rightBarOpacity = opacity;
        rightBarPausedOpacity = pausedOpacity;
        rightBarPausedSeparate = pausedSeparate;
        rightBarDx = dx;
        rightBarDy = dy;
        rightBarScale = scale;
    }

    /** 抖X home.9902412e18：透明系统导航栏。 */
    static void setTransparentNavBar(boolean enabled) {
        transparentNavBar = enabled;
    }

    /** 由 PurifyEnhancer 注入抖X广告跳过开关（skipad / playlet_ad / miniapp）。 */
    static void setAutoSkipAd(boolean enabled) {
        autoSkipAd = enabled;
    }

    /** 抖X extras.d2a3ea57f1：自动跳过章节广告（独立于净化组广告开关）。 */
    static void setAutoSkipChapterAd(boolean enabled) {
        autoSkipChapterAd = enabled;
    }

    /** 抖X background_video.53：视频背景颜色（#RRGGBB / #AARRGGBB，null 恢复默认）。 */
    static void setVideoBackgroundColor(String color) {
        videoBackgroundColor = color;
    }

    /** 抖X background_photo.60：图文背景颜色（null 恢复默认）。 */
    static void setPhotoBackgroundColor(String color) {
        photoBackgroundColor = color;
    }

    /** 抖X background_livephoto.67：实况背景颜色（null 恢复默认）。 */
    static void setLivePhotoBackgroundColor(String color) {
        livePhotoBackgroundColor = color;
    }

    /** 抖X progress.20ee30e2ae：常驻进度条。 */
    static void setKeepProgressVisible(boolean enabled) {
        keepProgressVisible = enabled;
    }

    /** 抖X gestures.be54e501cb：长按临时倍速开关。 */
    static void setTempSpeedEnabled(boolean enabled) {
        tempSpeedEnabled = enabled;
    }

    /** 由 ThemeManager 注入抖X外观覆盖（douyin_appearance）。null 表示不覆盖该字段。 */
    static void applyAppearanceOverrides(
            Boolean moduleEnabled,
            Boolean blockDoubleTap,
            Boolean immersiveEnabled,
            Boolean showDanmaku,
            Boolean doubleTapComments) {
        appearanceModuleEnabled = moduleEnabled;
        appearanceBlockDoubleTap = blockDoubleTap;
        appearanceImmersiveEnabled = immersiveEnabled;
        appearanceShowDanmaku = showDanmaku;
        appearanceDoubleTapComments = doubleTapComments;
        applyAppearanceOverridesToFields();
    }

    /** 由 ThemeManager 注入抖X外观可见性覆盖（douyin_appearance）。null 表示不覆盖该字段。 */
    static void applyUiVisibilityOverrides(
            Boolean hideStatusBar,
            Boolean hideNavBar,
            Boolean hideHomeBottomBar,
            Boolean cleanMode,
            Boolean hideAdCard,
            Boolean hideAuthorNotice) {
        appearanceHideStatusBar = hideStatusBar;
        appearanceHideNavBar = hideNavBar;
        appearanceHideHomeBottomBar = hideHomeBottomBar;
        appearanceCleanMode = cleanMode;
        appearanceHideAdCard = hideAdCard;
        appearanceHideAuthorNotice = hideAuthorNotice;
        applyAppearanceOverridesToFields();
    }

    /** 隐藏底栏时是否保留占位（home.bottom_keep_space）。 */
    static void setHomeBottomBarKeepSpace(boolean keep) {
        keepHomeBottomBarSpace = keep;
    }

    /** 沉浸模式是否应激活：原生沉浸开关或任一抖X外观隐藏项开启。 */
    private static boolean isImmersiveActive() {
        return immersiveEnabled
                || hideHomeBottomBar
                || cleanMode
                || hideAdCard
                || hideAuthorNotice;
    }

    private static void applyAppearanceOverridesToFields() {
        if (appearanceModuleEnabled != null) {
            moduleEnabled = appearanceModuleEnabled;
        }
        if (appearanceBlockDoubleTap != null) {
            blockDoubleTap = appearanceBlockDoubleTap;
        }
        if (appearanceImmersiveEnabled != null) {
            immersiveEnabled = appearanceImmersiveEnabled;
        }
        if (appearanceShowDanmaku != null) {
            showDanmaku = appearanceShowDanmaku;
        }
        if (appearanceDoubleTapComments != null) {
            openCommentsOnDoubleTap = appearanceDoubleTapComments;
        }
        if (appearanceHideStatusBar != null) {
            hideStatusBar = appearanceHideStatusBar;
        }
        if (appearanceHideNavBar != null) {
            hideNavBar = appearanceHideNavBar;
        }
        if (appearanceHideHomeBottomBar != null) {
            hideHomeBottomBar = appearanceHideHomeBottomBar;
        }
        if (appearanceCleanMode != null) {
            cleanMode = appearanceCleanMode;
        }
        if (appearanceHideAdCard != null) {
            hideAdCard = appearanceHideAdCard;
        }
        if (appearanceHideAuthorNotice != null) {
            hideAuthorNotice = appearanceHideAuthorNotice;
        }
    }

    static void onActivityResumed(Activity activity) {
        if (activity == null) {
            return;
        }
        MAIN.post(() -> {
            active = new WeakReference<>(activity);
            View decor = activity.getWindow().getDecorView();
            activeRoot = new WeakReference<>(decor);
            attachHiddenViewGuard(decor);
            Log.i(DouyinModule.TAG, "activity resumed: " + activity.getClass().getName());
            armContentFilter(1_000L);
            scheduleScan(120L);
        });
    }

    static void onActivityPaused(Activity activity) {
        MAIN.post(() -> {
            Activity current = active.get();
            if (current == activity) {
                removeDownloadButton();
                VideoDownloader.dismissChooser();
                restoreAll(current);
                active.clear();
                activeRoot.clear();
                touchInProgress = false;
            }
        });
    }

    static void onFeedPageSelected() {
        if (!moduleEnabled) return;
        PlaybackState.beginAutoSwitch();
        MAIN.post(() -> {
            boostTransitionWindow();
            armContentFilter(120L);
            scheduleScan(0L);
        });
    }

    static void beforeActivityTouch(Activity activity, MotionEvent event) {
        if (!moduleEnabled) return;
        if (swipeRunning
                || !touchInProgress
                || event.getActionMasked() != MotionEvent.ACTION_UP
                || !PlaybackState.isUserPaused()) {
            return;
        }
        View decor = activeDecor(activity);
        float density = decor == null
                ? 1f
                : decor.getResources().getDisplayMetrics().density;
        float dx = event.getRawX() - touchDownX;
        float dy = event.getRawY() - touchDownY;
        if (decor != null
                && Math.abs(dy) > 72f * density
                && Math.abs(dy) > Math.abs(dx)) {
            PlaybackState.beginAutoSwitch();
            boostTransitionWindow();
        }
    }

    /** 取消长按临时倍速：移除定时器并恢复原速。 */
    private static void cancelTempSpeed() {
        TEMP_SPEED_HANDLER.removeCallbacks(TEMP_SPEED_ACTIVATE);
        PlayerHooks.setTemporarySpeedActive(false);
    }

    static void onActivityTouch(Activity activity, MotionEvent event) {
        if (!moduleEnabled) return;
        int action = event.getActionMasked();
        if (swipeRunning) {
            if (touchInProgress
                    && (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL)) {
                touchInProgress = false;
            }
            return;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            touchInProgress = true;
            touchDownImmersive = hasHiddenViews() && !PlaybackState.isUserPaused();
            touchGestureToken++;
            touchDownX = event.getRawX();
            touchDownY = event.getRawY();
            touchDownAt = event.getEventTime();
            View decor = activeDecor(activity);
            FeedContentTracker.Snapshot model = FeedContentTracker.current(decor);
            touchDownObservedAid = model == null ? null : model.aid;
            touchDownAid = touchDownObservedAid == null
                    ? lastAcceptedAid
                    : touchDownObservedAid;
            touchDownEngine = PlaybackState.engine();
            touchDownEngineState = PlaybackState.engineState(touchDownEngine);
            // 长按临时倍速（抖X gestures.be54e501cb）
            if (tempSpeedEnabled) {
                TEMP_SPEED_HANDLER.removeCallbacks(TEMP_SPEED_ACTIVATE);
                TEMP_SPEED_HANDLER.postDelayed(TEMP_SPEED_ACTIVATE, 500L);
            }
            return;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            touchInProgress = false;
            cancelTempSpeed();
            return;
        }
        if (action != MotionEvent.ACTION_UP) {
            return;
        }
        touchInProgress = false;
        cancelTempSpeed();
        long gestureId = event.getDownTime();
        if (gestureId == lastHandledTouchDownTime) {
            return;
        }
        lastHandledTouchDownTime = gestureId;

        float dx = event.getRawX() - touchDownX;
        float dy = event.getRawY() - touchDownY;
        long elapsed = event.getEventTime() - touchDownAt;
        View decor = activeDecor(activity);
        float density = decor == null
                ? 1f
                : decor.getResources().getDisplayMetrics().density;
        if (decor != null
                && Math.abs(dy) > 72f * density
                && Math.abs(dy) > Math.abs(dx)) {
            boostTransitionWindow();
            armContentFilter(700L);
            confirmUserFeedSwitch(
                    touchGestureToken,
                    touchDownAid,
                    decor,
                    8
            );
            return;
        }
        if (decor == null
                || dx * dx + dy * dy > 1_600f
                || elapsed > 500L
                || (!touchDownImmersive && (event.getRawX() < decor.getWidth() * 0.18f
                || event.getRawX() > decor.getWidth() * 0.82f
                || event.getRawY() < decor.getHeight() * 0.12f
                || event.getRawY() > decor.getHeight() * 0.86f))) {
            return;
        }

        boolean resumeRequested = PlaybackState.isUserPaused();
        long gestureToken = touchGestureToken;
        long pauseIntentAt = touchDownAt;
        Object pauseIntentEngine = touchDownEngine;
        int pauseIntentInitialState = touchDownEngineState;
        String pauseIntentAid = touchDownObservedAid;
        touchDownEngine = null;
        if (resumeRequested) {
            confirmUserResume(
                    gestureToken,
                    pauseIntentEngine,
                    4
            );
        } else {
            confirmUserPause(
                    gestureToken,
                    pauseIntentAt,
                    pauseIntentEngine,
                    pauseIntentInitialState,
                    pauseIntentAid,
                    activity,
                    decor,
                    4
            );
        }
    }

    private static void confirmUserResume(
            long gestureToken,
            Object resumeIntentEngine,
            int attemptsLeft
    ) {
        MAIN.postDelayed(() -> {
            if (gestureToken != touchGestureToken
                    || !PlaybackState.isUserPaused()) {
                return;
            }
            if (PlaybackState.confirmUserPlaying(resumeIntentEngine)) {
                removeDownloadButton();
                scheduleScan(0L);
                return;
            }
            if (attemptsLeft > 1) {
                confirmUserResume(
                        gestureToken,
                        resumeIntentEngine,
                        attemptsLeft - 1
                );
            } else {
                Log.d(DouyinModule.TAG,
                        "ignored center tap because playback stayed paused");
                scheduleScan(0L);
            }
        }, 120L);
    }

    private static void confirmUserPause(
            long gestureToken,
            long pauseIntentAt,
            Object pauseIntentEngine,
            int pauseIntentInitialState,
            String pauseIntentAid,
            Activity activity,
            View decor,
            int attemptsLeft
    ) {
        MAIN.postDelayed(() -> {
            if (gestureToken != touchGestureToken
                    || PlaybackState.isUserPaused()) {
                return;
            }
            FeedContentTracker.Snapshot currentModel =
                    FeedContentTracker.current(decor);
            String confirmedPauseAid = currentModel == null
                    ? pauseIntentAid
                    : currentModel.aid;
            boolean confirmedPause =
                    PlaybackState.confirmUserPaused(
                            pauseIntentEngine,
                            pauseIntentAt,
                            pauseIntentInitialState,
                            confirmedPauseAid
                    );
            if (confirmedPause) {
                restoreAll(activity, decor);
                showDownloadButton(activity, decor);
                return;
            }
            if (attemptsLeft > 1) {
                confirmUserPause(
                        gestureToken,
                        pauseIntentAt,
                        pauseIntentEngine,
                        pauseIntentInitialState,
                        pauseIntentAid,
                        activity,
                        decor,
                        attemptsLeft - 1
                );
            } else {
                Log.d(DouyinModule.TAG,
                        "ignored center tap because playback stayed active");
                scheduleScan(0L);
            }
        }, 180L);
    }

    private static void confirmUserFeedSwitch(long token,
                                              String previousAid,
                                              View decor,
                                              int attemptsLeft) {
        MAIN.postDelayed(() -> {
            if (token != touchGestureToken || previousAid == null) {
                return;
            }
            FeedContentTracker.Snapshot model = FeedContentTracker.current(decor);
            if (model != null && !previousAid.equals(model.aid)) {
                if (PlaybackState.isUserPaused()) {
                    PlaybackState.userPlaying();
                } else {
                    PlaybackState.confirmVideoSwitch();
                }
                scheduleScan(0L);
                return;
            }
            if (attemptsLeft > 1) {
                confirmUserFeedSwitch(token, previousAid, decor, attemptsLeft - 1);
            }
        }, 160L);
    }

    static void onPlaybackChanged(boolean playing) {
        MAIN.post(() -> {
            if (!moduleEnabled) return;
            if (playing) {
                boostTransitionWindow();
            }
            Activity activity = activeActivity();
            View decor = activeDecor(activity);
            if (decor == null) {
                scheduleScan(250L);
                return;
            }
            if (playing) {
                removeDownloadButton();
                scheduleScan(0L);
            } else {
                long token = PlaybackState.generation();
                MAIN.postDelayed(() -> {
                    if (moduleEnabled && token == PlaybackState.generation()
                            && !PlaybackState.shouldKeepUiHidden()) {
                        Activity currentActivity = activeActivity();
                        View currentDecor = activeDecor(currentActivity);
                        restoreAll(currentActivity, currentDecor);
                        showDownloadButton(currentActivity, currentDecor);
                    }
                }, 720L);
            }
        });
    }

    static void onPlaybackCompleted(String reason) {
        MAIN.post(() -> {
            if (!moduleEnabled) return;
            Activity activity = activeActivity();
            View decor = activeDecor(activity);
            if (decor == null) {
                return;
            }
            swipeToNext(decor, reason);
        });
    }

    private static void scheduleScan(long delayMs) {
        if (scanScheduled) {
            return;
        }
        scanScheduled = true;
        MAIN.postDelayed(ImmersiveUi::scan, delayMs);
    }

    private static void scan() {
        scanScheduled = false;
        try {
            scanOnce();
        } catch (Throwable error) {
            long now = SystemClock.uptimeMillis();
            if (now - lastScanFailureAt >= 2_000L) {
                lastScanFailureAt = now;
                Log.e(DouyinModule.TAG,
                        "immersive UI scan failed; watchdog will retry", error);
            }
        } finally {
            if (!scanScheduled) {
                scheduleScan(SCAN_INTERVAL_MS);
            }
        }
    }

    private static void scanOnce() {
        Activity activity = activeActivity();
        View decor = activeDecor(activity);
        if (!moduleEnabled) {
            removeDownloadButton();
            restoreAll(activity, decor);
            PlaybackTimeOverlay.remove();
            removeFpsOverlay();
            removeAvatarOverlay();
            removeDoubleOverlay();
            scheduleScan(500L);
            return;
        }
        if (decor == null) {
            scheduleScan(250L);
            return;
        }
        if (activity != null && (activity.isFinishing() || activity.isDestroyed())) {
            return;
        }
        // 播放进度时间叠加（抖X playback.progress_time）
        if (activity != null) {
            PlaybackTimeOverlay.update(activity);
        }
        // 弹幕增强：关键词过滤 + 弹幕颜色（抖X danmaku.*）
        DanmakuEnhancer.update(decor);
        // 显示准确数量（抖X controls.9aa386bb74 / live.0d1ad71357 /
        // profile.exact_counts / profile.exact_grid_likes）
        ExactNumbers.scan(decor);
        // 隐藏视频进度条（抖X progress.hidden）
        if (hideProgressEnabled) {
            hideProgressBars(decor);
        }
        // 常驻进度条（抖X progress.20ee30e2ae）：强制保持可见
        if (keepProgressVisible) {
            keepProgressBarsVisible(decor);
        }
        // 隐藏首页搜索入口（抖X home.d23c896621）
        if (hideHomeSearch) {
            hideByText(decor, "搜索", 2, true);
        }
        // 隐藏首页消息角标（抖X home.3fe35ca5c8）
        if (hideMessageBadge) {
            hideMessageBadge(decor);
        }
        // 隐藏首页底栏皮肤（抖X home.ba08b02cee）
        if (hideBottomBarSkin) {
            hideBottomBarSkin(decor);
        }
        // 隐藏整个首页底栏（抖X home.bottom_hidden）
        if (hideHomeBottomBar) {
            hideHomeBottomTabBar(decor);
        }
        // 首页底栏液态玻璃（抖X glass.bottom）：对底栏容器应用半透明+模糊
        GlassUi.applyBottomGlass(findHomeTabBar(decor));
        // 顶部导航液态玻璃（抖X glass.top）
        GlassUi.applyTopGlass(findTopTabBar(decor));
        // 视频右侧栏液态玻璃（抖X glass.right）
        GlassUi.applyRightGlass(findRightBar(decor));
        // 顶栏实时帧率（抖X glass.fps）
        if (glassFpsEnabled) {
            updateFpsOverlay(decor);
        }
        // 底栏账号头像（抖X glass.avatar）
        if (glassAvatarEnabled) {
            updateAvatarOverlay(decor);
        }
        // 顶栏双列浏览入口（抖X glass.double）
        if (glassDoubleEnabled) {
            updateDoubleOverlay(decor);
        }
        // 首页按钮刷新前确认（抖X home.6f099dad58）
        if (refreshConfirmEnabled) {
            installRefreshConfirm(decor);
        }
        // 音乐封面随右侧栏移动（抖X controls.a98f8afb28）
        if (musicDiscDragEnabled) {
            enableMusicDiscDrag(decor);
        }
        // 视频右侧栏样式（抖X style__right）
        applyRightBarStyle(decor);
        // 自动点击广告跳过/解锁按钮（抖X skipad / playlet_ad / miniapp）
        autoSkipAd(decor);

        // 视频背景颜色（抖X background_video.52/.53）
        if (videoBackgroundColor != null && !videoBackgroundColor.isEmpty()
                && !videoBackgroundColor.equals(lastAppliedVideoBackground)) {
            applyVideoBackground(decor);
            lastAppliedVideoBackground = videoBackgroundColor;
        }
        // 图文/实况背景颜色（抖X background_photo.59/.60、background_livephoto.66/.67）
        if ((photoBackgroundColor != null && !photoBackgroundColor.isEmpty())
                || (livePhotoBackgroundColor != null && !livePhotoBackgroundColor.isEmpty())) {
            applyMediaBackgrounds(decor);
        }

        if (PlaybackState.consumePlaybackError()) {
            onPlaybackCompleted("playback error");
            scheduleNextScan();
            return;
        }

        if ((autoPlayEnabled || PlaybackState.shouldPauseOnCompletion())
                && PlaybackState.consumeLoopBoundary()) {
            // pauseOnCompletion 场景已在 consumeLoopBoundary 内暂停播放器并恢复 UI；
            // 只有自动连播开启时才模拟滑动到下一集。
            if (autoPlayEnabled) {
                onPlaybackCompleted("completed loop boundary");
            }
            scheduleNextScan();
            return;
        }

        // 仅过滤推荐流（feed_filter.scope）：开启时非推荐 Tab 不过滤
        boolean feedScopeOk = !feedScopeRecommendOnly || isRecommendTab(decor);
        if (feedScopeOk && checkCurrentFeedContent(decor)) {
            scheduleNextScan();
            return;
        }

        boolean keepUiHidden = isImmersiveActive() && PlaybackState.shouldKeepUiHidden();
        RenderViews realVideos =
                keepUiHidden
                        ? findVisibleRealVideoViews(decor)
                        : RenderViews.EMPTY;

        if (!keepUiHidden) {
            restoreAll(activity, decor);
            showDownloadButton(activity, decor);
            scheduleScan(SCAN_INTERVAL_MS);
            return;
        }

        removeDownloadButton();
        if (commentsOpenedAt > 0L) {
            long openedFor = SystemClock.uptimeMillis() - commentsOpenedAt;
            boolean opening = openedFor < 1_200L;
            // Never trust the player-size heuristic here: 图文 keeps a tiny audio surface,
            // which made the hold permanent and killed immersive playback after closing.
            if (opening || (openedFor < 30_000L && isCommentPanelVisible(decor))) {
                // The sheet is host UI: hiding it left a black half screen, so keep the real
                // UI until it is closed.
                restoreAll(activity, decor);
                scheduleNextScan();
                return;
            }
            commentsOpenedAt = 0L;
            Log.i(DouyinModule.TAG, "comment panel closed, immersive resumes");
        }
        List<View> videos = findVisibleVideoViews(
                decor,
                realVideos.visibleVideos
        );
        if (!videos.isEmpty()) {
            videoMissingLogged = false;
            expandVideoViewport(decor, videos);
            List<View> preservedViews = new ArrayList<>(videos);
            if (showDanmaku) {
                int before = preservedViews.size();
                collectVisibleDanmakuViews(decor, videos, preservedViews);
                int found = preservedViews.size() - before;
                if (found != lastDanmakuCount) {
                    lastDanmakuCount = found;
                    Log.i(DouyinModule.TAG, "preserved danmaku layers=" + found
                            + (found > 0
                            ? " " + preservedViews.get(preservedViews.size() - 1)
                            .getClass().getSimpleName()
                            : ""));
                }
            }
            restorePreservedPaths(preservedViews);
            hideOutsidePreservedPaths(decor, preservedViews);
            updateGesturePaths(decor, videos);
            hideSystemBars(activity, decor);
        } else {
            long now = SystemClock.uptimeMillis();
            if (ImmersiveTransitionPolicy.shouldHoldHiddenUi(
                    hasHiddenViews(),
                    now,
                    transitionBoostUntil
            )) {
                reassertHiddenViews();
                hideSystemBars(activity, decor);
            } else {
                restoreAll(activity, decor);
            }
            if (!videoMissingLogged) {
                videoMissingLogged = true;
                Log.w(DouyinModule.TAG,
                        "no centered visible video SurfaceView/TextureView found");
            }
        }
        scheduleNextScan();
    }

    private static void scheduleNextScan() {
        long delay = SystemClock.uptimeMillis() < transitionBoostUntil
                ? 16L
                : SCAN_INTERVAL_MS;
        scheduleScan(delay);
    }

    private static void boostTransitionWindow() {
        transitionBoostUntil = Math.max(
                transitionBoostUntil,
                SystemClock.uptimeMillis() + UI_TRANSITION_HOLD_MS
        );
    }

    private static Activity activeActivity() {
        Activity current = active.get();
        if (current != null && !current.isFinishing() && !current.isDestroyed()) {
            return current;
        }
        Activity resolved = resolveTopActivity();
        if (resolved != null) {
            active = new WeakReference<>(resolved);
            Log.i(DouyinModule.TAG, "resolved activity: " + resolved.getClass().getName());
        }
        return resolved;
    }

    private static View activeDecor(Activity activity) {
        if (activity != null) {
            View decor = activity.getWindow().getDecorView();
            activeRoot = new WeakReference<>(decor);
            return decor;
        }
        View current = activeRoot.get();
        if (current != null && current.isAttachedToWindow()) {
            return current;
        }
        View resolved = resolveLargestWindowRoot();
        if (resolved != null) {
            activeRoot = new WeakReference<>(resolved);
            Log.i(DouyinModule.TAG, "resolved window root: "
                    + resolved.getClass().getName() + " "
                    + resolved.getWidth() + "x" + resolved.getHeight());
        }
        return resolved;
    }

    /**
     * 普通视频要「暂停」后才出现下载按钮；图文没有暂停状态（只有背景音乐），
     * 识别为图文并恢复完整界面时就允许保存图片。
     */
    private static boolean canDownload() {
        return PlaybackState.isContentAvailable()
                && (PlaybackState.isUserPaused() || PlaybackState.isPhotoContent());
    }

    private static void showDownloadButton(Activity activity, View decor) {
        if (!moduleEnabled || activity == null
                || decor == null
                || !canDownload()
                || !(decor instanceof FrameLayout container)) {
            removeDownloadButton();
            return;
        }

        TextView current = downloadButton.get();
        if (current != null && current.getParent() == container) {
            current.setVisibility(View.VISIBLE);
            current.bringToFront();
            return;
        }
        removeDownloadButton();

        int size = dp(decor, 52);
        int verticalGap = dp(decor, 10);
        int rightMargin = dp(decor, 4);
        TextView button = new TextView(activity);
        button.setText("↓\n下载");
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setContentDescription("下载当前视频、音频或图片");
        button.setClickable(true);
        button.setFocusable(true);
        button.setElevation(dp(decor, 6));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(0x73000000);
        background.setStroke(dp(decor, 1), 0x66FFFFFF);
        button.setBackground(background);
        button.setOnClickListener(ignored -> {
            if (!moduleEnabled || !canDownload()) {
                removeDownloadButton();
                return;
            }
            Activity currentActivity = activeActivity();
            View currentDecor = activeDecor(currentActivity);
            FeedContentTracker.Snapshot snapshot =
                    FeedContentTracker.current(currentDecor);
            VideoDownloader.chooseDownload(
                    currentActivity,
                    snapshot,
                    () -> FeedContentTracker.current(currentDecor)
            );
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                size,
                size,
                Gravity.TOP | Gravity.END
        );
        params.topMargin = resolveDownloadButtonTop(decor, size, verticalGap);
        params.rightMargin = rightMargin;
        container.addView(button, params);
        button.bringToFront();
        downloadButton = new WeakReference<>(button);
        MAIN.postDelayed(() -> repositionDownloadButton(button, decor), 120L);
        Log.i(DouyinModule.TAG,
                "pause download button shown: top=" + params.topMargin);
    }

    private static void repositionDownloadButton(TextView button, View decor) {
        if (downloadButton.get() != button
                || !canDownload()
                || !(button.getLayoutParams() instanceof FrameLayout.LayoutParams params)) {
            return;
        }
        params.topMargin = resolveDownloadButtonTop(
                decor,
                params.width,
                dp(decor, 10)
        );
        button.setLayoutParams(params);
        button.bringToFront();
    }

    private static void removeDownloadButton() {
        TextView button = downloadButton.get();
        if (button != null) {
            HIDDEN.remove(button);
            button.setOnClickListener(null);
            if (button.getParent() instanceof ViewGroup parent) {
                parent.removeView(button);
            }
        }
        downloadButton.clear();
    }

    private static int resolveDownloadButtonTop(View decor, int size, int gap) {
        int[] rootLocation = new int[2];
        decor.getLocationOnScreen(rootLocation);
        int nativeTop = findActiveRightMenuTop(
                decor,
                rootLocation[0],
                rootLocation[1],
                decor.getWidth(),
                decor.getHeight()
        );
        if (nativeTop == Integer.MAX_VALUE) {
            nativeTop = findRightActionTop(
                    decor,
                    rootLocation[0],
                    rootLocation[1],
                    decor.getWidth(),
                    decor.getHeight()
            );
        }
        int desired = nativeTop == Integer.MAX_VALUE
                ? Math.round(decor.getHeight() * 0.38f)
                : nativeTop - rootLocation[1] - size - gap;
        int minimum = Math.round(decor.getHeight() * 0.20f);
        int maximum = Math.max(minimum, Math.round(decor.getHeight() * 0.60f));
        return Math.max(minimum, Math.min(maximum, desired));
    }

    private static int findActiveRightMenuTop(
            View view,
            int rootLeft,
            int rootTop,
            int rootWidth,
            int rootHeight
    ) {
        int best = Integer.MAX_VALUE;
        if ("com.ss.android.ugc.aweme.feed.ui.FeedRightScaleView"
                .equals(view.getClass().getName())
                && view.isAttachedToWindow()
                && view.isShown()
                && view.getAlpha() > 0.1f) {
            Rect visible = new Rect();
            if (view.getGlobalVisibleRect(visible)
                    && visible.centerX() >= rootLeft + Math.round(rootWidth * 0.72f)
                    && visible.top >= rootTop + Math.round(rootHeight * 0.25f)
                    && visible.height() >= Math.round(rootHeight * 0.30f)) {
                best = visible.top;
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                best = Math.min(
                        best,
                        findActiveRightMenuTop(
                                group.getChildAt(i),
                                rootLeft,
                                rootTop,
                                rootWidth,
                                rootHeight
                        )
                );
            }
        }
        return best;
    }

    private static int findRightActionTop(
            View view,
            int rootLeft,
            int rootTop,
            int rootWidth,
            int rootHeight
    ) {
        int best = Integer.MAX_VALUE;
        if (view != downloadButton.get()
                && view.getVisibility() == View.VISIBLE
                && view.isShown()
                && view.getAlpha() > 0.1f
                && (view.isClickable() || view.getContentDescription() != null)) {
            Rect visible = new Rect();
            if (view.getGlobalVisibleRect(visible)) {
                int centerX = visible.centerX();
                int maxWidth = Math.min(rootWidth / 4, dp(view, 104));
                int maxHeight = dp(view, 128);
                if (centerX >= rootLeft + Math.round(rootWidth * 0.78f)
                        && visible.top >= rootTop + Math.round(rootHeight * 0.30f)
                        && visible.bottom <= rootTop + Math.round(rootHeight * 0.95f)
                        && visible.width() > 0
                        && visible.width() <= maxWidth
                        && visible.height() > 0
                        && visible.height() <= maxHeight) {
                    best = visible.top;
                }
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                best = Math.min(
                        best,
                        findRightActionTop(
                                group.getChildAt(i),
                                rootLeft,
                                rootTop,
                                rootWidth,
                                rootHeight
                        )
                );
            }
        }
        return best;
    }

    private static int dp(View view, int value) {
        return Math.round(
                value * view.getResources().getDisplayMetrics().density
        );
    }

    private static View resolveLargestWindowRoot() {
        try {
            Class<?> globalClass = Class.forName("android.view.WindowManagerGlobal");
            Method getInstance = globalClass.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            Object global = getInstance.invoke(null);
            Field viewsField = globalClass.getDeclaredField("mViews");
            viewsField.setAccessible(true);
            Object value = viewsField.get(global);
            if (!(value instanceof List<?> roots)) {
                return null;
            }

            View best = null;
            long bestArea = 0L;
            for (Object item : roots) {
                if (!(item instanceof View root) || !root.isAttachedToWindow()) {
                    continue;
                }
                long area = (long) root.getWidth() * root.getHeight();
                if (area > bestArea) {
                    best = root;
                    bestArea = area;
                }
            }
            return best;
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (!activityResolveErrorLogged) {
                activityResolveErrorLogged = true;
                Log.e(DouyinModule.TAG, "failed to resolve current window root", error);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Activity resolveTopActivity() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentThreadMethod =
                    activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentThreadMethod.setAccessible(true);
            Object activityThread = currentThreadMethod.invoke(null);
            if (activityThread == null) {
                return null;
            }

            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object records = activitiesField.get(activityThread);
            if (!(records instanceof Map<?, ?> activities)) {
                return null;
            }

            Activity fallback = null;
            for (Object record : activities.values()) {
                if (record == null) {
                    continue;
                }
                Field activityField = record.getClass().getDeclaredField("activity");
                activityField.setAccessible(true);
                Object value = activityField.get(record);
                if (!(value instanceof Activity candidate)
                        || candidate.isFinishing()
                        || candidate.isDestroyed()) {
                    continue;
                }
                fallback = candidate;
                try {
                    Field pausedField = record.getClass().getDeclaredField("paused");
                    pausedField.setAccessible(true);
                    if (!pausedField.getBoolean(record)) {
                        return candidate;
                    }
                } catch (NoSuchFieldException ignored) {
                    return candidate;
                }
            }
            return fallback;
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (!activityResolveErrorLogged) {
                activityResolveErrorLogged = true;
                Log.e(DouyinModule.TAG, "failed to resolve current Activity", error);
            }
            return null;
        }
    }

    private static RenderViews findVisibleRealVideoViews(View root) {
        List<View> candidates = new ArrayList<>();
        List<View> photoCandidates = new ArrayList<>();
        collectRenderViews(root, candidates, photoCandidates);
        List<View> visible = centeredVisibleVideoViews(root, candidates);
        visible.removeIf(ImmersiveUi::isRenderSurfaceUnavailable);
        List<View> preserved = new ArrayList<>(visible);
        // 图文/图集 posts draw the photo in a large image or Skity view while the "video"
        // surface stays black. Merging those layers unconditionally (instead of waiting for
        // the content model) removes the black frame when a video turns into a 图文 post.
        for (View photo : centeredVisibleVideoViews(root, photoCandidates)) {
            if (!preserved.contains(photo)) {
                preserved.add(photo);
            }
        }
        return preserved.isEmpty()
                ? RenderViews.EMPTY
                : new RenderViews(preserved);
    }


    /** True while a comment sheet covers the lower part of the feed. */
    private static boolean isCommentPanelVisible(View decor) {
        int minHeight = Math.max(1, decor.getHeight() / 4);
        return containsVisibleCommentPanel(decor, decor, minHeight);
    }

    private static boolean containsVisibleCommentPanel(
            View node,
            View decor,
            int minHeight
    ) {
        String name = node.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        boolean commentType = name.contains(".comment.")
                || name.contains("commentlist")
                || name.contains("commentpanel")
                || name.contains("commentinput");
        if (commentType && node.isShown()) {
            Rect rect = new Rect();
            if (node.getGlobalVisibleRect(rect) && rect.height() >= minHeight) {
                return true;
            }
        }
        if (node instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                if (containsVisibleCommentPanel(group.getChildAt(index), decor, minHeight)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRenderSurfaceUnavailable(View view) {
        return (view instanceof SurfaceView || view instanceof TextureView)
                && !hasValidRenderSurface(view);
    }


    private static List<View> findVisibleVideoViews(
            View root,
            List<View> visibleRealVideos
    ) {
        if (!visibleRealVideos.isEmpty()) {
            return visibleRealVideos;
        }
        List<View> candidates = new ArrayList<>();
        collectFallbackVideoViews(root, candidates);
        List<View> visible = centeredVisibleVideoViews(root, candidates);
        // A preserved renderer without a valid Surface would show a black picture with all
        // host UI hidden; restoring the UI is always better than a black screen.
        visible.removeIf(ImmersiveUi::isRenderSurfaceUnavailable);
        return visible;
    }

    private static boolean hasValidRenderSurface(View view) {
        try {
            if (view instanceof SurfaceView surfaceView) {
                return surfaceView.getHolder().getSurface() != null
                        && surfaceView.getHolder().getSurface().isValid();
            }
            return view instanceof TextureView textureView
                    && textureView.isAvailable();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isStrictlyVisibleToRoot(View candidate, View root) {
        View current = candidate;
        while (current != null) {
            if (current.getVisibility() != View.VISIBLE
                    || current.getAlpha() <= 0.05f) {
                return false;
            }
            if (current == root) {
                return true;
            }
            if (!(current.getParent() instanceof View parent)) {
                return false;
            }
            current = parent;
        }
        return false;
    }

    private static List<View> centeredVisibleVideoViews(
            View root,
            List<View> candidates
    ) {
        Rect rootVisible = new Rect();
        if (!root.getGlobalVisibleRect(rootVisible)) {
            return Collections.emptyList();
        }
        int centerX = rootVisible.centerX();
        int centerY = rootVisible.centerY();
        long screenArea =
                (long) Math.max(1, rootVisible.width())
                        * Math.max(1, rootVisible.height());
        List<View> videoPaths = new ArrayList<>();
        Rect visible = new Rect();
        for (View candidate : candidates) {
            boolean alphaVisible =
                    candidate.getAlpha() > 0.05f || HIDDEN.containsKey(candidate);
            if (!candidate.isAttachedToWindow()
                    || candidate.getVisibility() != View.VISIBLE
                    || !candidate.isShown()
                    || !alphaVisible
                    || !candidate.getGlobalVisibleRect(visible)
                    || !visible.contains(centerX, centerY)) {
                continue;
            }
            long area = (long) visible.width() * visible.height();
            if (area >= screenArea / 5L
                    && !videoPaths.contains(candidate)) {
                videoPaths.add(candidate);
            }
        }
        return videoPaths.isEmpty()
                ? Collections.emptyList()
                : videoPaths;
    }

    private static void collectRealVideoViews(View view, List<View> out) {
        if (view instanceof SurfaceView || view instanceof TextureView) {
            out.add(view);
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collectRealVideoViews(group.getChildAt(i), out);
            }
        }
    }

    /** Single tree walk filling both the surface and the 图文 content candidates. */
    private static void collectRenderViews(
            View view,
            List<View> surfaces,
            List<View> photos
    ) {
        if (view instanceof SurfaceView || view instanceof TextureView) {
            surfaces.add(view);
        } else {
            String className = view.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            if (view instanceof ImageView) {
                if (className.endsWith("imageview")
                        || className.contains("drawee")
                        || className.contains("photoview")
                        || className.contains("slides")) {
                    photos.add(view);
                }
            } else if (className.contains("functorview")
                    || className.contains("horizon")
                    || className.contains("slidesview")
                    || className.contains("slideview")
                    || className.contains("slidepager")) {
                photos.add(view);
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collectRenderViews(group.getChildAt(i), surfaces, photos);
            }
        }
    }

    private static void expandVideoViewport(View decor, List<View> videos) {
        for (View video : videos) {
            if (expandVideoViewportFrom(decor, video)) {
                return;
            }
        }
    }

    private static boolean expandVideoViewportFrom(View decor, View video) {
        View viewport = video;
        while (viewport.getParent() instanceof View parent && parent != decor) {
            boolean targetViewport = "com.ss.android.ugc.aweme.common.widget.RTViewPager"
                    .equals(viewport.getClass().getName());
            int missingHeight = parent.getHeight() - viewport.getHeight();
            boolean parentFillsWindow =
                    parent.getWidth() >= decor.getWidth() * 0.9f
                            && parent.getHeight() >= decor.getHeight() * 0.95f;
            boolean viewportLeavesBottomSlot =
                    viewport.getWidth() >= decor.getWidth() * 0.9f
                            && missingHeight > 32
                            && missingHeight < decor.getHeight() / 3;
            if (targetViewport && parentFillsWindow && viewportLeavesBottomSlot) {
                applyExpandedViewport(viewport);
                return true;
            }
            viewport = parent;
        }
        return false;
    }

    private static void applyExpandedViewport(View viewport) {
        if (EXPANDED_VIEWPORTS.containsKey(viewport)) {
            return;
        }
        ViewGroup.LayoutParams original = viewport.getLayoutParams();
        if (original == null) {
            return;
        }

        if (!(original instanceof RelativeLayout.LayoutParams relative)) {
            Log.w(DouyinModule.TAG,
                    "RTViewPager layout not RelativeLayout.LayoutParams: "
                            + original.getClass().getName());
            return;
        }
        RelativeLayout.LayoutParams expanded = new RelativeLayout.LayoutParams(relative);
        expanded.height = ViewGroup.LayoutParams.MATCH_PARENT;
        expanded.topMargin = 0;
        expanded.bottomMargin = 0;
        expanded.removeRule(RelativeLayout.ABOVE);
        expanded.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        expanded.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        EXPANDED_VIEWPORTS.put(viewport, original);
        viewport.setLayoutParams(expanded);
        viewport.requestLayout();
        Log.d(DouyinModule.TAG,
                "expanded video viewport: " + viewport.getClass().getName());
    }

    private static void collectFallbackVideoViews(View view, List<View> out) {
        if (!(view instanceof SurfaceView)
                && !(view instanceof TextureView)
                && looksLikeVideoSurface(view)) {
            out.add(view);
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collectFallbackVideoViews(group.getChildAt(i), out);
            }
        }
    }

    private static boolean looksLikeVideoSurface(View view) {
        String name = view.getClass().getName().toLowerCase();
        return name.contains("videosurface")
                || name.contains("playerview")
                || name.endsWith("surfaceview")
                || name.endsWith("textureview");
    }

    private static void collectVisibleDanmakuViews(
            View node,
            List<View> videos,
            List<View> out
    ) {
        String className = node.getClass().getName();
        if (DanmakuViewClassifier.isContainer(className)
                && node.isAttachedToWindow()
                && node.getVisibility() == View.VISIBLE) {
            // 40.8 swaps renderer children (compose/functor/canvas) inside this layer while
            // playing, so the whole subtree has to stay preserved; whitelisting single
            // renderers hid every comment drawn by the others.
            if (!out.contains(node)) {
                out.add(node);
            }
            return;
        }
        if (DanmakuViewClassifier.isRenderView(className)
                && node.isAttachedToWindow()
                && node.getVisibility() == View.VISIBLE
                && overlapsAnyVideo(node, videos)
                && !out.contains(node)) {
            out.add(node);
        }
        if (node instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collectVisibleDanmakuViews(group.getChildAt(i), videos, out);
            }
        }
    }

    private static boolean overlapsAnyVideo(View candidate, List<View> videos) {
        Rect candidateRect = new Rect();
        if (!candidate.getGlobalVisibleRect(candidateRect) || candidateRect.isEmpty()) {
            return false;
        }
        Rect videoRect = new Rect();
        for (View video : videos) {
            if (video.getGlobalVisibleRect(videoRect)
                    && Rect.intersects(candidateRect, videoRect)) {
                return true;
            }
        }
        return false;
    }

    private static void hideOutsidePreservedPaths(View node, List<View> preservedViews) {
        if (preservedViews.contains(node)) {
            return;
        }
        if (!(node instanceof ViewGroup group)
                || !containsAny(group, preservedViews)) {
            hide(node);
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (preservedViews.contains(child)
                    || containsAny(child, preservedViews)) {
                hideOutsidePreservedPaths(child, preservedViews);
            } else {
                hide(child);
            }
        }
    }

    private static boolean containsAny(View node, List<View> targets) {
        for (View target : targets) {
            if (contains(node, target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(View node, View target) {
        if (node == target) {
            return true;
        }
        if (node instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (contains(group.getChildAt(i), target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void restorePreservedPaths(List<View> preservedViews) {
        synchronized (HIDDEN) {
            for (Map.Entry<View, SavedView> entry :
                    new ArrayList<>(HIDDEN.entrySet())) {
                View view = entry.getKey();
                SavedView saved = entry.getValue();
                if (view == null || saved == null
                        || (!preservedViews.contains(view)
                        && !containsAny(view, preservedViews))) {
                    continue;
                }
                view.setAlpha(saved.alpha);
                view.setVisibility(saved.visibility);
                view.setImportantForAccessibility(saved.accessibility);
                HIDDEN.remove(view);
            }
        }
    }

    private static void hide(View view) {
        if (!HIDDEN.containsKey(view)) {
            HIDDEN.put(view, new SavedView(
                    view.getAlpha(),
                    view.getImportantForAccessibility(),
                    view.getVisibility()
            ));
        }
        view.setAlpha(0f);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private static boolean hasHiddenViews() {
        synchronized (HIDDEN) {
            return !HIDDEN.isEmpty();
        }
    }

    /** 隐藏视频进度条（抖X progress.hidden）。 */
    private static void hideProgressBars(View root) {
        if (root == null) {
            return;
        }
        hideProgressBarsRecursive(root);
    }

    private static void hideProgressBarsRecursive(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof android.widget.SeekBar) {
            view.setVisibility(View.GONE);
        } else {
            String cls = view.getClass().getName().toLowerCase();
            if (cls.contains("seekbar")
                    || (cls.contains("progressbar") && !cls.contains("loading"))) {
                view.setVisibility(View.GONE);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                hideProgressBarsRecursive(group.getChildAt(i));
            }
        }
    }

    /** 常驻进度条：把进度条强制设为可见（抖X progress.20ee30e2ae）。 */
    private static void keepProgressBarsVisible(View root) {
        if (root == null) {
            return;
        }
        keepProgressBarsRecursive(root);
    }

    private static void keepProgressBarsRecursive(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof android.widget.SeekBar) {
            view.setVisibility(View.VISIBLE);
        } else {
            String cls = view.getClass().getName().toLowerCase();
            if (cls.contains("seekbar")
                    || (cls.contains("progressbar") && !cls.contains("loading"))) {
                view.setVisibility(View.VISIBLE);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                keepProgressBarsRecursive(group.getChildAt(i));
            }
        }
    }

    /** 自动点击短剧/小游戏广告的"跳过/解锁/领奖"按钮（抖X skipad / playlet_ad / miniapp / extras.d2a3ea57f1）。 */
    private static void autoSkipAd(View decor) {
        if (decor == null || (!autoSkipAd && !autoSkipChapterAd)) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastAdSkipAt < 3_000L) {
            return;
        }
        View button = findAdSkipButton(decor);
        if (button != null && button.isShown()) {
            lastAdSkipAt = now;
            try {
                button.performClick();
            } catch (Throwable ignored) {
                // 点击失败不影响主流程
            }
        }
    }

    /** 将播放器容器背景设为指定颜色，模拟"视频背景"。 */
    private static void applyVideoBackground(View root) {
        View container = findVideoContainer(root);
        if (container == null) {
            return;
        }
        try {
            int color = android.graphics.Color.parseColor(videoBackgroundColor);
            container.setBackgroundColor(color);
        } catch (IllegalArgumentException ignored) {
            // 非法颜色串忽略
        }
    }

    /** 查找视频播放器视图（VideoView/TextureView/SurfaceView），返回其父容器。 */
    private static View findVideoContainer(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                String name = child.getClass().getName().toLowerCase();
                if (name.contains("videoview") || name.contains("textureview")
                        || name.contains("surfaceview")) {
                    return group;
                }
                View found = findVideoContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 图文/实况背景：分别对图片容器与视频容器设置背景色。 */
    private static void applyMediaBackgrounds(View root) {
        if (photoBackgroundColor != null && !photoBackgroundColor.isEmpty()) {
            View container = findImageContainer(root);
            if (container != null) {
                try {
                    container.setBackgroundColor(
                            android.graphics.Color.parseColor(photoBackgroundColor));
                } catch (IllegalArgumentException ignored) {
                    // 非法颜色串忽略
                }
            }
        }
        if (livePhotoBackgroundColor != null && !livePhotoBackgroundColor.isEmpty()) {
            View container = findVideoContainer(root);
            if (container != null) {
                try {
                    container.setBackgroundColor(
                            android.graphics.Color.parseColor(livePhotoBackgroundColor));
                } catch (IllegalArgumentException ignored) {
                    // 非法颜色串忽略
                }
            }
        }
    }

    /** 查找图文容器（ViewPager/图片轮播/大图 ImageView），返回其父容器。 */
    private static View findImageContainer(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                String name = child.getClass().getName().toLowerCase();
                if (name.contains("viewpager") || name.contains("photoview")
                        || name.contains("imagerview") || name.contains("gallery")) {
                    return group;
                }
                if (child instanceof android.widget.ImageView) {
                    int width = child.getWidth();
                    if (width > 0 && width
                            > child.getResources().getDisplayMetrics().widthPixels / 2) {
                        return group;
                    }
                }
                View found = findImageContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 递归查找广告"跳过/解锁/领奖"文本，向上找可点击容器。 */
    private static View findAdSkipButton(View view) {
        if (view instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) view).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if (text.contains("跳过") || text.contains("看广告解锁")
                    || text.contains("立即解锁") || text.contains("继续观看")
                    || text.contains("领取奖励") || text.contains("免费领取")) {
                View cur = view;
                for (int i = 0; i < 4 && cur != null; i++) {
                    if (cur.isClickable()) {
                        return cur;
                    }
                    if (cur.getParent() instanceof ViewGroup) {
                        cur = (ViewGroup) cur.getParent();
                    } else {
                        break;
                    }
                }
                return view;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findAdSkipButton(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 判断当前是否推荐流 Tab：找到文本为"推荐"的 TextView 且其可点击容器处于选中态。 */
    private static boolean isRecommendTab(View root) {
        if (root instanceof TextView) {
            CharSequence cs = ((TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if ("推荐".equals(text)) {
                View cur = root;
                for (int i = 0; i < 5 && cur != null; i++) {
                    if (cur.isSelected()) {
                        return true;
                    }
                    if (cur.getParent() instanceof ViewGroup) {
                        cur = (ViewGroup) cur.getParent();
                    } else {
                        break;
                    }
                }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (isRecommendTab(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 按文本隐藏视图：找到文本命中项后向上提升 levels 层隐藏容器。 */
    private static boolean hideByText(View root, String target, int levels, boolean contains) {
        if (root instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) root).getText();
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

    /** 隐藏首页消息角标（抖X home.3fe35ca5c8）：找到底栏"消息"标签并隐藏其数字/红点角标。 */
    private static void hideMessageBadge(View root) {
        if (root instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if ("消息".equals(text) || text.contains("消息")) {
                View cur = root;
                for (int i = 0; i < 3 && cur.getParent() instanceof ViewGroup; i++) {
                    ViewGroup parent = (ViewGroup) cur.getParent();
                    if (parent == null) {
                        return;
                    }
                    cur = parent;
                    if (hideBadgeInContainer(cur)) {
                        return;
                    }
                }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                hideMessageBadge(group.getChildAt(i));
            }
        }
    }

    /** 在容器内查找并隐藏消息角标（数字角标或红色圆点）。 */
    private static boolean hideBadgeInContainer(View container) {
        if (!(container instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) container;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (isBadgeView(child)) {
                child.setVisibility(View.GONE);
                return true;
            }
        }
        return false;
    }

    /** 角标识别：1-3 位数字小文本，或宽高都很小的红色/橙色圆点。 */
    private static boolean isBadgeView(View v) {
        if (v instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) v).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if (text.isEmpty() || text.length() > 3 || !text.matches("\\d+")) {
                return false;
            }
            return v.getWidth() <= 200 && v.getHeight() <= 100;
        }
        if (v.getWidth() <= 80 && v.getHeight() <= 80
                && v.getWidth() > 0 && v.getHeight() > 0) {
            return isReddishBackground(v);
        }
        return false;
    }

    /** 判断视图背景是否偏红（消息角标红点）。 */
    private static boolean isReddishBackground(View v) {
        try {
            android.graphics.drawable.Drawable d = v.getBackground();
            if (d instanceof android.graphics.drawable.ColorDrawable) {
                int color = ((android.graphics.drawable.ColorDrawable) d).getColor();
                return android.graphics.Color.red(color) > 140
                        && android.graphics.Color.green(color) < 120
                        && android.graphics.Color.blue(color) < 120;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 隐藏整个首页底栏（抖X home.bottom_hidden）。
     *  找到同时含"首页/朋友/消息"文本的底部导航容器，整栏隐藏；
     *  开启保留占位（home.bottom_keep_space）时用 INVISIBLE 保持布局。 */
    private static void hideHomeBottomTabBar(View decor) {
        View bar = findHomeTabBar(decor);
        if (bar != null && bar.getVisibility() == View.VISIBLE) {
            bar.setVisibility(keepHomeBottomBarSpace
                    ? View.INVISIBLE : View.GONE);
        }
    }

    private static View findHomeTabBar(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (group.getChildCount() > 0 && isHomeTabBar(group)) {
                return group;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findHomeTabBar(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean isHomeTabBar(ViewGroup group) {
        int screenHeight;
        try {
            screenHeight = group.getResources().getDisplayMetrics().heightPixels;
        } catch (Throwable ignored) {
            return false;
        }
        int h = group.getHeight();
        if (h <= 0 || h > screenHeight / 4) {
            return false;
        }
        return hasText(group, "首页") && hasText(group, "朋友")
                && hasText(group, "消息");
    }

    /** 定位首页顶部标签栏（抖X glass.top）：文本同底栏但位于屏幕上半部。 */
    private static View findTopTabBar(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (group.getChildCount() > 0 && isTopTabBar(group)) {
                return group;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findTopTabBar(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean isTopTabBar(ViewGroup group) {
        if (!isHomeTabBar(group)) {
            return false;
        }
        try {
            int[] loc = new int[2];
            group.getLocationOnScreen(loc);
            int screenHeight = group.getResources().getDisplayMetrics().heightPixels;
            return loc[1] < screenHeight / 2;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasText(View view, String target) {
        if (view instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) view).getText();
            String text = cs == null ? "" : cs.toString();
            if (text.contains(target)) {
                return true;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasText(group.getChildAt(i), target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 隐藏首页底栏皮肤（抖X home.ba08b02cee）：找到底部导航栏容器并清除背景。 */
    private static void hideBottomBarSkin(View root) {
        if (root instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if ("首页".equals(text) || "朋友".equals(text)
                    || "消息".equals(text) || "我".equals(text)) {
                View cur = root;
                for (int i = 0; i < 3 && cur.getParent() instanceof ViewGroup; i++) {
                    ViewGroup parent = (ViewGroup) cur.getParent();
                    if (parent == null) {
                        return;
                    }
                    cur = parent;
                }
                clearBackgroundRecursive(cur);
                return;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                hideBottomBarSkin(group.getChildAt(i));
            }
        }
    }

    /** 递归清除视图背景（底栏皮肤）。 */
    private static void clearBackgroundRecursive(View v) {
        if (v == null) {
            return;
        }
        try {
            if (v.getBackground() != null) {
                v.setBackground(null);
            }
        } catch (Throwable ignored) {
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                clearBackgroundRecursive(g.getChildAt(i));
            }
        }
    }

    /** 首页按钮刷新前确认（抖X home.6f099dad58）：拦截"刷新"按钮点击并弹确认。 */
    private static void installRefreshConfirm(View root) {
        if (root instanceof android.widget.TextView) {
            CharSequence cs = ((android.widget.TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if ("刷新".equals(text) || text.contains("刷新")) {
                final View btn = root;
                btn.setOnClickListener(v -> {
                    Activity activity = active == null ? null : active.get();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        new android.app.AlertDialog.Builder(activity)
                                .setTitle("确认刷新")
                                .setMessage("确定要刷新首页吗？")
                                .setPositiveButton("刷新", (d, w) -> performOriginalClick(btn, v))
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        performOriginalClick(btn, v);
                    }
                });
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                installRefreshConfirm(group.getChildAt(i));
            }
        }
    }

    /** 通过反射调用被替换前的原始 OnClickListener，避免递归弹窗。 */
    private static void performOriginalClick(View btn, View current) {
        try {
            java.lang.reflect.Field infoField = View.class.getDeclaredField("mListenerInfo");
            infoField.setAccessible(true);
            Object info = infoField.get(btn);
            if (info != null) {
                java.lang.reflect.Field clickField = info.getClass().getDeclaredField("mOnClickListener");
                clickField.setAccessible(true);
                View.OnClickListener original = (View.OnClickListener) clickField.get(info);
                if (original != null && original != current) {
                    original.onClick(btn);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        btn.performClick();
    }

    /** 音乐封面随右侧栏移动（抖X controls.a98f8afb28）：为旋转的圆形音乐碟片启用拖动。 */
    private static void enableMusicDiscDrag(View root) {
        if (root instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) root;
            int w = iv.getWidth();
            int h = iv.getHeight();
            if (w > 0 && h > 0 && Math.abs(w - h) < 20 && w < 400
                    && iv.getVisibility() == View.VISIBLE
                    && iv.getAnimation() instanceof android.view.animation.RotateAnimation) {
                installDrag(iv);
                return;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                enableMusicDiscDrag(group.getChildAt(i));
            }
        }
    }

    /** 视频右侧栏样式（抖X style__right）：对右侧操作栏应用透明度/位移/缩放。 */
    private static void applyRightBarStyle(View root) {
        boolean defaultOpacity = rightBarOpacity >= 100
                && rightBarPausedOpacity >= 100
                && !rightBarPausedSeparate;
        boolean defaultTransform = rightBarDx == 0 && rightBarDy == 0
                && rightBarScale >= 0.99f && rightBarScale <= 1.01f;
        if (defaultOpacity && defaultTransform) {
            return;
        }
        View bar = findRightBar(root);
        if (bar == null) {
            return;
        }
        try {
            int opacity = rightBarOpacity;
            if (rightBarPausedSeparate && !PlaybackState.isPlaying()) {
                opacity = rightBarPausedOpacity;
            }
            float alpha = Math.max(0f, Math.min(1f, opacity / 100f));
            bar.setAlpha(alpha);
            if (rightBarDx != 0 || rightBarDy != 0) {
                bar.setTranslationX(dp(bar, rightBarDx));
                bar.setTranslationY(dp(bar, rightBarDy));
            }
            if (rightBarScale > 0) {
                bar.setScaleX(rightBarScale);
                bar.setScaleY(rightBarScale);
            }
        } catch (Throwable ignored) {
            // 布局变化或异常时跳过本轮
        }
    }

    /** 定位视频右侧操作栏：屏幕右侧、较高、含多个按钮的容器。 */
    private static View findRightBar(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            if (group.getWidth() > 0 && group.getHeight() > 200
                    && group.getChildCount() >= 2) {
                try {
                    int[] loc = new int[2];
                    group.getLocationOnScreen(loc);
                    int screenW = group.getResources().getDisplayMetrics().widthPixels;
                    if (loc[0] + group.getWidth() > screenW * 0.55f
                            && containsRightBarHint(group)) {
                        return group;
                    }
                } catch (Throwable ignored) {
                    // 视图未附加到窗口时跳过
                }
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findRightBar(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ---------- 玻璃扩展（glass.avatar / glass.double / glass.fps）----------
    // 说明：以下为可用近似实现；头像为占位视图，待逆向出登录用户头像接口后替换为真实头像。

    private static TextView glassFpsView;
    private static Choreographer.FrameCallback glassFpsCallback;
    private static long glassFpsBaseNanos;
    private static int glassFpsCount;

    /** 顶栏实时帧率（抖X glass.fps）：在状态栏下方居中显示 FPS。 */
    private static void updateFpsOverlay(View decor) {
        if (!(decor instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) decor;
        TextView tv = glassFpsView;
        if (tv == null || tv.getParent() == null) {
            tv = new TextView(root.getContext());
            tv.setIncludeFontPadding(false);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setTextColor(Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(tv, 8), dp(tv, 2), dp(tv, 8), dp(tv, 2));
            tv.setBackgroundColor(0x99000000);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            lp.topMargin = statusBarHeight(root.getContext()) + dp(tv, 6);
            root.addView(tv, lp);
            glassFpsView = tv;
            glassFpsBaseNanos = 0;
            glassFpsCount = 0;
        }
        if (glassFpsCallback == null) {
            glassFpsCallback = frameTimeNanos -> {
                if (glassFpsView == null) {
                    return;
                }
                if (glassFpsBaseNanos == 0) {
                    glassFpsBaseNanos = frameTimeNanos;
                } else {
                    glassFpsCount++;
                    double seconds = (frameTimeNanos - glassFpsBaseNanos) / 1_000_000_000.0;
                    if (seconds >= 1.0) {
                        int fps = (int) Math.round(glassFpsCount / seconds);
                        glassFpsView.setText("FPS " + fps);
                        glassFpsCount = 0;
                        glassFpsBaseNanos = frameTimeNanos;
                    }
                }
                Choreographer.getInstance().postFrameCallback(this);
            };
            Choreographer.getInstance().postFrameCallback(glassFpsCallback);
        }
    }

    private static void removeFpsOverlay() {
        if (glassFpsCallback != null) {
            try {
                Choreographer.getInstance().removeFrameCallback(glassFpsCallback);
            } catch (Throwable ignored) {
                // 主循环未运行时忽略
            }
            glassFpsCallback = null;
        }
        if (glassFpsView != null) {
            if (glassFpsView.getParent() instanceof ViewGroup) {
                ((ViewGroup) glassFpsView.getParent()).removeView(glassFpsView);
            }
            glassFpsView = null;
        }
        glassFpsBaseNanos = 0;
        glassFpsCount = 0;
    }

    private static TextView glassAvatarView;

    /** 底栏账号头像（抖X glass.avatar）：在底部导航"我"标签上方叠加圆形占位头像。 */
    private static void updateAvatarOverlay(View decor) {
        if (!(decor instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) decor;
        TextView av = glassAvatarView;
        if (av == null || av.getParent() == null) {
            av = new TextView(root.getContext());
            av.setText("我");
            av.setTextColor(Color.WHITE);
            av.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            av.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xCCFF2D55);
            av.setBackground(bg);
            int size = dp(av, 32);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    size, size, Gravity.BOTTOM);
            try {
                int screenW = root.getResources().getDisplayMetrics().widthPixels;
                lp.leftMargin = screenW * 4 / 5 - size / 2;
            } catch (Throwable ignored) {
                lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            }
            lp.bottomMargin = dp(av, 6);
            root.addView(av, lp);
            glassAvatarView = av;
        }
    }

    private static void removeAvatarOverlay() {
        if (glassAvatarView != null) {
            if (glassAvatarView.getParent() instanceof ViewGroup) {
                ((ViewGroup) glassAvatarView.getParent()).removeView(glassAvatarView);
            }
            glassAvatarView = null;
        }
    }

    private static TextView glassDoubleView;

    /** 顶栏双列浏览入口（抖X glass.double）：在状态栏下方右侧显示"双列"入口占位。 */
    private static void updateDoubleOverlay(View decor) {
        if (!(decor instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) decor;
        TextView dv = glassDoubleView;
        if (dv == null || dv.getParent() == null) {
            dv = new TextView(root.getContext());
            dv.setText("双列");
            dv.setTextColor(Color.WHITE);
            dv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            dv.setGravity(Gravity.CENTER);
            dv.setPadding(dp(dv, 10), dp(dv, 4), dp(dv, 10), dp(dv, 4));
            dv.setBackgroundColor(0x99000000);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.RIGHT);
            lp.topMargin = statusBarHeight(root.getContext()) + dp(dv, 6);
            lp.rightMargin = dp(dv, 12);
            root.addView(dv, lp);
            glassDoubleView = dv;
        }
    }

    private static void removeDoubleOverlay() {
        if (glassDoubleView != null) {
            if (glassDoubleView.getParent() instanceof ViewGroup) {
                ((ViewGroup) glassDoubleView.getParent()).removeView(glassDoubleView);
            }
            glassDoubleView = null;
        }
    }

    private static int statusBarHeight(android.content.Context context) {
        try {
            int id = context.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            return id > 0 ? context.getResources().getDimensionPixelSize(id) : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** 右侧栏特征：容器内存在点赞/评论/收藏/分享等按钮提示。 */
    private static boolean containsRightBarHint(View root) {
        if (root instanceof TextView) {
            CharSequence cs = ((TextView) root).getText();
            String text = cs == null ? "" : cs.toString();
            if (text.contains("点赞") || text.contains("评论")
                    || text.contains("收藏") || text.contains("分享")) {
                return true;
            }
        }
        CharSequence desc = root.getContentDescription();
        if (desc != null) {
            String d = desc.toString();
            if (d.contains("赞") || d.contains("评论")
                    || d.contains("收藏") || d.contains("分享")) {
                return true;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsRightBarHint(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 为视图安装单指拖动（保存触摸偏移，移动时更新位置）。 */
    private static void installDrag(final View v) {
        if (v.getTag() instanceof String && DRAG_TAG.equals(v.getTag())) {
            return;
        }
        v.setTag(DRAG_TAG);
        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    dragDx = event.getRawX() - view.getX();
                    dragDy = event.getRawY() - view.getY();
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    view.setX(event.getRawX() - dragDx);
                    view.setY(event.getRawY() - dragDy);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return false;
            }
        });
    }

    private static final String DRAG_TAG = "douyim_drag_enabled";

    private static void updateGesturePaths(View root, List<View> videos) {
        GESTURE_PATHS.clear();
        collectGesturePaths(root, root, videos);
    }

    private static void collectGesturePaths(View node, View root, List<View> videos) {
        if (ImmersiveTouchPolicy.isGestureView(node.getClass().getName())
                && node.isAttachedToWindow()
                && node.getVisibility() == View.VISIBLE
                && node.getWidth() >= root.getWidth() * 0.8f
                && node.getHeight() >= root.getHeight() * 0.5f
                && overlapsAnyVideo(node, videos)) {
            // Keep only the gesture receiver and its ancestors touchable. A sibling button
            // inside the same transparent container must still fail the hidden-ancestor check.
            View path = node;
            while (path != null) {
                GESTURE_PATHS.add(path);
                path = path.getParent() instanceof View parent ? parent : null;
            }
        }
        if (node instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collectGesturePaths(group.getChildAt(i), root, videos);
            }
        }
    }

    static boolean isImmersiveGestureView(View view) {
        return moduleEnabled && immersiveEnabled && hasHiddenViews() && GESTURE_PATHS.contains(view)
                && ImmersiveTouchPolicy.isGestureView(view.getClass().getName());
    }

    static boolean shouldBlockHiddenTouch(View view, boolean cancel) {
        if (isSyntheticTapActive()) {
            // Our own comment tap has to reach the hidden right bar.
            return false;
        }
        if (!moduleEnabled || !immersiveEnabled || cancel || !hasHiddenViews()) {
            return false;
        }
        boolean hidden = false;
        View path = view;
        while (path != null) {
            if (HIDDEN.containsKey(path)) {
                hidden = true;
                break;
            }
            path = path.getParent() instanceof View parent ? parent : null;
        }
        return ImmersiveTouchPolicy.shouldBlock(hidden, GESTURE_PATHS.contains(view), cancel);
    }

    private static void reassertHiddenViews() {
        if (!moduleEnabled || !immersiveEnabled) return;
        synchronized (HIDDEN) {
            for (Map.Entry<View, SavedView> entry :
                    new ArrayList<>(HIDDEN.entrySet())) {
                View view = entry.getKey();
                if (view != null && view.isAttachedToWindow()
                        && view.getAlpha() != 0f) {
                    view.setAlpha(0f);
                }
            }
        }
    }

    private static void attachHiddenViewGuard(View root) {
        View previous = hiddenGuardRoot.get();
        if (previous == root) {
            return;
        }
        detachHiddenViewGuard();
        if (root == null) {
            return;
        }
        ViewTreeObserver observer = root.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnPreDrawListener(HIDDEN_VIEW_GUARD);
            hiddenGuardRoot = new WeakReference<>(root);
        }
    }

    private static void detachHiddenViewGuard() {
        View root = hiddenGuardRoot.get();
        hiddenGuardRoot.clear();
        if (root == null) {
            return;
        }
        ViewTreeObserver observer = root.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnPreDrawListener(HIDDEN_VIEW_GUARD);
        }
    }

    private static void restoreAll(Activity activity) {
        restoreAll(activity, activeDecor(activity), true);
    }

    private static void restoreAll(Activity activity, View decor) {
        restoreAll(activity, decor, false);
    }

    private static void restoreAll(
            Activity activity,
            View decor,
            boolean leavingActivity
    ) {
        if (leavingActivity) {
            detachHiddenViewGuard();
        }
        // The feed description is bottom-aligned inside the expanded pager.
        // Restore its original bounds before showing the bottom navigation,
        // including when playback pauses without leaving the activity.
        restoreExpandedViewports();
        int restored = 0;
        StringBuilder samples = new StringBuilder();
        synchronized (HIDDEN) {
            for (Map.Entry<View, SavedView> entry : new ArrayList<>(HIDDEN.entrySet())) {
                View view = entry.getKey();
                SavedView saved = entry.getValue();
                if (view != null && saved != null) {
                    view.setAlpha(saved.alpha);
                    view.setVisibility(saved.visibility);
                    view.setImportantForAccessibility(saved.accessibility);
                    if (restored < 5) {
                        if (samples.length() > 0) {
                            samples.append(',');
                        }
                        samples.append(view.getClass().getSimpleName())
                                .append('@')
                                .append(saved.alpha);
                    }
                    restored++;
                }
            }
            HIDDEN.clear();
            GESTURE_PATHS.clear();
        }
        if (restored > 0) {
            Log.d(DouyinModule.TAG,
                    "restored hidden views=" + restored + " samples=" + samples);
        }
        if (decor != null) {
            Integer systemUi = ROOT_SYSTEM_UI.remove(decor);
            if (systemUi != null) {
                decor.setSystemUiVisibility(systemUi);
            }
        }
        restoreSystemBars(activity);
    }

    private static void restoreExpandedViewports() {
        synchronized (EXPANDED_VIEWPORTS) {
            for (Map.Entry<View, ViewGroup.LayoutParams> entry :
                    new ArrayList<>(EXPANDED_VIEWPORTS.entrySet())) {
                View view = entry.getKey();
                ViewGroup.LayoutParams original = entry.getValue();
                if (view != null && original != null) {
                    view.setLayoutParams(original);
                    view.requestLayout();
                }
            }
            EXPANDED_VIEWPORTS.clear();
        }
    }

    private static void hideSystemBars(Activity activity, View decor) {
        if (!ROOT_SYSTEM_UI.containsKey(decor)) {
            ROOT_SYSTEM_UI.put(decor, decor.getSystemUiVisibility());
        }
        // 原生沉浸模式：全隐藏。抖X桥接开关：按开关只隐藏对应系统栏。
        boolean hideStatus = immersiveEnabled || hideStatusBar;
        boolean hideNav = immersiveEnabled || hideNavBar || transparentNavBar;
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (hideStatus) {
            flags |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }
        if (hideNav) {
            // 透明导航栏：内容延伸过去但保留导航栏可见
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!transparentNavBar) {
                flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
        }
        decor.setSystemUiVisibility(
                decor.getSystemUiVisibility() | flags
        );
    }

    private static void restoreSystemBars(Activity activity) {
        // Window flags are deliberately left untouched; see hideSystemBars.
    }

    private static boolean swipeToNext(View decor, String reason) {
        if (!moduleEnabled) return false;
        long now = SystemClock.uptimeMillis();
        if (swipeRunning
                || !decor.isAttachedToWindow()
                || now - lastSwipeAt < 1_500L) {
            return false;
        }
        // Ad pages (WebView, 小程序, 互动广告) swallow a gesture aimed at the decor view.
        // Dispatching straight into the feed's own pager bypasses those overlays.
        View gestureTarget = FeedContentTracker.currentPager(decor);
        if (gestureTarget == null || !gestureTarget.isAttachedToWindow()) {
            gestureTarget = decor;
        }
        final View target = gestureTarget;
        int width = target.getWidth();
        int height = target.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        swipeRunning = true;
        long currentSwipeToken = ++swipeToken;
        lastSwipeAt = now;
        PlaybackState.beginAutoSwitch();
        PlaybackState.resetProgressAfterSwipe();
        armContentFilter(700L);
        Log.i(DouyinModule.TAG, "swipe to next feed item: " + reason
                + " target=" + target.getClass().getSimpleName()
                + " " + width + "x" + height);

        final float x = width * 0.5f;
        final float startY = height * 0.72f;
        final float endY = height * 0.24f;
        final long downTime = SystemClock.uptimeMillis();
        try {
            dispatch(target, downTime, downTime, MotionEvent.ACTION_DOWN, x, startY);
        } catch (Throwable error) {
            finishSwipe(currentSwipeToken, "initial dispatch failed", error);
            return false;
        }
        MAIN.postDelayed(
                () -> finishSwipe(currentSwipeToken, "watchdog timeout", null),
                1_200L
        );

        int steps = 8;
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            MAIN.postDelayed(() -> {
                if (!swipeRunning || currentSwipeToken != swipeToken) {
                    return;
                }
                if (!moduleEnabled) {
                    dispatch(target, downTime, SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_CANCEL, x, startY);
                    finishSwipe(currentSwipeToken, "module disabled", null);
                    return;
                }
                float fraction = step / (float) steps;
                float y = startY + (endY - startY) * fraction;
                int action = step == steps ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
                try {
                    dispatch(
                            target,
                            downTime,
                            SystemClock.uptimeMillis(),
                            action,
                            x,
                            y
                    );
                } catch (Throwable error) {
                    finishSwipe(currentSwipeToken, "gesture dispatch failed", error);
                    return;
                }
                if (step == steps) {
                    MAIN.postDelayed(
                            () -> finishSwipe(currentSwipeToken, null, null),
                            500L
                    );
                }
            }, i * 22L);
        }
        return true;
    }

    private static void finishSwipe(long token, String reason, Throwable error) {
        if (token != swipeToken || !swipeRunning) {
            return;
        }
        swipeRunning = false;
        if (reason != null) {
            if (error == null) {
                Log.w(DouyinModule.TAG,
                        "synthetic swipe recovered: " + reason);
            } else {
                Log.w(DouyinModule.TAG,
                        "synthetic swipe recovered: " + reason, error);
            }
        }
        scheduleScan(0L);
    }

    /** Real touch injection; some feed buttons only react to touch, not performClick(). */
    private static boolean tapView(View decor, View target) {
        Rect rect = new Rect();
        if (!target.getGlobalVisibleRect(rect) || rect.isEmpty()) {
            return false;
        }
        long now = SystemClock.uptimeMillis();
        syntheticTapUntil = now + 400L;
        float x = rect.centerX();
        float y = rect.centerY();
        try {
            dispatch(decor, now, now, MotionEvent.ACTION_DOWN, x, y);
            dispatch(decor, now, now + 45L, MotionEvent.ACTION_UP, x, y);
            return true;
        } catch (Throwable error) {
            Log.w(DouyinModule.TAG, "comment tap dispatch failed", error);
            return false;
        }
    }

    private static boolean isSyntheticTapActive() {
        return SystemClock.uptimeMillis() < syntheticTapUntil;
    }

    private static void dispatch(View view, long downTime, long eventTime,
                                 int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            view.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }

    private static void armContentFilter(long delayMs) {
        long now = SystemClock.uptimeMillis();
        contentCheckNotBefore = now + delayMs;
        contentCheckUntil = now + Math.max(delayMs + 2_500L, 3_000L);
        lastContentCheckAt = 0L;
        resetFilterCandidate();
    }

    private static boolean checkCurrentFeedContent(View decor) {
        long now = SystemClock.uptimeMillis();
        boolean candidatePending =
                filterCandidateCount > 0 && now - filterCandidateAt <= 900L;
        if (now < contentCheckNotBefore) {
            return candidatePending;
        }
        boolean activelyArmed = now <= contentCheckUntil || candidatePending;
        long minimumInterval = activelyArmed
                ? 220L
                : FALLBACK_CONTENT_CHECK_INTERVAL_MS;
        if (now - lastContentCheckAt < minimumInterval
                || now - lastFilteredSwipeAt < 1_500L) {
            return candidatePending;
        }
        lastContentCheckAt = now;

        FeedContentTracker.Snapshot model = FeedContentTracker.current(decor);
        PlaybackState.setContentAvailable(model != null);
        if (model == null) {
            // 离开信息流（搜索页、个人页等）后不要留下上次的图文标记。
            PlaybackState.setPhotoContent(false);
        }
        if (model != null) {
            PlaybackState.setPhotoContent(model.photoContent);
            if (PlaybackState.clearUserPauseForContentChange(model.aid)) {
                transitionBoostUntil = Math.max(
                        transitionBoostUntil,
                        now + 1_200L
                );
            }
            String reason = model.shouldFilter()
                    ? model.filterReason
                    : null;
            if (reason != null) {
                if (!activelyArmed) {
                    contentCheckNotBefore = 0L;
                    contentCheckUntil = now + 2_500L;
                }
                if (!confirmFilterCandidate(model.aid, reason, now)) {
                    return true;
                }
                filterCurrentItem(
                        decor,
                        reason + " " + model.classificationDetails()
                );
                return true;
            }
            resetFilterCandidate();
            if (!model.aid.equals(lastAcceptedAid)) {
                lastAcceptedAid = model.aid;
                Log.d(DouyinModule.TAG,
                        "feed item accepted as video: "
                                + model.classificationDetails());
            }
            return false;
        }

        if (!candidatePending) {
            resetFilterCandidate();
        }
        return candidatePending;
    }

    private static boolean confirmFilterCandidate(
            String aid,
            String reason,
            long now
    ) {
        boolean sameCandidate =
                aid != null
                        && aid.equals(filterCandidateAid)
                        && reason.equals(filterCandidateReason)
                        && now - filterCandidateAt <= 750L;
        if (sameCandidate) {
            filterCandidateCount++;
        } else {
            filterCandidateAid = aid;
            filterCandidateReason = reason;
            filterCandidateCount = 1;
        }
        filterCandidateAt = now;
        Log.d(DouyinModule.TAG,
                "filter candidate " + filterCandidateCount + "/3: "
                        + reason + " aid=" + aid);
        return filterCandidateCount >= 3;
    }

    private static void resetFilterCandidate() {
        filterCandidateAid = null;
        filterCandidateReason = null;
        filterCandidateCount = 0;
        filterCandidateAt = 0L;
    }

    private static boolean filterCurrentItem(View decor, String reason) {
        long now = SystemClock.uptimeMillis();
        if (now < filterSuspendedUntil) {
            return false;
        }
        if (swipeRunning || now - lastFilteredSwipeAt < 1_500L) {
            return false;
        }
        // Safety valve: a mis-classified batch must never turn the whole feed into a
        // slideshow. Six skips inside 20s suspends filtering for a minute.
        if (now - filterBurstStart > 20_000L) {
            filterBurstStart = now;
            filterBurstCount = 0;
        }
        if (++filterBurstCount > 6) {
            filterSuspendedUntil = now + 60_000L;
            Log.w(DouyinModule.TAG,
                    "filter suspended for 60s after " + filterBurstCount
                            + " consecutive skips");
            filterBurstCount = 0;
            return false;
        }
        if (!swipeToNext(decor, reason)) {
            return false;
        }
        lastFilteredSwipeAt = now;
        Log.i(DouyinModule.TAG, "filtering feed item: " + reason);
        return true;
    }

    private static final class SavedView {
        final float alpha;
        final int accessibility;
        final int visibility;

        SavedView(float alpha, int accessibility, int visibility) {
            this.alpha = alpha;
            this.accessibility = accessibility;
            this.visibility = visibility;
        }
    }

    private static final class RenderViews {
        static final RenderViews EMPTY =
                new RenderViews(Collections.emptyList());

        final List<View> visibleVideos;

        RenderViews(List<View> visibleVideos) {
            this.visibleVideos = visibleVideos;
        }
    }

}
