package org.exthmui.share.misc;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.exthmui.share.exceptions.FailedInvokingSendingMethodException;
import org.exthmui.share.exceptions.FailedStartSendingException;
import org.exthmui.share.exceptions.InvalidConnectionTypeException;
import org.exthmui.share.exceptions.InvalidSenderException;
import org.exthmui.share.shared.StackTraceUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Sender;

public class SendingHelper {

    private final Context mContext;

    public static final String TAG = "SendingHelper";

    public SendingHelper(@NonNull Context context) {
        this.mContext = context;
    }

    public UUID send(@NonNull PeerInfo target, @NonNull Entity entity)
        throws FailedStartSendingException {
        Constants.ConnectionType connectionType = Constants.ConnectionType
            .parseFromCode(target.getConnectionType());
        if (connectionType == null) {
            throw new InvalidConnectionTypeException("ConnectionType is null");
        }
        try {
            Method method = connectionType.getSenderClass()
                .getDeclaredMethod("getInstance", Context.class);
            Sender<?> sender = (Sender<?>) method.invoke(null, mContext);
            if (sender == null) {
                throw new InvalidSenderException("Sender is null");
            }
            return sender.sendToPeerInfo(target, entity);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            throw new FailedInvokingSendingMethodException(e);
        }
    }

    public UUID send(@NonNull PeerInfo target, @NonNull List<Entity> entities)
        throws FailedStartSendingException {
        Constants.ConnectionType connectionType = Constants.ConnectionType
            .parseFromCode(target.getConnectionType());
        if (connectionType == null) {
            throw new InvalidConnectionTypeException("ConnectionType is null");
        }
        try {
            Method method = connectionType.getSenderClass()
                .getDeclaredMethod("getInstance", Context.class);
            Sender<?> sender = (Sender<?>) method.invoke(null, mContext);
            if (sender == null) {
                throw new InvalidSenderException("Sender is null");
            }
            return sender.sendToPeerInfo(target, entities);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            throw new FailedInvokingSendingMethodException(e);
        }
    }
}