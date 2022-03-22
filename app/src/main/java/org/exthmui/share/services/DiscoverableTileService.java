package org.exthmui.share.services;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.exthmui.share.R;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.base.listeners.BaseEventListener;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

public class DiscoverableTileService extends TileService {
    private static final String TAG = "DiscoverableTileService";
    private final Set<BaseEventListener> mListenersReceiveService = new HashSet<>();
    private final MyServiceConnection mConnection = new MyServiceConnection();
    @Nullable
    private ReceiveService mService;

    public DiscoverableTileService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.unregisterReceiverListeners(mListenersReceiveService);
            unbindService(mConnection);
            mService = null;
        }
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        bindService(new Intent(getApplicationContext(), ReceiveService.class), mConnection, BIND_AUTO_CREATE);
        refreshState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if (mService != null) {
            mService.unregisterReceiverListeners(mListenersReceiveService);
            unbindService(mConnection);
            mService = null;
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        int state = getQsTile().getState();
        switch (state) {
            case Tile.STATE_ACTIVE:
                if (mService == null) {
                    mConnection.registerOnServiceConnectedListener(new OnServiceConnectedListener() {
                        @Override
                        public void onServiceConnected() {
                            mService.stopReceivers();
                            mService.stopForeground();
                            mConnection.unregisterOnServiceConnectedListener(this);
                        }
                    });
                } else {
                    mService.stopReceivers();
                    mService.stopForeground();
                }
                break;
            case Tile.STATE_INACTIVE:
                if (mService == null) {
                    mConnection.registerOnServiceConnectedListener(new OnServiceConnectedListener() {
                        @Override
                        public void onServiceConnected() {
                            mService.startReceivers();
                            mConnection.unregisterOnServiceConnectedListener(this);
                        }
                    });
                } else {
                    mService.startReceivers();
                }
                break;
            case Tile.STATE_UNAVAILABLE:
                Toast.makeText(getApplicationContext(), "Reason", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void refreshState() {
        if (mService == null) return;
        if (!mService.isReceiversAvailable()) {
            setState(Tile.STATE_UNAVAILABLE);
        } else if (mService.isAnyReceiverStarted()) {
            setState(Tile.STATE_ACTIVE);
        } else {
            setState(Tile.STATE_INACTIVE);
        }
    }

    private void setState(int state) {
        Tile tile = getQsTile();
        if (tile == null) return;
        Icon icon;
        switch (state) {
            case Tile.STATE_UNAVAILABLE:
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_tile_discoverable_disabled);
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.setLabel(getText(R.string.tile_scanning_unavailable));
                tile.setIcon(icon);
                break;
            case Tile.STATE_INACTIVE:
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_tile_discoverable_disabled);
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getText(R.string.tile_discoverable_disabled));
                tile.setIcon(icon);
                break;
            case Tile.STATE_ACTIVE:
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_tile_discoverable_enabled);
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getText(R.string.tile_discoverable_enabled));
                tile.setIcon(icon);
                break;
        }
        tile.updateTile();
    }

    interface OnServiceConnectedListener extends EventListener {
        void onServiceConnected();
    }

    class MyServiceConnection implements ServiceConnection {
        private final Set<OnServiceConnectedListener> mOnServiceConnectedListeners = new HashSet<>();

        private void registerOnServiceConnectedListener(OnServiceConnectedListener listener) {
            mOnServiceConnectedListeners.add(listener);
        }

        private void unregisterOnServiceConnectedListener(OnServiceConnectedListener listener) {
            mOnServiceConnectedListeners.remove(listener);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceUtils.MyService.MyBinder binder = (ServiceUtils.MyService.MyBinder) service;
            mService = (ReceiveService) binder.getService();
            for (OnServiceConnectedListener l :
                    mOnServiceConnectedListeners) {
                l.onServiceConnected();
            }
            refreshState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            assert mService != null;
            mService.unregisterReceiverListeners(mListenersReceiveService);
            mService = null;
        }
    }
}