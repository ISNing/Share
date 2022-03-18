package org.exthmui.share.shared;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Utils {
    private static final String TAG = "Utils";
    public static boolean isDevelopmentModeEnabled(ContentResolver cr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            return (Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) > 0);
        else return (Settings.Secure.getInt(cr, Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0) > 0);
    }

    public Bitmap Drawable2Bitmap(Drawable img){
        return ((BitmapDrawable) img).getBitmap();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public String Bitmap2StrByBase64(Bitmap bm){
        int compressQuality = Constants.COMPRESS_QUALITY;
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, compressQuality, bos);
        byte[] bytes=bos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * 判断数值是否属于枚举类的值
     * @param clzz 枚举类 Enum
     * @param code
     * @author wayleung
     * @return
     */
    public static boolean isInclude(Class<?> clzz,Integer code) {
        boolean include = false;
        if(clzz.isEnum()){
            Object[] enumConstants = clzz.getEnumConstants();
            try {
                Method getCode = clzz.getMethod("getCode");
                for (Object enumConstant:enumConstants){
                    if (getCode.invoke(enumConstant).equals(code)) {
                        include = true;
                        break;
                    }
                }
            }catch (NoSuchMethodException ignored){}
            catch (IllegalAccessException ignored){}
            catch (InvocationTargetException ignored) {}
        }

        return include;
    }

    public static @NonNull DocumentFile getDestinationDirectory(Context context) {
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

    public static @NonNull String getDefaultFileName(Context context) {
        String defaultValue = context.getString(R.string.prefs_default_global_default_file_name);
        String defaultFileName = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefs_key_global_default_file_name), defaultValue);
        if (defaultFileName == null | TextUtils.isEmpty(defaultFileName)) {
            Log.e(TAG, String.format("Got invalid default file name, returning default value \"%s\".", defaultValue));
            defaultFileName = defaultValue;
        }
        return defaultFileName;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
