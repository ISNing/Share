package org.exthmui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;

public abstract class CrossFadeUtils {
    public static ViewPropertyAnimator fadeIn(@NonNull View view, long duration, Animator.AnimatorListener listener) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        return view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(listener);
    }

    public static ViewPropertyAnimator fadeOut(@NonNull View view, long duration, Animator.AnimatorListener listener) {
        return view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(listener);
    }

    public static ViewPropertyAnimator fadeIn(@NonNull View view, long duration) {
        return fadeIn(view, duration, null);
    }
    public static ViewPropertyAnimator fadeOut(@NonNull View view, long duration) {
        return fadeOut(view, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                });
    }
}
