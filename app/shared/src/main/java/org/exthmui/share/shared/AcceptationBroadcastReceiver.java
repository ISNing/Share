package org.exthmui.share.shared;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.listeners.OnReceiveShareBroadcastActionListener;

public class AcceptationBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPTATION_DIALOG = "org.exthmui.share.intent.action.ACCEPTATION_DIALOG";
    public static final String ACTION_ACCEPT = "org.exthmui.share.intent.action.ACCEPT_SHARE";
    public static final String ACTION_REJECT = "org.exthmui.share.intent.action.REJECT_SHARE";

    public static final String EXTRA_PLUGIN_CODE = "org.exthmui.share.extra.PLUGIN_CODE";
    public static final String EXTRA_REQUEST_ID = "org.exthmui.share.extra.REQUEST_ID";
    public static final String EXTRA_PEER_INFO_TRANSFER = "org.exthmui.share.extra.PEER_INFO_TRANSFER";
    public static final String EXTRA_FILE_INFOS = "org.exthmui.share.extra.FILE_INFOS";

    @Nullable
    private OnReceiveShareBroadcastActionListener mOnReceiveShareBroadcastActionListener;

    public void setListener(@Nullable OnReceiveShareBroadcastActionListener listener) {
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
        if (mOnReceiveShareBroadcastActionListener == null) return;
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        String pluginCode = intent.getStringExtra(EXTRA_PLUGIN_CODE);
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        switch (intent.getAction()) {
            case ACTION_ACCEPTATION_DIALOG:
                SenderInfo senderInfo = (SenderInfo) intent.getSerializableExtra(EXTRA_PEER_INFO_TRANSFER);
                FileInfo[] fileInfos = (FileInfo[]) intent.getSerializableExtra(EXTRA_FILE_INFOS);
                mOnReceiveShareBroadcastActionListener.onReceiveActionAcceptationDialog(pluginCode, requestId, senderInfo, fileInfos, notificationId);
                break;
            case ACTION_ACCEPT:
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId);
                }
                mOnReceiveShareBroadcastActionListener.onReceiveActionAcceptShare(pluginCode, requestId);
                break;
            case ACTION_REJECT:
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId);
                }
                mOnReceiveShareBroadcastActionListener.onReceiveActionRejectShare(pluginCode, requestId);
                break;
        }
    }
}
