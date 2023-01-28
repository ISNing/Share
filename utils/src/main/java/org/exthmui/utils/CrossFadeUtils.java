package org.exthmui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import androidx.annotation.NonNull;

public abstract class CrossFadeUtils {
    public static void fadeIn(@NonNull View view, long duration, Animator.AnimatorListener listener) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(listener).start();
    }

    public static void fadeOut(@NonNull View view, long duration, Animator.AnimatorListener listener) {
        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(listener).start();
    }

    public static void fadeIn(@NonNull View view, long duration) {
        fadeIn(view, duration, null);
    }
    public static void fadeOut(@NonNull View view, long duration) {
        fadeOut(view, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                });
    }
}
