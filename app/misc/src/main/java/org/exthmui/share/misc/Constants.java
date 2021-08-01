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
        DESKTOP(1, R.drawable.ic_desktop),LAPTOP(2, R.drawable.ic_laptop),
        PHONE(3, R.drawable.ic_phone),TABLET(4, R.drawable.ic_tablet),
        UNKNOWN(0, R.drawable.ic_device_unknown);
        private final int numVal;
        private final int imgRes;

        DeviceTypes(int numVal, int imgRes) {
            this.numVal = numVal;
            this.imgRes = imgRes;
        }

        public int getNumVal() {
            return numVal;
        }
        public int getImgRes(){
            return imgRes;
        }
    }

    public enum ConnectionStatus{
        AVAILABLE(1),
        UNAVAILABLE(2),
        UNKNOWN(0);
        private final int numVal;

        ConnectionStatus(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }

    public enum TransmissionStatus{
        TRANSMITTING(1),
        FAILED(2),
        UNAVAILABLE(0);
        private final int numVal;

        TransmissionStatus(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }
}
