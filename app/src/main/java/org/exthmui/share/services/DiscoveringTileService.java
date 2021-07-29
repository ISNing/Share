package org.exthmui.share.services;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.exthmui.share.R;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.base.listeners.BaseEventListener;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

public class DiscoveringTileService extends TileService {
    private static final String TAG = "DiscoveringTileService";

    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    @Nullable private DiscoverService mService;

    public DiscoveringTileService() {
        mConnection.registerOnServiceConnectedListener(service -> {
            mService = (DiscoverService) service;
            refreshState();
        });
        mConnection.registerOnServiceDisconnectedListener(name -> mService = null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(getApplicationContext(), DiscoverService.class), mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onClick() {
        super.onClick();
        int state = getQsTile().getState();
        switch (state) {
            case Tile.STATE_ACTIVE:
                if (mService == null) {
                    mConnection.registerOnServiceConnectedListener(new ServiceUtils.OnServiceConnectedListener() {
                        @Override
                        public void onServiceConnected(ServiceUtils.MyService service) {
                            mService.stopDiscoverers();
                            mConnection.unregisterOnServiceConnectedListener(this);
                        }
                    });
                } else {
                    mService.stopDiscoverers();
                }
                break;
            case Tile.STATE_INACTIVE:
                if (mService == null) {
                    mConnection.registerOnServiceConnectedListener(new ServiceUtils.OnServiceConnectedListener() {
                        @Override
                        public void onServiceConnected(ServiceUtils.MyService service) {
                            mService.startDiscoverers();
                            mConnection.unregisterOnServiceConnectedListener(this);
                        }
                    });
                } else {
                    mService.startDiscoverers();
                }
                break;
            case Tile.STATE_UNAVAILABLE:
                Toast.makeText(getApplicationContext(), "Reason", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    private void refreshState() {
        if (mService == null) return;
        if (!mService.isDiscoverersAvailable()) {
            setState(Tile.STATE_UNAVAILABLE);
        } else if (mService.isAnyDiscovererStarted()) {
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
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_tile_discovering_disabled);
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.setLabel(getText(R.string.tile_scanning_unavailable));
                tile.setIcon(icon);
                break;
            case Tile.STATE_INACTIVE:
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_tile_discovering_disabled);
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getText(R.string.tile_scanning_disabled));
                tile.setIcon(icon);
                break;
            case Tile.STATE_ACTIVE:
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_tile_discovering_enabled);
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getText(R.string.tile_scanning_enabled));
                tile.setIcon(icon);
                break;
        }
        tile.updateTile();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service going down(onDestroy), unbinding DiscoverService");
        if (mService != null) {
            mService.beforeUnbind();
        }
        unbindService(mConnection);
        mService = null;
        super.onDestroy();
    }
}