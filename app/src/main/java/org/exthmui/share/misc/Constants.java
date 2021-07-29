package org.exthmui.share.misc;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public class Constants {
    public enum ConnectionType implements org.exthmui.share.shared.misc.IConnectionType {
        MSNEARSHARE(new org.exthmui.share.msnearshare.Metadata()),
        WIFIDIRECT(new org.exthmui.share.wifidirect.Metadata());
        @NonNull private final String friendlyName;
        @NonNull private final String code;
        @NonNull private final Class<? extends Sender<? extends PeerInfo>> senderClass;
        @NonNull private final Class<? extends Discoverer> discovererClass;
        @NonNull private final Class<? extends Receiver> receiverClass;
        @NonNull private final Class<? extends PeerInfo> peerClass;
        // IMPORTANT: preferenceFragmentClass must have a public non-argument constructor
        @NonNull private final Class<? extends PluginPreferenceFragmentCompat> preferenceFragmentClass;

        ConnectionType(@NonNull String friendlyName, @NonNull String code, @NonNull Class<? extends Sender<? extends PeerInfo>> senderClass, @NonNull Class<? extends Discoverer> discovererClass, @NonNull Class<? extends Receiver> receiverClass, @NonNull Class<? extends PeerInfo> peerClass, @NonNull Class<? extends PluginPreferenceFragmentCompat> preferenceFragmentClass) {
            this.friendlyName = friendlyName;
            this.code = code;
            this.senderClass = senderClass;
            this.discovererClass = discovererClass;
            this.receiverClass = receiverClass;
            this.peerClass = peerClass;
            this.preferenceFragmentClass = preferenceFragmentClass;
        }

        ConnectionType(IConnectionType type) {
            this.friendlyName = type.getFriendlyName();
            this.code = type.getCode();
            this.senderClass = type.getSenderClass();
            this.discovererClass = type.getDiscovererClass();
            this.receiverClass = type.getReceiverClass();
            this.peerClass = type.getPeerClass();
            this.preferenceFragmentClass = type.getPreferenceFragmentClass();
        }

        @Override
        @NonNull
        public String getFriendlyName() {
            return friendlyName;
        }

        @Override
        @NonNull
        public String getCode() {
            return code;
        }

        @Override
        @NonNull
        public Class<? extends Sender<? extends PeerInfo>> getSenderClass() {
            return senderClass;
        }

        @Override
        @NonNull
        public Class<? extends Discoverer> getDiscovererClass() {
            return discovererClass;
        }

        @Override
        @NonNull
        public Class<? extends Receiver> getReceiverClass() {
            return receiverClass;
        }

        @Override
        @NonNull
        public Class<? extends PeerInfo> getPeerClass() {
            return peerClass;
        }

        @Override
        @NonNull
        public Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass() {
            return preferenceFragmentClass;
        }

        public static ConnectionType parseFromCode(String code) {
            for (ConnectionType o : ConnectionType.values()) {
                if (o.getCode().equals(code)) {
                    return o;
                }
            }
            return null;
        }

        public static ConnectionType parseFromPreferenceFragmentClass(Class<? extends PluginPreferenceFragmentCompat> preferenceFragmentClass) {
            for (ConnectionType o : ConnectionType.values()) {
                if (preferenceFragmentClass.isAssignableFrom(o.getPreferenceFragmentClass())) {
                    return o;
                }
            }
            return null;
        }
    }
}
