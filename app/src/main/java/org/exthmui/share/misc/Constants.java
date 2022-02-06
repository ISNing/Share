package org.exthmui.share.misc;

import static org.exthmui.share.shared.Constants.CONNECTION_CODE_MSNEARSHARE;
import static org.exthmui.share.shared.Constants.CONNECTION_CODE_WIFIDIRECT;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

import org.exthmui.share.msnearshare.NearShareManager;
import org.exthmui.share.msnearshare.NearSharePeer;
import org.exthmui.share.msnearshare.NearShareReceiver;
import org.exthmui.share.msnearshare.NearShareSettingsFragment;
import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.wifidirect.DirectManager;
import org.exthmui.share.wifidirect.DirectPeer;
import org.exthmui.share.wifidirect.DirectReceiver;
import org.exthmui.share.wifidirect.DirectSettingsFragment;

public class Constants {
    public enum ConnectionType {
        MSNEARSHARE("Microsoft NearShare",CONNECTION_CODE_MSNEARSHARE, NearShareManager.class, NearShareManager.class, NearShareReceiver.class, NearSharePeer.class, NearShareSettingsFragment.class),
        WIFIDIRECT("WiFi Direct", CONNECTION_CODE_WIFIDIRECT, DirectManager.class, DirectManager.class, DirectReceiver.class, DirectPeer.class, DirectSettingsFragment.class);
        @NonNull private final String friendlyName;
        @NonNull private final String code;
        @NonNull private final Class<? extends Sender<? extends PeerInfo>> senderClass;
        @NonNull private final Class<? extends Discoverer> discovererClass;
        @NonNull private final Class<? extends Receiver> receiverClass;
        @NonNull private final Class<? extends PeerInfo> peerClass;
        // IMPORTANT: preferenceFragmentClass must have a public non-argument constructor
        @NonNull private final Class<? extends PreferenceFragmentCompat> preferenceFragmentClass;

        ConnectionType(@NonNull String friendlyName, @NonNull String code, @NonNull Class<? extends Sender<? extends PeerInfo>> senderClass, @NonNull Class<? extends Discoverer> discovererClass, @NonNull Class<? extends Receiver> receiverClass, @NonNull Class<? extends PeerInfo> peerClass, @NonNull Class<? extends PreferenceFragmentCompat> preferenceFragmentClass) {
            this.friendlyName = friendlyName;
            this.code = code;
            this.senderClass = senderClass;
            this.discovererClass = discovererClass;
            this.receiverClass = receiverClass;
            this.peerClass = peerClass;
            this.preferenceFragmentClass = preferenceFragmentClass;
        }

        @NonNull
        public String getFriendlyName() {
            return friendlyName;
        }

        @NonNull
        public String getCode() {
            return code;
        }

        @NonNull
        public Class<? extends Sender<? extends PeerInfo>> getSenderClass() {
            return senderClass;
        }

        @NonNull
        public Class<? extends Discoverer> getDiscovererClass() {
            return discovererClass;
        }

        @NonNull
        public Class<? extends Receiver> getReceiverClass() {
            return receiverClass;
        }

        @NonNull
        public Class<? extends PeerInfo> getPeerClass() {
            return peerClass;
        }

        @NonNull
        public Class<? extends PreferenceFragmentCompat> getPreferenceFragmentClass() {
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

        public static ConnectionType parseFromPreferenceFragmentClass(Class<? extends PreferenceFragmentCompat> preferenceFragmentClass) {
            for (ConnectionType o : ConnectionType.values()) {
                if (preferenceFragmentClass.isInstance(o.getPreferenceFragmentClass())) {
                    return o;
                }
            }
            return null;
        }
    }

    public static final String PREFS_KEY_PLUGINS_ENABLED = "plugins_enabled";
}
