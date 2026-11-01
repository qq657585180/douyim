package com.jingfei.douyin.hook;

final class ImmersiveTransitionPolicy {
    private ImmersiveTransitionPolicy() {
    }

    static boolean shouldHoldHiddenUi(
            boolean hasHiddenViews,
            long now,
            long transitionUntil
    ) {
        return hasHiddenViews && now < transitionUntil;
    }
}
