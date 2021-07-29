package org.exthmui.share.shared.base.listeners;

public interface OnReceiveShareBroadcastActionListener {
    void onReceiveActionAcceptationDialog(String pluginCode, String requestId, String peerName, String fileName, long fileSize, int notificationId);

    void onReceiveActionAcceptShare(String pluginCode, String requestId);
    void onReceiveActionRejectShare(String pluginCode, String requestId);
}
