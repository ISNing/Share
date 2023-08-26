package org.exthmui.share.shared.base.results;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.exthmui.share.shared.misc.Constants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public interface TransmissionResult {
    @Keep
    void initialize(Context context);

    @NonNull
    Constants.TransmissionStatus getStatus();

    default int getStatusCode() {
        return getStatus().getNumVal();
    }

    @NonNull
    String getMessage();

    @Nullable
    String getLocalizedMessage();

    Gson GSON = new Gson();

    static String mapToString(Map<String, TransmissionResult> resultMap) {
        Map<String, Pair<String, String>> basicMap = new HashMap<>();
        for (String key : resultMap.keySet()) {
            TransmissionResult result = resultMap.get(key);
            if (result == null) continue;
            basicMap.put(key, new Pair<>(result.getClass().getCanonicalName(), GSON.toJson(result)));
        }
        return GSON.toJson(basicMap);
    }

    static Map<String, TransmissionResult> stringToMap(Context context, String resultMapStr) throws IllegalArgumentException {
        Type type = new TypeToken<Map<String, Pair<String, String>>>() {
        }.getType();
        Map<String, Pair<String, String>> strMap = GSON.fromJson(resultMapStr, type);
        Map<String, TransmissionResult> resultMap = new HashMap<>(strMap.size());
        for (Map.Entry<String, Pair<String, String>> entry : strMap.entrySet()) {
            try {
                Class<?> clazz = Class.forName(entry.getValue().first);
                if (TransmissionResult.class.isAssignableFrom(clazz)) {
                    TransmissionResult result = (TransmissionResult) GSON.fromJson(entry.getValue().second, clazz);
                    Method initialize = TransmissionResult.class.getDeclaredMethod("initialize", Context.class);
                    initialize.invoke(result, context);
                    resultMap.put(entry.getKey(), result);
                } else throw new IllegalArgumentException();
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return resultMap;
    }
}
