package org.exthmui.share.misc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public abstract class Constants {
    public enum ConnectionType implements org.exthmui.share.shared.misc.IConnectionType {
        MSNEARSHARE(new org.exthmui.share.msnearshare.Metadata()),
        WIFIDIRECT(new org.exthmui.share.wifidirect.Metadata()),
        LANNSD(new org.exthmui.share.lannsd.Metadata());

        @NonNull
        private final String friendlyName;
        @NonNull
        private final String code;
        @NonNull
        private final int priority;
        @NonNull
        private final Class<? extends Sender<? extends IPeer>> senderClass;
        @NonNull
        private final Class<? extends Discoverer> discovererClass;
        @NonNull
        private final Class<? extends Receiver> receiverClass;
        @NonNull
        private final Class<? extends IPeer> peerClass;
        // IMPORTANT: preferenceFragmentClass must have a public non-argument constructor
        @NonNull
        private final Class<? extends PluginPreferenceFragmentCompat> preferenceFragmentClass;

        ConnectionType(@NonNull String friendlyName, @NonNull String code, int priority, @NonNull Class<? extends Sender<? extends IPeer>> senderClass, @NonNull Class<? extends Discoverer> discovererClass, @NonNull Class<? extends Receiver> receiverClass, @NonNull Class<? extends IPeer> peerClass, @NonNull Class<? extends PluginPreferenceFragmentCompat> preferenceFragmentClass) {
            this.friendlyName = friendlyName;
            this.code = code;
            this.priority = priority;
            this.senderClass = senderClass;
            this.discovererClass = discovererClass;
            this.receiverClass = receiverClass;
            this.peerClass = peerClass;
            this.preferenceFragmentClass = preferenceFragmentClass;
        }

        ConnectionType(@NonNull IConnectionType type) {
            this(type.getFriendlyName(), type.getCode(), type.getPriority(), type.getSenderClass(),
                    type.getDiscovererClass(), type.getReceiverClass(), type.getPeerClass(),
                    type.getPreferenceFragmentClass());
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
        public int getPriority() {
            return priority;
        }

        @Override
        @NonNull
        public Class<? extends Sender<? extends IPeer>> getSenderClass() {
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
        public Class<? extends IPeer> getPeerClass() {
            return peerClass;
        }

        @Override
        @NonNull
        public Class<? extends PluginPreferenceFragmentCompat> getPreferenceFragmentClass() {
            return preferenceFragmentClass;
        }

        @Nullable
        public static ConnectionType parseFromCode(String code) {
            for (ConnectionType o : ConnectionType.values()) {
                if (o.getCode().equals(code)) {
                    return o;
                }
            }
            return null;
        }

        @Nullable
        public static ConnectionType parseFromPreferenceFragmentClass(@NonNull Class<? extends PluginPreferenceFragmentCompat> preferenceFragmentClass) {
            for (ConnectionType o : ConnectionType.values()) {
                if (preferenceFragmentClass.isAssignableFrom(o.getPreferenceFragmentClass())) {
                    return o;
                }
            }
            return null;
        }
    }
}
