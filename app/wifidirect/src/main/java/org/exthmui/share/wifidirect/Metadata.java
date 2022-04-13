package org.exthmui.share.wifidirect;

import static org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_WIFIDIRECT;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public class Metadata implements IConnectionType {
    @NonNull
    @Override
    public String getFriendlyName() {
        return "WiFi Direct";
    }

    @NonNull
    @Override
    public String getCode() {
        return CONNECTION_CODE_WIFIDIRECT;
    }

    @NonNull
    @Override
    public Class<? extends Sender<? extends PeerInfo>> getSenderClass() {
        return DirectManager.class;
    }

    @NonNull
    @Override
    public Class<? extends Discoverer> getDiscovererClass() {
        return DirectManager.class;
    }

    @NonNull
    @Override
    public Class<? extends Receiver> getReceiverClass() {
        return DirectReceiver.class;
    }

    @NonNull
    @Override
    public Class<? extends PeerInfo> getPeerClass() {
        return DirectPeer.class;
    }

    @NonNull
    @Override
    public Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass() {
        return DirectSettingsFragment.class;
    }
}
