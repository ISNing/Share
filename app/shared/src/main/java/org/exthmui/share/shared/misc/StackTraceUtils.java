package org.exthmui.share.shared.misc;

import androidx.annotation.NonNull;

public abstract class StackTraceUtils {
    @NonNull
    public static String getStackTraceString(@NonNull StackTraceElement[] stackTraceElements) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : stackTraceElements) {
            builder.append(element.toString()).append("\n");
        }
        return builder.toString();
    }
}
