package org.exthmui.share.shared.listeners;

import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;

public interface OnReceiveShareBroadcastActionListener {
    void onReceiveActionAcceptationDialog(String pluginCode, String requestId, SenderInfo senderInfo, FileInfo[] fileInfos, int notificationId);

    void onReceiveActionAcceptShare(String pluginCode, String requestId);
    void onReceiveActionRejectShare(String pluginCode, String requestId);
}
