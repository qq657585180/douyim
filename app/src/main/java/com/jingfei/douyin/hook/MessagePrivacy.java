package com.jingfei.douyin.hook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jingfei.douyin.prefs.MessagePrefs;

import java.lang.ref.WeakReference;

/**
 * 消息隐私（douyin_message）Hook 子系统。
 * 已实现：隐藏消息页顶部"朋友"入口（message.friends）。
 * 待补充：聊天工具/已读输入状态/撤回续火/聊天导出等依赖抖音内部消息协议，暂无稳定 API 依据。
 */
final class MessagePrivacy {
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Runnable SCAN = MessagePrivacy::scanMessageUi;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile boolean scanning;
    private static long lastReadAllClickAt;
    private static long lastFireClickAt;
    private static final int MESSAGE_MENU_TAG_KEY = 0x2d0a5f01;
    private static final int REVOKE_TAG_KEY = 0x2d0a5f02;
    /** 最近一条非撤回消息文本，用于恢复"撤回了一条消息"的原文。 */
    private static String lastSeenMessageText;

    private MessagePrivacy() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_message");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            module.log(Log.INFO, DouyinModule.TAG,
                    "MessagePrivacy installed (douyin_message)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "MessagePrivacy skipped: " + error);
        }
    }

    /** Activity 可见后开始扫描消息页（由 DouyinModule 生命周期 Hook 调用）。 */
    static void onActivityResumed(Activity activity) {
        currentActivity = new WeakReference<>(activity);
        if (!scanning) {
            scanning = true;
            HANDLER.post(SCAN);
        }
    }

    static void onActivityPaused(Activity activity) {
        if (currentActivity != null && currentActivity.get() == activity) {
            currentActivity = null;
        }
    }

    /** 消息页 UI 增强：隐藏顶部"朋友"入口、一键已读。 */
    private static void scanMessageUi() {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scanning = false;
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                // 隐藏消息页顶部"朋友"标签（message.friends）
                if (bool(MessagePrefs.KEY_MESSAGE_FRIENDS, false)) {
                    hideByText(decor, "朋友", 1, true);
                }
                // 消息一键已读（messages.5583c877cb）：找到"已读"按钮并点击
                if (bool(MessagePrefs.KEY_MESSAGES_5583C877CB, false)) {
                    clickReadAll(decor);
                }
                // 自动续火（messages.ff3e0ab8e6）：找到"送火花/续火"提示并点击
                if (bool(MessagePrefs.KEY_MESSAGES_FF3E0AB8E6, false)) {
                    clickFireKeep(decor);
                }
                // 消息长按工具（messages.3e3feb2a50）：长按消息项弹出操作菜单
                if (bool(MessagePrefs.KEY_MESSAGES_3E3FEB2A50, false)
                        || bool(MessagePrefs.KEY_MESSAGES_6E933E99E2, false)) {
                    installMessageLongClick(decor);
                }
                // 隐私：显示本地撤回原文（privacy.c7ea651bf4）
                if (bool(MessagePrefs.KEY_PRIVACY_C7EA651BF4, false)) {
                    restoreRevokedMessage(decor);
                }
            }
        } catch (Throwable ignored) {
            // 视图结构变化或权限异常时跳过本轮
        }
        HANDLER.postDelayed(SCAN, SCAN_INTERVAL_MS);
    }

    /** 查找并点击消息页的"一键已读/全部已读"按钮（3 秒防重）。 */
    private static void clickReadAll(View decor) {
        long now = SystemClock.uptimeMillis();
        if (now - lastReadAllClickAt < 3_000L) {
            return;
        }
        View button = findReadAllButton(decor);
        if (button != null && button.isShown()) {
            lastReadAllClickAt = now;
            try {
                button.performClick();
            } catch (Throwable ignored) {
                // 点击失败不影响主流程
            }
        }
    }

    /** 递归查找文本含"已读"的按钮，向上找可点击容器。 */
    private static View findReadAllButton(View view) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if (text.contains("一键已读") || text.contains("全部已读")
                    || text.contains("已读全部") || text.contains("标记已读")) {
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
                View found = findReadAllButton(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 自动续火：在聊天页找到"送火花/续火花"提示并点击（60 秒防重）。 */
    private static void clickFireKeep(View decor) {
        long now = SystemClock.uptimeMillis();
        if (now - lastFireClickAt < 60_000L) {
            return;
        }
        View button = findFireButton(decor);
        if (button != null && button.isShown()) {
            lastFireClickAt = now;
            try {
                button.performClick();
            } catch (Throwable ignored) {
                // 点击失败不影响主流程
            }
        }
    }

    /** 递归查找文本含"火花/续火"的可点击容器。 */
    private static View findFireButton(View view) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            String text = cs == null ? "" : cs.toString().trim();
            if (text.contains("火花") || text.contains("续火")) {
                View cur = view;
                for (int i = 0; i < 5 && cur != null; i++) {
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
                View found = findFireButton(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 消息长按工具：在聊天消息列表的每一项上注册长按监听（messages.3e3feb2a50）。
     *  开启导出时菜单含"导出聊天记录"（messages.6e933e99e2）。 */
    private static void installMessageLongClick(View decor) {
        View rv = findChatRecyclerView(decor);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        for (int i = 0; i < group.getChildCount(); i++) {
            final View child = group.getChildAt(i);
            if (child == null || child.getTag(MESSAGE_MENU_TAG_KEY) != null) {
                continue;
            }
            child.setTag(MESSAGE_MENU_TAG_KEY, Boolean.TRUE);
            child.setOnLongClickListener(v -> {
                showMessageMenu(group, child);
                return true;
            });
        }
    }

    /** 在 decor 中查找聊天消息列表：类名含 RecyclerView 且所在页面有输入框 EditText。 */
    private static View findChatRecyclerView(View view) {
        if (view == null) {
            return null;
        }
        try {
            if (view.getClass().getName().contains("RecyclerView")) {
                if (containsEditText(view)) {
                    return view;
                }
            }
        } catch (Throwable ignored) {
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findChatRecyclerView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean containsEditText(View view) {
        if (view instanceof android.widget.EditText) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsEditText(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void showMessageMenu(final ViewGroup list, final View messageView) {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null) {
            return;
        }
        final String text = collectTexts(messageView);
        List<String> menu = new ArrayList<>();
        if (text.length() > 0) {
            menu.add("复制消息文本");
        }
        if (bool(MessagePrefs.KEY_MESSAGES_6E933E99E2, false)) {
            menu.add("导出聊天记录");
        }
        if (menu.isEmpty()) {
            return;
        }
        final String[] items = menu.toArray(new String[0]);
        new AlertDialog.Builder(activity)
                .setTitle("消息操作")
                .setItems(items, (d, which) -> {
                    String chosen = items[which];
                    if ("复制消息文本".equals(chosen)) {
                        copyText(activity, text);
                        toast(activity, "已复制");
                    } else if ("导出聊天记录".equals(chosen)) {
                        exportChat(activity, list);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 收集视图内所有 TextView 文本，按行合并。 */
    private static String collectTexts(View view) {
        if (view == null) {
            return "";
        }
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            String s = cs == null ? "" : cs.toString().trim();
            return s.length() > 0 ? s : "";
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < group.getChildCount(); i++) {
                String s = collectTexts(group.getChildAt(i));
                if (s.length() > 0 && !parts.contains(s)) {
                    parts.add(s);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(p);
            }
            return sb.toString();
        }
        return "";
    }

    /** 导出聊天记录：把当前可见消息文本写入系统下载目录（MediaStore Downloads）。 */
    private static void exportChat(Activity activity, ViewGroup list) {
        try {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < list.getChildCount(); i++) {
                String t = collectTexts(list.getChildAt(i));
                if (t.length() > 0) {
                    lines.add(t);
                }
            }
            if (lines.isEmpty()) {
                toast(activity, "没有可导出的消息");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(line);
            }
            String content = sb.toString();
            String fileName = "douyin_chat_"
                    + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                            java.util.Locale.CHINA)
                    .format(new java.util.Date()) + ".txt";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            Uri uri = activity.getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = activity.getContentResolver()
                        .openOutputStream(uri);
                if (os != null) {
                    os.write(content.getBytes("UTF-8"));
                    os.close();
                    toast(activity, "已导出：" + fileName);
                }
            }
        } catch (Throwable ignored) {
            toast(activity, "导出失败");
        }
    }

    /** 隐私（privacy.c7ea651bf4）：检测聊天列表中的"撤回了一条消息"提示，
     *  并在该消息项上追加显示撤回前的原文（来自上一次扫描缓存的非撤回消息文本）。 */
    private static void restoreRevokedMessage(View decor) {
        View rv = findChatRecyclerView(decor);
        if (!(rv instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) rv;
        String previous = lastSeenMessageText;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child == null) {
                continue;
            }
            String text = collectTexts(child);
            if (text.length() == 0) {
                continue;
            }
            if (text.contains("撤回")) {
                if (previous.length() > 0
                        && child.getTag(REVOKE_TAG_KEY) == null) {
                    child.setTag(REVOKE_TAG_KEY, Boolean.TRUE);
                    appendRevokedOriginal(child, previous);
                }
            } else {
                previous = text;
            }
        }
        lastSeenMessageText = previous;
    }

    /** 在撤回消息项上追加一行灰色小字显示原文。 */
    private static void appendRevokedOriginal(View messageView, String original) {
        if (!(messageView instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) messageView;
        TextView originalView = new TextView(group.getContext());
        originalView.setText("已撤回原文：" + original);
        originalView.setTextSize(13f);
        originalView.setTextColor(0xFF888888);
        originalView.setPadding(dp(6), dp(2), dp(6), dp(2));
        group.addView(originalView);
    }

    /** dp → px。 */
    private static int dp(int value) {
        return Math.round(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, value,
                android.content.res.Resources.getSystem().getDisplayMetrics()));
    }

    private static void copyText(Activity activity, String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        ClipboardManager cm = (ClipboardManager) activity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("douyin_message", text));
        }
    }

    private static void toast(Activity activity, String message) {
        try {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
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

    private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER =
            (preferences, key) -> {
                // 配置变更后无需额外处理：Hook 实时读取 getBoolean/getInt/getString
            };
}
