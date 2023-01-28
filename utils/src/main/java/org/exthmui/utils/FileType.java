package org.exthmui.utils;


import androidx.annotation.Nullable;

public enum FileType {
    UNKNOWN(-1), APK(0), AUDIO(1), CERTIFICATE(2),
    CODE(3), COMPRESSED(4), CONTACT(5), EVENTS(6),
    FONT(7), IMAGE(8), PDF(9), PRESENTATION(10),
    SPREADSHEETS(11), DOCUMENTS(12), TEXT(13), VIDEO(14),
    ENCRYPTED(15), GIF(16);
    private final int numVal;

    FileType(int numVal) {
        this.numVal = numVal;
    }

    @Nullable
    public static FileType parse(int numVal) {
        for (FileType o : FileType.values()) {
            if (o.getNumVal() == numVal) {
                return o;
            }
        }
        return null;
    }

    public int getNumVal() {
        return numVal;
    }
}