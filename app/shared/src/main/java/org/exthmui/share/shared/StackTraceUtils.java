package org.exthmui.share.shared;

public class StackTraceUtils {
    public static String getStackTraceString(StackTraceElement[] stackTraceElements) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : stackTraceElements) {
            builder.append(element.toString()).append("\n");
        }
        return builder.toString();
    }
}
