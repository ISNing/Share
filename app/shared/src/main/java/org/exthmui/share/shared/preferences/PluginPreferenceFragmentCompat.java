package org.exthmui.share.shared.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.services.IDiscoverService;
import org.exthmui.share.shared.services.IReceiveService;

public abstract class PluginPreferenceFragmentCompat extends PreferenceFragmentCompat {
    private final static int DISCOVER_GRANT_PREFERENCE_ORDER = -2;
    private final static int RECEIVE_GRANT_PREFERENCE_ORDER = -1;

    @Nullable
    private Preference mGrantPreferenceDiscover;
    @Nullable
    private Preference mGrantPreferenceReceive;

    @Nullable
    private IDiscoverService discoverService;
    @Nullable
    private IReceiveService receiveService;

    private boolean toAddGrantPreferenceDiscover;
    private boolean toAddGrantPreferenceReceive;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View ret = super.onCreateView(inflater, container, savedInstanceState);
        RecyclerView listView = getListView();
        listView.setNestedScrollingEnabled(false);// Disable nested scrolling
        return ret;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        onCreatePreferences(savedInstanceState, rootKey, null);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            if (mGrantPreferenceDiscover != null)
                preferenceScreen.addPreference(mGrantPreferenceDiscover);
            if (mGrantPreferenceReceive != null)
                preferenceScreen.addPreference(mGrantPreferenceReceive);
            if (toAddGrantPreferenceDiscover) {
                toAddGrantPreferenceDiscover = false;
                assert discoverService != null;
                addDiscoverGrantPermissionPreference(discoverService);
                discoverService = null;
            }
            if (toAddGrantPreferenceReceive) {
                toAddGrantPreferenceReceive = false;
                assert receiveService != null;
                addReceiveGrantPermissionPreference(receiveService);
                receiveService = null;
            }
        }
    }

    public abstract void onCreatePreferences(Bundle savedInstanceState, String rootKey, Object ignored);

    public void checkDiscoverPermissions(@NonNull IDiscoverService discoverService) {
        removeDiscoverGrantPermissionPreference();
        if (!discoverService.getDiscovererPermissionsNotGranted(getType().getCode()).isEmpty()) {
            if (isAdded()) {
                addDiscoverGrantPermissionPreference(discoverService);
            } else {
                this.discoverService = discoverService;
                toAddGrantPreferenceDiscover = true;
            }
        }
    }

    public void checkReceivePermissions(@NonNull IReceiveService receiveService) {
        removeReceiveGrantPermissionPreference();

        if (!receiveService.getReceiverPermissionsNotGranted(getType().getCode()).isEmpty()) {
            if (isAdded()) {
                addReceiveGrantPermissionPreference(receiveService);
            } else {
                this.receiveService = receiveService;
                toAddGrantPreferenceReceive = true;
            }
        }
    }

    public void addDiscoverGrantPermissionPreference(@NonNull IDiscoverService service) {
        IConnectionType type = getType();
        Preference grantPreference = new Preference(requireContext());
        grantPreference.setTitle(getString(R.string.prefs_title_global_permissions_not_granted_discover, type.getFriendlyName()));
        grantPreference.setSummary(R.string.prefs_summary_global_permissions_not_granted_discover);
        grantPreference.setOrder(DISCOVER_GRANT_PREFERENCE_ORDER);
        grantPreference.setOnPreferenceClickListener(preference -> {
            if (service.getDiscovererPermissionsNotGranted(type.getCode()).isEmpty())
                checkDiscoverPermissions(service);
            else
                service.grantDiscovererPermissions(type.getCode(), requireActivity(), type.getDiscovererClass().hashCode());
            return true;
        });
        mGrantPreferenceDiscover = grantPreference;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) return;
        getPreferenceScreen().addPreference(mGrantPreferenceDiscover);
    }

    public void addReceiveGrantPermissionPreference(@NonNull IReceiveService service) {
        IConnectionType type = getType();
        Preference grantPreference = new Preference(requireContext());
        grantPreference.setTitle(getString(R.string.prefs_title_global_permissions_not_granted_receive, type.getFriendlyName()));
        grantPreference.setSummary(R.string.prefs_summary_global_permissions_not_granted_receive);
        grantPreference.setOrder(RECEIVE_GRANT_PREFERENCE_ORDER);
        grantPreference.setOnPreferenceClickListener(preference -> {
            if (service.getReceiverPermissionsNotGranted(type.getCode()).isEmpty())
                checkReceivePermissions(service);
            else
                service.grantReceiverPermissions(type.getCode(), requireActivity(), type.getReceiverClass().hashCode());
            return true;
        });
        mGrantPreferenceReceive = grantPreference;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) return;
        getPreferenceScreen().addPreference(mGrantPreferenceReceive);
    }

    public void removeDiscoverGrantPermissionPreference() {
        if (mGrantPreferenceDiscover == null) return;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) return;
        preferenceScreen.removePreference(mGrantPreferenceDiscover);
        mGrantPreferenceDiscover = null;
    }

    public void removeReceiveGrantPermissionPreference() {
        if (mGrantPreferenceReceive == null) return;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) return;
        preferenceScreen.removePreference(mGrantPreferenceReceive);
        mGrantPreferenceReceive = null;
    }

    @NonNull
    public abstract IConnectionType getType();
}
