package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.services.ReceiveService;
import org.exthmui.share.shared.IShareBroadcastReceiver;
import org.exthmui.utils.ServiceUtils;

public class ShareBroadcastReceiver extends BroadcastReceiver implements IShareBroadcastReceiver {

    @Nullable
    private DiscoverService mDiscoverService;
    private final ServiceUtils.MyServiceConnection mDiscoverConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private ReceiveService mReceiveService;
    private final ServiceUtils.MyServiceConnection mReceiveConnection = new ServiceUtils.MyServiceConnection();

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        Context serviceContext = context.getApplicationContext();
        mDiscoverConnection.registerOnServiceConnectedListener(service -> mDiscoverService = (DiscoverService) service);
        mDiscoverConnection.registerOnServiceDisconnectedListener(name -> mDiscoverService = null);
        mReceiveConnection.registerOnServiceConnectedListener(service -> mReceiveService = (ReceiveService) service);
        mReceiveConnection.registerOnServiceDisconnectedListener(name -> mReceiveService = null);
        serviceContext.bindService(new Intent(serviceContext, DiscoverService.class), mDiscoverConnection, BIND_AUTO_CREATE);
        serviceContext.bindService(new Intent(serviceContext, ReceiveService.class), mReceiveConnection, BIND_AUTO_CREATE);

        String pluginCode = intent.getStringExtra(EXTRA_PLUGIN_CODE);

        switch (intent.getAction()) {
            case ACTION_ENABLE_DISCOVER:
                if (mDiscoverService == null)
                    mDiscoverConnection.registerOnServiceConnectedListener(service -> ((DiscoverService) service).startDiscoverers());
                else mDiscoverService.startDiscoverers();
                break;
            case ACTION_DISABLE_DISCOVER:
                if (mDiscoverService == null)
                    mDiscoverConnection.registerOnServiceConnectedListener(service -> ((DiscoverService) service).stopDiscoverers());
                else mDiscoverService.stopDiscoverers();
                break;
            case ACTION_ENABLE_RECEIVER:
                if (mReceiveService == null)
                    mReceiveConnection.registerOnServiceConnectedListener(service -> ((ReceiveService) service).startReceivers());
                else mReceiveService.startReceivers();
                break;
            case ACTION_DISABLE_RECEIVER:
                if (mReceiveService == null)
                    mReceiveConnection.registerOnServiceConnectedListener(service -> ((ReceiveService) service).stopReceivers());
                else mReceiveService.stopReceivers();
                break;

            case ACTION_START_DISCOVER:
                if (mDiscoverService == null)
                    mDiscoverConnection.registerOnServiceConnectedListener(service -> ((DiscoverService) service).startDiscoverer(pluginCode));
                else mDiscoverService.startDiscoverer(pluginCode);
                break;
            case ACTION_STOP_DISCOVER:
                if (mDiscoverService == null)
                    mDiscoverConnection.registerOnServiceConnectedListener(service -> ((DiscoverService) service).stopDiscoverer(pluginCode));
                else mDiscoverService.stopDiscoverer(pluginCode);
                break;
            case ACTION_START_RECEIVER:
                if (mReceiveService == null)
                    mReceiveConnection.registerOnServiceConnectedListener(service -> ((ReceiveService) service).startReceiver(pluginCode));
                else mReceiveService.startReceiver(pluginCode);
                break;
            case ACTION_STOP_RECEIVER:
                if (mReceiveService == null)
                    mReceiveConnection.registerOnServiceConnectedListener(service -> ((ReceiveService) service).stopReceiver(pluginCode));
                else mReceiveService.stopReceiver(pluginCode);
                break;
        }
        if (mDiscoverService == null)
            mDiscoverConnection.registerOnServiceConnectedListener(service -> {
                ((DiscoverService) service).beforeUnbind();
                serviceContext.unbindService(mDiscoverConnection);
            });
        else {
            mDiscoverService.beforeUnbind();
            serviceContext.unbindService(mDiscoverConnection);
            mDiscoverService = null;
        }
        if (mReceiveService == null)
            mReceiveConnection.registerOnServiceConnectedListener(service -> {
                ((ReceiveService) service).beforeUnbind();
                serviceContext.unbindService(mReceiveConnection);
            });
        else {
            mReceiveService.beforeUnbind();
            serviceContext.unbindService(mReceiveConnection);
            mReceiveService = null;
        }
    }
}
