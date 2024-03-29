package org.exthmui.share.services;

import android.app.Activity;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import org.exthmui.share.BuildConfig;
import org.exthmui.share.R;
import org.exthmui.share.misc.Constants;
import org.exthmui.share.shared.AcceptationBroadcastReceiver;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.events.ReceiverStoppedEvent;
import org.exthmui.share.shared.listeners.OnReceiveShareBroadcastActionListener;
import org.exthmui.share.shared.listeners.OnReceiverErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnReceiverStartedListener;
import org.exthmui.share.shared.listeners.OnReceiverStoppedListener;
import org.exthmui.share.shared.misc.ReceiverUtils;
import org.exthmui.utils.BaseEventListenersUtils;
import org.exthmui.utils.ServiceUtils;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

public class ReceiveService extends ServiceUtils.MyService implements org.exthmui.share.shared.services.IReceiveService {

    public static final String TAG = "ReceiveService";
    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnReceiverStartedListener.class,
                    OnReceiverStoppedListener.class
            };

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    @IntRange(from = 0)
    private int mBindNumber = 0;

    private final Set<Receiver> mReceiverList = new HashSet<>();
    private final Set<BaseEventListener> mInternalListenerList = new HashSet<>();
    private final Set<BaseEventListener> mReceiverListenerList = new HashSet<>();

    private final AcceptationBroadcastReceiver mAcceptationBroadcastReceiver = new AcceptationBroadcastReceiver();

    public ReceiveService() {
        initializeInternalListeners();
    }

    @Override
    public void registerListener(@NonNull BaseEventListener listener) {
        if (BaseEventListenersUtils.INSTANCE.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }

    @Override
    public void unregisterListener(BaseEventListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAcceptationBroadcastReceiver.setListener(new OnReceiveShareBroadcastActionListener() {
            @Override
            public void onReceiveActionAcceptationDialog(String pluginCode, String requestId, SenderInfo senderInfo, FileInfo[] fileInfos, int notificationId) {
                // Due to changes of API 31, we must start activity directly
            }

            @Override
            public void onReceiveActionAcceptShare(String pluginCode, String requestId) {
                Receiver receiver = getReceiver(pluginCode);
                if (receiver == null) return;
                receiver.onReceiveActionAccept(new ReceiveActionAcceptEvent(this, pluginCode, requestId));
            }

            @Override
            public void onReceiveActionRejectShare(String pluginCode, String requestId) {
                Receiver receiver = getReceiver(pluginCode);
                if (receiver == null) return;
                receiver.onReceiveActionReject(new ReceiveActionRejectEvent(this, pluginCode, requestId));
            }
        });
        registerReceiver(mAcceptationBroadcastReceiver, AcceptationBroadcastReceiver.Companion.getIntentFilter());

        addReceivers();
        registerInternalListeners();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mAcceptationBroadcastReceiver.setListener(null);// Avoid memory leaking
        unregisterReceiver(mAcceptationBroadcastReceiver);
        unregisterReceiverListeners(mReceiverListenerList);
        Log.d(TAG, "Service going down(onDestroy), stopping receivers");
        stopReceivers();
        super.onDestroy();
    }

    private void initializeInternalListeners() {
        mInternalListenerList.add((OnReceiverStartedListener) event -> {
            updateTileState();
            notifyListeners(event);
        });
        mInternalListenerList.add((OnReceiverStoppedListener) event -> {
            updateTileState();
            if (isAllReceiversStopped()) {
                notifyListeners(new ReceiverStoppedEvent(this));
                stopForeground();
            }
        });
        mInternalListenerList.add((OnReceiverErrorOccurredListener) event -> {
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_receive_error_occurred_with_message,
                            event.getMessageLocalized()), duration);
            toast.show();
        });
    }

    @Override
    public void unregisterReceiverListeners(@NonNull Collection<BaseEventListener> listeners) {
        for (Receiver receiver : mReceiverList) {
            for (BaseEventListener listener : listeners) {
                receiver.unregisterListener(listener);
            }
        }
        registerInternalListeners();
    }

    @Override
    @Nullable
    public Receiver getReceiver(String code) {
        Constants.ConnectionType connectionType = Constants.ConnectionType.parseFromCode(code);
        if (connectionType == null) return null;
        for (Receiver receiver : mReceiverList) {
            if (!receiver.getClass().isAssignableFrom(connectionType.getReceiverClass()))
                continue;
            return receiver;
        }
        return null;
    }

    @Override
    public void stopReceivers() {
        for (Receiver receiver : mReceiverList) {
            if (!receiver.isInitialized()) receiver.initialize();
            if (!receiver.isInitialized()) Log.e(TAG, String.format("Failed initializing Discoverer %s", receiver.getConnectionType().getCode()));
            if (receiver.isReceiverStarted()) {
                Log.e(TAG, String.format("Discoverer %s has already been stopped", receiver.getConnectionType().getCode()));
                return;
            }
            receiver.stopReceive();
        }
    }

    @Override
    public void addReceivers() {
        Set<String> codes = PreferenceManager.getDefaultSharedPreferences(this).getStringSet(getString(R.string.prefs_key_global_plugins_enabled), Collections.emptySet());
        for (String code : codes) {
            addReceiver(code);
        }
    }

    @Override
    public void registerInternalListeners(@NonNull Receiver receiver) {
        for (BaseEventListener listener: mInternalListenerList) {
            receiver.registerListener(listener);
        }
    }

    @Override
    public void registerInternalListeners() {
        for (Receiver receiver: mReceiverList) {
            registerInternalListeners(receiver);
        }
    }

    @Override
    public void addReceiver(String code) {
        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
        if (type == null) return;
        try {
            Method method = type.getReceiverClass().getDeclaredMethod("getInstance", Context.class);
            Receiver receiver = (Receiver) method.invoke(null, this);
            assert receiver != null;
            mReceiverList.add(receiver);
            registerInternalListeners(receiver);
            updateTileState();
        } catch (@NonNull IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.INSTANCE.notifyListeners(event, mListeners);
    }

    @Override
    public boolean isAllReceiversStopped() {
        return !isAnyReceiverStarted();
    }

    @Override
    public void registerReceiverListeners(@NonNull Collection<? extends BaseEventListener> listeners) {
        for (Receiver receiver : mReceiverList) {
            for (BaseEventListener listener : listeners) {
                receiver.registerListener(listener);
            }
        }
    }

    @Override
    public boolean isAnyReceiverStarted() {
        for (Receiver receiver : mReceiverList) {
            if (receiver.isReceiverStarted()) return true;
        }
        return false;
    }

    @Override
    public void removeReceiver(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        if (receiver.isReceiverStarted()) {
            receiver.registerListener((OnReceiverStoppedListener) event -> {
                mReceiverList.remove(receiver);
                updateTileState();
            });
            receiver.stopReceive();
        } else {
            mReceiverList.remove(receiver);
            updateTileState();
        }
    }

    @Override
    public void startReceiver(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        if (!receiver.isInitialized()) receiver.initialize();
        if (!receiver.isInitialized()) Log.e(TAG, String.format("Failed initializing Discoverer %s", code));
        if (receiver.isReceiverStarted()) {
            Log.e(TAG, String.format("Discoverer %s has already been started", code));
            return;
        }
        receiver.startReceive();
    }

    @Override
    public void stopReceiver(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        if (!receiver.isInitialized()) receiver.initialize();
        if (!receiver.isInitialized()) Log.e(TAG, String.format("Failed initializing Discoverer %s", code));
        if (receiver.isReceiverStarted()) {
            Log.e(TAG, String.format("Discoverer %s has already been stopped", code));
            return;
        }
        receiver.stopReceive();
    }

    @Override
    public void restartReceiver() {
        stopReceivers();
        startReceivers();
    }

    @Override
    public void startReceivers() {
        for (Receiver receiver : mReceiverList) {
            if (!receiver.isInitialized()) receiver.initialize();
            if (!receiver.isInitialized()) Log.e(TAG, String.format("Failed initializing Discoverer %s", receiver.getConnectionType().getCode()));
            if (receiver.isReceiverStarted()) {
                Log.e(TAG, String.format("Discoverer %s has already been started", receiver.getConnectionType().getCode()));
                return;
            }
            receiver.startReceive();
        }
    }

    @Override
    public void registerReceiverListeners(@NonNull Collection<? extends BaseEventListener> listeners, String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        for (BaseEventListener listener : listeners) {
            receiver.registerListener(listener);
        }
    }

    @Override
    public void unregisterReceiverListeners(@NonNull Collection<? extends BaseEventListener> listeners, String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        for (BaseEventListener listener : listeners) {
            receiver.unregisterListener(listener);
        }

    }

    @Override
    public boolean isAnyReceiverStopped() {
        return !isAllReceiversStarted();
    }

    @Override
    public boolean isAllReceiversStarted() {
        for (Receiver receiver : mReceiverList) {
            if (!receiver.isReceiverStarted()) return false;
        }
        return true;
    }

    @Override
    public boolean isReceiverStopped(String code) {
        return !isReceiverStarted(code);
    }

    @Override
    public boolean isReceiverStarted(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return false;
        return receiver.isReceiverStarted();
    }

    @Override
    public boolean isReceiversAvailable() {
        for (Receiver receiver : mReceiverList) {
            if (receiver.isFeatureAvailable() && receiver.getPermissionNotGranted().isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public boolean isReceiverAvailable(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return false;
        return receiver.isFeatureAvailable() && receiver.getPermissionNotGranted().isEmpty();
    }

    @Override
    public void grantReceiversPermissions(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(activity, getReceiversPermissionsNotGranted().toArray(new String[0]), REQUEST_CODE_GRANT_PERMISSIONS);
    }

    @NonNull
    @Override
    public Set<String> getReceiversPermissionsNotGranted() {
        Set<String> permissions = new HashSet<>();
        for (Receiver receiver : mReceiverList) {
            permissions.addAll(receiver.getPermissionNotGranted());
        }
        return permissions;
    }

    @Override
    public void grantReceiverPermissions(String code, @NonNull Activity activity, @IntRange(from = 0) int requestCode) {
        ActivityCompat.requestPermissions(activity, getReceiverPermissionsNotGranted(code).toArray(new String[0]), requestCode);
    }

    @NonNull
    @Override
    public Set<String> getReceiverPermissionsNotGranted(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return Collections.emptySet();
        return receiver.getPermissionNotGranted();
    }

    private void updateTileState() {
        TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoverableTileService.class.getName()));
    }


    private void startForeground() {
        Notification notification = ReceiverUtils.buildServiceNotification(this);
        startForeground(this.hashCode(), notification);
    }

    private void stopForeground() {
        stopForeground(true);
    }

    @Override
    public void onBind(Intent intent, Object ignored) {
        Log.d(TAG, "Service going to be bound(onBind)");
        mBindNumber++;
        stopForeground();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Service going to be rebound(onRebind)");
        super.onRebind(intent);
        mBindNumber++;
        stopForeground();
    }

    /**
     * IMPORTANT: To keep service alive when receiver running, MUST be called before unbind service.
     */
    public void beforeUnbind() {
        if (mBindNumber > 1) return;
        Log.d(TAG, "Checking whether to start service and in foreground or in background(beforeUnbind)");
        if (isAnyReceiverStarted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, String.format("Receiver running, API newer than %d starting service in foreground", Build.VERSION_CODES.O));
                startForegroundService(new Intent(getApplicationContext(), ReceiveService.class));
                startForeground();
            } else {
                Log.d(TAG, String.format("Receiver running, API lower than %d, starting service in background", Build.VERSION_CODES.O));
                startService(new Intent(getApplicationContext(), ReceiveService.class));
            }
        }
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service going to be unbound(onUnbind)");
        mBindNumber--;
        return true;
    }
}