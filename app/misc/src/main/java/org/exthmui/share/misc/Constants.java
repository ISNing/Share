package org.exthmui.share.misc;

public class Constants {
    public static final int FILE_TYPE_OTHERS=0;
    public static final int FILE_TYPE_APPLICATION=1;
    public static final int FILE_TYPE_DOCUMENT=2;
    public static final int FILE_TYPE_AUDIO=3;
    public static final int FILE_TYPE_VIDEO=4;
    public static final int FILE_TYPE_COMPRESSED=5;

    public static int COMPRESS_QUALITY=40;

    public enum DeviceTypes{
        COMPUTER(1),
        PHONE(2),TABLET(3),
        UNKNOWN(0);
        private final int numVal;

        DeviceTypes(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }

    public enum TransmissionStatus{
        TRANSMITTNG(1),
        FAILED(2),
        UNAVALIBLE(0);
        private final int numVal;

        TransmissionStatus(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }
}
