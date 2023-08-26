package org.exthmui.share.shared.misc;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.base.FileInfo;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

public abstract class Utils {
    public static final String TAG = "Utils";

    @NonNull
    public static DocumentFile getDestinationDirectory(@NonNull Context context) {
        String destinationDirectoryUri = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefs_key_global_destination_directory), context.getString(R.string.prefs_default_global_destination_directory));

        DocumentFile downloadsDirectory = DocumentFile.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

        if (TextUtils.isEmpty(destinationDirectoryUri)) return downloadsDirectory;
        DocumentFile destinationDirectory = DocumentFile.fromTreeUri(context, Uri.parse(destinationDirectoryUri));
        if (destinationDirectory == null) {
            Log.e(TAG, "Failed to get DocumentFile object, returning Download directory.");
            return downloadsDirectory;
        }
        if (!destinationDirectory.isDirectory()) {
            Log.e(TAG, "Got non-directory destination directory path, returning Download directory.");
            return downloadsDirectory;
        }
        if (!destinationDirectory.canWrite()) {
            Log.e(TAG, "Got non-writeable destination directory path, returning Download directory.");
            return downloadsDirectory;
        }
        return destinationDirectory;
    }

    @NonNull
    public static String getDefaultFileName(@NonNull Context context) {
        String defaultValue = context.getString(R.string.prefs_default_global_default_file_name);
        String defaultFileName = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefs_key_global_default_file_name), defaultValue);
        if (defaultFileName == null || TextUtils.isEmpty(defaultFileName)) {
            Log.e(TAG, String.format("Got invalid default file name, returning default value \"%s\".", defaultValue));
            defaultFileName = defaultValue;
        }
        return defaultFileName;
    }

    public static String getDeviceNameOnBoard(@NonNull Context context) {
        String deviceName;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {// FIXME: 3/24/22
            deviceName = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
        } else
            deviceName = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
        return deviceName;
    }

    @NonNull
    public static String getSelfName(@NonNull Context context) {
        String defaultValue = getDeviceNameOnBoard(context);
        if (defaultValue == null) defaultValue = context.getString(R.string.device_name_default);
        String deviceName = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefs_key_global_device_name), defaultValue);
        if (deviceName == null || TextUtils.isEmpty(deviceName)) {
            Log.e(TAG, String.format("Got invalid device name, returning default value \"%s\".", defaultValue));
            deviceName = defaultValue;
        }
        if (deviceName.length() > Constants.DISPLAY_NAME_LENGTH) {
            Log.e(TAG, String.format("Device name is too long: %d, cutting down to %d.", deviceName.length(), Constants.DISPLAY_NAME_LENGTH));
            deviceName = deviceName.substring(0, Constants.DISPLAY_NAME_LENGTH);
        }
        return deviceName;
    }

    @NonNull
    public static String getSelfId(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.prefs_key_global_peer_id);
        String peerId = sharedPreferences.getString(key, null);
        if (peerId == null || TextUtils.isEmpty(peerId) || peerId.length() > Constants.PEER_ID_LENGTH) {
            peerId = genPeerId();
            sharedPreferences.edit().putString(key, peerId).apply();
            Log.e(TAG, String.format("Got invalid device id, regenerated and saved a new value: %s.", peerId));
        }
        return peerId;
    }

    public static boolean useSAF(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.prefs_key_global_use_saf);
        boolean useSAF = sharedPreferences.getBoolean(key, false);
        if (useSAF) Log.w(TAG, "Use SAF enabled, this may cause performance problem");
        return useSAF;
    }

    public static int getSelfDeviceType(@NonNull Context context) {
        return 0;//TODO: Waiting to implement
    }

    @NonNull
    public static String genPeerId() {
        return UUID.randomUUID().toString().substring(0, Constants.PEER_ID_LENGTH);
    }

    public static SharedPreferences getDefaultSharedPreferences(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isDevelopmentModeEnabled(ContentResolver cr) {
        return (Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) > 0);
    }


    @NonNull
    public static String genFileInfosStr(@NonNull Context context, @Nullable FileInfo[] fileInfos) {
        StringBuilder fileNameStr = null;
        if (fileInfos != null) {
            for (FileInfo fileInfo : fileInfos) {
                if (fileNameStr == null)
                    fileNameStr = new StringBuilder();
                else
                    fileNameStr.append("\n");

                String fileSizeStr = (fileInfo == null || fileInfo.getFileSize() == 0) ?
                        context.getString(R.string.notification_placeholder_unknown) :
                        Formatter.formatFileSize(context, fileInfo.getFileSize());
                fileNameStr.append(String.format("%s(%s)", (fileInfo == null || fileInfo.getFileName() == null) ?
                        context.getString(R.string.notification_placeholder_unknown) :
                        fileInfo.getFileName(), fileSizeStr));
            }
        } else
            fileNameStr = new StringBuilder(context.getString(R.string.notification_placeholder_unknown));
        if (fileNameStr == null)
            fileNameStr = new StringBuilder();
        return fileNameStr.toString();
    }

    @NonNull
    public static String genFileInfosStr(@NonNull Context context, @Nullable String[] fileNames, @Nullable long[] fileSizes) {
        StringBuilder fileNameStr = null;
        if (fileNames != null) {
            for (int i = 0; i < fileNames.length; i++) {
                if (fileNameStr == null)
                    fileNameStr = new StringBuilder();
                else
                    fileNameStr.append("\n");

                String fileSizeStr;
                if (fileSizes != null)
                    fileSizeStr = Formatter.formatFileSize(context, fileSizes[i]);
                else fileSizeStr = context.getString(R.string.notification_placeholder_unknown);
                fileNameStr.append(String.format("%s(%s)", fileNames[i] == null ?
                        context.getString(R.string.notification_placeholder_unknown) :
                        fileNames[i], fileSizeStr));
            }
        } else
            fileNameStr = new StringBuilder(context.getString(R.string.notification_placeholder_unknown));
        if (fileNameStr == null)
            fileNameStr = new StringBuilder();
        return fileNameStr.toString();
    }

    /**
     * Judge whether the value is included in enum clazz
     *
     * @param clazz Enum class
     * @param code  The value to be found
     * @return Whether the value is included in enum clazz
     */
    public static boolean isInclude(@NonNull Class<?> clazz, Integer code) {
        boolean include = false;
        if (clazz.isEnum()) {
            Object[] enumConstants = clazz.getEnumConstants();
            try {
                Method getCode = clazz.getMethod("getCode");
                for (Object enumConstant : Objects.requireNonNull(enumConstants)) {
                    if (Objects.requireNonNull(getCode.invoke(enumConstant)).equals(code)) {
                        include = true;
                        break;
                    }
                }
            } catch (@NonNull NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        return include;
    }

    public Bitmap Drawable2Bitmap(@NonNull Drawable img) {
        return ((BitmapDrawable) img).getBitmap();
    }

    public String Bitmap2StrByBase64(@NonNull Bitmap bm) {
        int compressQuality = Constants.COMPRESS_QUALITY;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, compressQuality, bos);
        byte[] bytes = bos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}
