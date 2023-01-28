package org.exthmui.utils;

import android.graphics.PorterDuff.Mode;

import androidx.annotation.NonNull;

public abstract class PorterDuffUtils {
    public static int modeToInt(@NonNull Mode val) {
        switch (val) {
            default:
            case CLEAR:
                return 0;
            case SRC:
                return 1;
            case DST:
                return 2;
            case SRC_OVER:
                return 3;
            case DST_OVER:
                return 4;
            case SRC_IN:
                return 5;
            case DST_IN:
                return 6;
            case SRC_OUT:
                return 7;
            case DST_OUT:
                return 8;
            case SRC_ATOP:
                return 9;
            case DST_ATOP:
                return 10;
            case XOR:
                return 11;
            case DARKEN:
                return 16;
            case LIGHTEN:
                return 17;
            case MULTIPLY:
                return 13;
            case SCREEN:
                return 14;
            case ADD:
                return 12;
            case OVERLAY: return 15;
        }
    }

    @NonNull
    public static Mode intToMode(int val) {
        switch (val) {
            default:
            case  0: return Mode.CLEAR;
            case  1: return Mode.SRC;
            case  2: return Mode.DST;
            case  3: return Mode.SRC_OVER;
            case  4: return Mode.DST_OVER;
            case  5: return Mode.SRC_IN;
            case  6: return Mode.DST_IN;
            case  7: return Mode.SRC_OUT;
            case  8: return Mode.DST_OUT;
            case  9: return Mode.SRC_ATOP;
            case 10: return Mode.DST_ATOP;
            case 11: return Mode.XOR;
            case 16: return Mode.DARKEN;
            case 17: return Mode.LIGHTEN;
            case 13: return Mode.MULTIPLY;
            case 14: return Mode.SCREEN;
            case 12: return Mode.ADD;
            case 15: return Mode.OVERLAY;
        }
    }
}
