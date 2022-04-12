package org.exthmui.share.shared;

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
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

public abstract class Utils {
    public static final String TAG = "Utils";

    public static @NonNull
    DocumentFile getDestinationDirectory(Context context) {
        String destinationDirectoryUri = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefs_key_global_destination_directory), context.getString(R.string.prefs_default_global_destination_directory));

        DocumentFile downloadsDirectory = DocumentFile.fromFile(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));

        if (destinationDirectoryUri == null) return downloadsDirectory;
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

    public static String getDeviceNameOnBoard(Context context) {
        String deviceName;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {// FIXME: 3/24/22
//            deviceName = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
//        } else
        deviceName = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
        return deviceName;
    }

    @NonNull
    public static String getSelfName(@NonNull Context context) {
        String defaultValue = getDeviceNameOnBoard(context);
        String deviceName = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefs_key_global_default_file_name), defaultValue);
        if (deviceName == null || TextUtils.isEmpty(deviceName)) {
            Log.e(TAG, String.format("Got invalid device name, returning default value \"%s\".", defaultValue));
            deviceName = defaultValue;
        }
        return deviceName;
    }

    @NonNull
    public static String getSelfId(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.prefs_key_global_default_file_name);
        String peerId = sharedPreferences.getString(key, null);
        if (peerId == null || TextUtils.isEmpty(peerId)) {
            peerId = genPeerId();
            sharedPreferences.edit().putString(key, peerId).apply();
            Log.e(TAG, String.format("Got invalid device name, regenerated and saved a new value: %s.", peerId));
        }
        return peerId;
    }

    @NonNull
    public static String genPeerId() {
        return UUID.randomUUID().toString();
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isDevelopmentModeEnabled(ContentResolver cr) {
        return (Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) > 0);
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
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        return include;
    }

    public Bitmap Drawable2Bitmap(Drawable img) {
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
