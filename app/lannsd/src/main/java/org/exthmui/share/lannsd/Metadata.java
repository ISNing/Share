package org.exthmui.share.lannsd;

import static org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_LANNSD;
import static org.exthmui.share.shared.misc.Constants.CONNECTION_PRIORITY_LANNSD;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public class Metadata implements IConnectionType {
    @NonNull
    @Override
    public String getFriendlyName() {
        return "Network Service Discovery(LAN)";
    }

    @NonNull
    @Override
    public String getCode() {
        return CONNECTION_CODE_LANNSD;
    }

    @Override
    public int getPriority() {
        return CONNECTION_PRIORITY_LANNSD;
    }

    @NonNull
    @Override
    public Class<? extends Sender<? extends IPeer>> getSenderClass() {
        return NsdManager.class;
    }

    @NonNull
    @Override
    public Class<? extends Discoverer> getDiscovererClass() {
        return NsdManager.class;
    }

    @NonNull
    @Override
    public Class<? extends Receiver> getReceiverClass() {
        return NsdReceiver.class;
    }

    @NonNull
    @Override
    public Class<? extends IPeer> getPeerClass() {
        return NsdPeer.class;
    }

    @NonNull
    @Override
    public Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass() {
        return NsdSettingsFragment.class;
    }
}
