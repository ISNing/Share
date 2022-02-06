package org.exthmui.share.services;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import org.exthmui.share.BuildConfig;
import org.exthmui.share.R;
import org.exthmui.share.misc.Constants;
import org.exthmui.share.shared.BaseEventListenersUtils;
import org.exthmui.share.shared.ReceiverUtils;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.ShareBroadcastReceiver;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnReceiveShareBroadcastActionListener;
import org.exthmui.share.shared.base.listeners.OnReceiverStartedListener;
import org.exthmui.share.shared.base.listeners.OnReceiverStoppedListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReceiveService extends ServiceUtils.MyService {

    public static final int REQUEST_CODE_GRANT_PERMISSIONS = 0;
    private static final String TAG = "ReceiveService";
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED;
    private static ReceiveService instance;

    static {
        LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
                {
                        OnReceiverStartedListener.class,
                        OnReceiverStoppedListener.class
                };
    }

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    private final Set<Receiver> mReceiverList = new HashSet<>();
    private final Set<BaseEventListener> mReceiverListenerList = new HashSet<>();
    private final Map<String, PeerInfo> mPeerInfoMap = new HashMap<>();

    private final ShareBroadcastReceiver mShareBroadcastReceiver = new ShareBroadcastReceiver();

    public ReceiveService() {
    }

    public static ReceiveService getInstance() {
        if (instance == null) instance = new ReceiveService();
        return instance;
    }

    public void registerListener(BaseEventListener listener) {
        if (BaseEventListenersUtils.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }

    public void unregisterListener(BaseEventListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mShareBroadcastReceiver.setListener(new OnReceiveShareBroadcastActionListener() {
            @Override
            public void onReceiveActionAcceptationDialog(String pluginCode, String requestId, String peerName, String fileName, long fileSize) {
                ReceiverUtils.buildRequestDialog(ReceiveService.this, pluginCode, requestId, peerName, fileName, fileSize);
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
        registerReceiver(mShareBroadcastReceiver, ShareBroadcastReceiver.getIntentFilter());

        addReceivers();
        registerInternalListeners();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mShareBroadcastReceiver);
        unregisterReceiverListeners(mReceiverListenerList);
        stopReceivers();
    }

    public void unregisterReceiverListeners(Collection<BaseEventListener> listeners) {
        for (Receiver receiver : mReceiverList) {
            for (BaseEventListener listener : listeners) {
                receiver.unregisterListener(listener);
            }
        }
        registerInternalListeners();
    }

    @Nullable
    private Receiver getReceiver(String code) {
        Constants.ConnectionType connectionType = Constants.ConnectionType.parseFromCode(code);
        if (connectionType == null) return null;
        for (Receiver receiver : mReceiverList) {
            if (!receiver.getClass().isAssignableFrom(connectionType.getReceiverClass()))
                continue;
            return receiver;
        }
        return null;
    }

    public void stopReceivers() {
        for (Receiver receiver : mReceiverList) {
            if (receiver.isReceiverStarted())
                receiver.stopReceive();
        }
    }

    public void addReceivers() {
        Set<String> codes = PreferenceManager.getDefaultSharedPreferences(this).getStringSet(getString(R.string.prefs_key_global_plugins_enabled), Collections.emptySet());
        for (String code : codes) {
            addReceiver(code);
        }
    }

    private void registerInternalListeners() {
        Set<BaseEventListener> listeners = new HashSet<>();
        listeners.add((OnReceiverStartedListener) event -> {
            TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoverableTileService.class.getName()));
            notifyListeners(event);
        });
        listeners.add((OnReceiverStoppedListener) event -> {
            TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoverableTileService.class.getName()));
            if (isAllReceiversStopped()) notifyListeners(event);
        });
        registerReceiverListeners(listeners);
    }

    public void addReceiver(String code) {
        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
        if (type == null) return;
        try {
            Method method = type.getReceiverClass().getDeclaredMethod("getInstance", Context.class);
            Receiver receiver = (Receiver) method.invoke(null, this);
            mReceiverList.add(receiver);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    private void notifyListeners(EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    public boolean isAllReceiversStopped() {
        return !isAnyReceiverStarted();
    }

    public void registerReceiverListeners(Collection<? extends BaseEventListener> listeners) {
        for (Receiver receiver : mReceiverList) {
            for (BaseEventListener listener : listeners) {
                receiver.registerListener(listener);
            }
        }
    }

    public boolean isAnyReceiverStarted() {
        for (Receiver receiver : mReceiverList) {
            if (receiver.isReceiverStarted()) return true;
        }
        return false;
    }

    public void removeReceiver(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        if (receiver.isReceiverStarted()) {
            receiver.registerListener((OnReceiverStoppedListener) event -> mReceiverList.remove(receiver));
            receiver.stopReceive();
        } else {
            mReceiverList.remove(receiver);
        }
    }

    public void startReceiver(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        receiver.startReceive();

    }

    public void stopReceiver(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        receiver.stopReceive();

    }

    public void restartReceiver() {
        stopReceivers();
        startReceivers();
    }

    public void startReceivers() {
        for (Receiver receiver : mReceiverList) {
            if (!receiver.isInitialized()) receiver.initialize();
            receiver.startReceive();
        }
    }

    public void registerReceiverListeners(Collection<? extends BaseEventListener> listeners, String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        for (BaseEventListener listener : listeners) {
            receiver.registerListener(listener);
        }
    }

    public void unregisterReceiverListeners(Collection<? extends BaseEventListener> listeners, String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return;
        for (BaseEventListener listener : listeners) {
            receiver.unregisterListener(listener);
        }

    }

    public boolean isAnyReceiverStopped() {
        return !isAllReceiversStarted();
    }

    public boolean isAllReceiversStarted() {
        for (Receiver receiver : mReceiverList) {
            if (!receiver.isReceiverStarted()) return false;
        }
        return true;
    }

    public boolean isReceiverStopped(String code) {
        return !isReceiverStarted(code);
    }

    public boolean isReceiverStarted(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return false;
        return receiver.isReceiverStarted();
    }

    public boolean isReceiversAvailable() {
        for (Receiver receiver : mReceiverList) {
            if (receiver.isFeatureAvailable() & receiver.getPermissionNotGranted().isEmpty())
                return true;
        }
        return false;
    }

    public boolean isReceiverAvailable(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return false;
        return receiver.isFeatureAvailable() & receiver.getPermissionNotGranted().isEmpty();
    }

    public void grantReceiversPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, getReceiversPermissionsNotGranted().toArray(new String[0]), REQUEST_CODE_GRANT_PERMISSIONS);
    }

    public Set<String> getReceiversPermissionsNotGranted() {
        Set<String> permissions = new HashSet<>();
        for (Receiver receiver : mReceiverList) {
            permissions.addAll(receiver.getPermissionNotGranted());
        }
        return permissions;
    }

    public void grantReceiverPermissions(String code, Activity activity, @IntRange(from = 0) int requestCode) {
        ActivityCompat.requestPermissions(activity, getReceiverPermissionsNotGranted(code).toArray(new String[0]), requestCode);
    }

    public Set<String> getReceiverPermissionsNotGranted(String code) {
        Receiver receiver = getReceiver(code);
        if (receiver == null) return Collections.emptySet();
        return receiver.getPermissionNotGranted();
    }
}