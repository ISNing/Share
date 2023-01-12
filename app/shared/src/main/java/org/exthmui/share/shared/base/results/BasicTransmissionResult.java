package org.exthmui.share.shared.base.results;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.exthmui.share.shared.misc.Constants;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class BasicTransmissionResult extends AbstractTransmissionResult {

    private static final Gson GSON = new Gson();

    private final Constants.TransmissionStatus status;

    public BasicTransmissionResult(@NonNull TransmissionResult result) {
        this(result.getStatus(), result.getMessage(), result.getLocalizedMessage());
    }

    public BasicTransmissionResult(@NonNull Constants.TransmissionStatus status,
                                   @Nullable String message) {
        super(message);
        this.status = status;
    }

    public BasicTransmissionResult(@NonNull Constants.TransmissionStatus status,
                                   @Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
        this.status = status;
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return status;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Unknown Status";
    }

    public static String mapToString(Map<String, TransmissionResult> resultMap) {
        Map<String, BasicTransmissionResult> basicMap = new HashMap<>();
        for (String key : resultMap.keySet()) {
            TransmissionResult result = resultMap.get(key);
            if (result == null) continue;
            if (result instanceof BasicTransmissionResult)
                basicMap.put(key, (BasicTransmissionResult) result);
            else basicMap.put(key, new BasicTransmissionResult(result));
        }
        return GSON.toJson(basicMap);
    }

    public static Map<String, BasicTransmissionResult> stringToMap(String resultMapStr) {
        Type type = new TypeToken<Map<String, BasicTransmissionResult>>() {
        }.getType();
        return GSON.fromJson(resultMapStr, type);
    }
}
