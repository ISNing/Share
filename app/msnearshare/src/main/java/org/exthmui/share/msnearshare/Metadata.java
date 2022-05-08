package org.exthmui.share.msnearshare;

import static org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_MSNEARSHARE;
import static org.exthmui.share.shared.misc.Constants.CONNECTION_PRIORITY_MSNEARSHARE;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public class Metadata implements IConnectionType {

    @NonNull
    @Override
    public String getFriendlyName() {
        return "Microsoft NearShare";
    }

    @NonNull
    @Override
    public String getCode() {
        return CONNECTION_CODE_MSNEARSHARE;
    }

    @Override
    public int getPriority() {
        return CONNECTION_PRIORITY_MSNEARSHARE;
    }

    @NonNull
    @Override
    public Class<? extends Sender<? extends IPeer>> getSenderClass() {
        return NearShareManager.class;
    }

    @NonNull
    @Override
    public Class<? extends Discoverer> getDiscovererClass() {
        return NearShareManager.class;
    }

    @NonNull
    @Override
    public Class<? extends Receiver> getReceiverClass() {
        return NearShareReceiver.class;
    }

    @NonNull
    @Override
    public Class<? extends IPeer> getPeerClass() {
        return NearSharePeer.class;
    }

    @NonNull
    @Override
    public Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass() {
        return NearShareSettingsFragment.class;
    }
}
