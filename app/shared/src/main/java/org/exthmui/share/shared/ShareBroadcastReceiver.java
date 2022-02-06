package org.exthmui.share.shared;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.exthmui.share.shared.base.listeners.OnReceiveShareBroadcastActionListener;

public class ShareBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPTATION_DIALOG = "org.exthmui.share.intent.action.ACCEPTATION_DIALOG";
    public static final String ACTION_ACCEPT = "org.exthmui.share.intent.action.ACCEPT_SHARE";
    public static final String ACTION_REJECT = "org.exthmui.share.intent.action.REJECT_SHARE";
    public static final String EXTRA_PLUGIN_CODE = "org.exthmui.share.extra.PLUGIN_CODE";
    public static final String EXTRA_REQUEST_ID = "org.exthmui.share.extra.REQUEST_ID";
    public static final String EXTRA_PEER_NAME = "org.exthmui.share.extra.PEER_NAME";
    public static final String EXTRA_FILE_NAME = "org.exthmui.share.extra.FILE_NAME";
    public static final String EXTRA_FILE_SIZE = "org.exthmui.share.extra.FILE_SIZE";

    private OnReceiveShareBroadcastActionListener mOnReceiveShareBroadcastActionListener;

    public void setListener(OnReceiveShareBroadcastActionListener listener) {
        mOnReceiveShareBroadcastActionListener = listener;
    }


    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ACCEPTATION_DIALOG);
        intentFilter.addAction(ACTION_ACCEPT);
        intentFilter.addAction(ACTION_REJECT);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String pluginCode = intent.getStringExtra(EXTRA_PLUGIN_CODE);
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        switch (intent.getAction()) {
            case ACTION_ACCEPTATION_DIALOG:
                String peerName = intent.getStringExtra(EXTRA_PEER_NAME);
                String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
                long fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, -1);
                mOnReceiveShareBroadcastActionListener.onReceiveActionAcceptationDialog(pluginCode, requestId, peerName, fileName, fileSize);
                break;
            case ACTION_ACCEPT:
                mOnReceiveShareBroadcastActionListener.onReceiveActionAcceptShare(pluginCode, requestId);
                break;
            case ACTION_REJECT:
                mOnReceiveShareBroadcastActionListener.onReceiveActionRejectShare(pluginCode, requestId);
                break;
        }

    }
}
