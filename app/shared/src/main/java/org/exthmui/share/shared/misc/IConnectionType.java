package org.exthmui.share.shared.misc;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public interface IConnectionType {
    @NonNull
    String getFriendlyName();

    @NonNull
    String getCode();

    @NonNull
    Class<? extends Sender<? extends PeerInfo>> getSenderClass();

    @NonNull
    Class<? extends Discoverer> getDiscovererClass();

    @NonNull
    Class<? extends Receiver> getReceiverClass();

    @NonNull
    Class<? extends PeerInfo> getPeerClass();

    @NonNull
    Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass();
}
