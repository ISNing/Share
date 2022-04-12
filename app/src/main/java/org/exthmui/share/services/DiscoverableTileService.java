package org.exthmui.share.services;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.exthmui.share.R;
import org.exthmui.share.shared.ServiceUtils;

public class DiscoverableTileService extends TileService {
    public static final String TAG = "DiscoverableTileService";
    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private ReceiveService mService;

    public DiscoverableTileService() {
        mConnection.registerOnServiceConnectedListener(service -> {
            mService = (ReceiveService) service;
            refreshState();
        });
        mConnection.registerOnServiceDisconnectedListener(name -> mService = null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(getApplicationContext(), ReceiveService.class), mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service going down(onDestroy), unbinding ReceiveService");
        if (mService != null) {
            mService.beforeUnbind();
        }
        unbindService(mConnection);
        mService = null;
        super.onDestroy();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshState();
    }

    @Override
    public void onClick() {
        super.onClick();
        refreshState();
        int state = getQsTile().getState();
        switch (state) {
            case Tile.STATE_ACTIVE:
                if (mService == null) {
                    mConnection.registerOnServiceConnectedListener(new ServiceUtils.OnServiceConnectedListener() {
                        @Override
                        public void onServiceConnected(ServiceUtils.MyService service) {
                            ((ReceiveService) service).stopReceivers();
                            mConnection.unregisterOnServiceConnectedListener(this);
                        }
                    });
                } else {
                    mService.stopReceivers();
                }
                break;
            case Tile.STATE_INACTIVE:
                if (mService == null) {
                    mConnection.registerOnServiceConnectedListener(new ServiceUtils.OnServiceConnectedListener() {
                        @Override
                        public void onServiceConnected(ServiceUtils.MyService service) {
                            ((ReceiveService) service).startReceivers();
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
}