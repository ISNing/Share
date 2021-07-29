package org.exthmui.share.shared.preferences;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.services.IDiscoverService;
import org.exthmui.share.shared.services.IReceiveService;

public abstract class PluginPreferenceFragmentCompat extends PreferenceFragmentCompat {
    private final static int DISCOVER_GRANT_PREFERENCE_ORDER = -2;
    private final static int RECEIVE_GRANT_PREFERENCE_ORDER = -1;

    private Preference mGrantPreferenceDiscover;
    private Preference mGrantPreferenceReceive;

    private IDiscoverService discoverService;
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
                addDiscoverGrantPermissionPreference(discoverService);
                discoverService = null;
            }
            if (toAddGrantPreferenceReceive) {
                toAddGrantPreferenceReceive = false;
                addReceiveGrantPermissionPreference(receiveService);
                receiveService = null;
            }
        }
    }

    public abstract void onCreatePreferences(Bundle savedInstanceState, String rootKey, Object ignored);

    public void checkDiscoverPermissions(IDiscoverService discoverService) {
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

    public void checkReceivePermissions(IReceiveService receiveService) {
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

    public void addDiscoverGrantPermissionPreference(IDiscoverService service) {
        IConnectionType type = getType();
        Preference grantPreference = new Preference(requireContext());
        grantPreference.setTitle(getString(R.string.prefs_title_global_permissions_not_granted_discover, type.getFriendlyName()));
        grantPreference.setSummary(R.string.prefs_summary_global_permissions_not_granted_discover);
        grantPreference.setOrder(DISCOVER_GRANT_PREFERENCE_ORDER);
        grantPreference.setOnPreferenceClickListener(preference -> {
            service.grantDiscovererPermissions(type.getCode(), requireActivity(), type.getDiscovererClass().hashCode());
            return false;
        });
        mGrantPreferenceDiscover = grantPreference;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) return;
        getPreferenceScreen().addPreference(mGrantPreferenceDiscover);
    }

    public void addReceiveGrantPermissionPreference(IReceiveService service) {
        IConnectionType type = getType();
        Preference grantPreference = new Preference(requireContext());
        grantPreference.setTitle(getString(R.string.prefs_title_global_permissions_not_granted_receive, type.getFriendlyName()));
        grantPreference.setSummary(R.string.prefs_summary_global_permissions_not_granted_receive);
        grantPreference.setOrder(RECEIVE_GRANT_PREFERENCE_ORDER);
        grantPreference.setOnPreferenceClickListener(preference -> {
            service.grantReceiverPermissions(type.getCode(), requireActivity(), type.getReceiverClass().hashCode());
            return false;
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
