package org.exthmui.share.shared.misc;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public interface IConnectionType {
    @NonNull
    String getFriendlyName();

    @NonNull
    String getCode();

    int getPriority();

    @NonNull
    Class<? extends Sender<? extends IPeer>> getSenderClass();

    @NonNull
    Class<? extends Discoverer> getDiscovererClass();

    @NonNull
    Class<? extends Receiver> getReceiverClass();

    @NonNull
    Class<? extends IPeer> getPeerClass();

    @NonNull
    Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass();
}
