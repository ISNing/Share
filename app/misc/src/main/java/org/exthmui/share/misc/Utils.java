package org.exthmui.share.misc;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Utils {
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
}
