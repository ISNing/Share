package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.preference.PreferenceManager;

import androidx.fragment.app.Fragment;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;

import org.exthmui.share.misc.Constants;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.shared.ServiceUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GlobalSettingsFragment extends PreferenceFragmentCompat {
    MultiSelectListPreference pluginsEnabledPrefs;
    ServiceConnection mConnection;
    DiscoverService mService;

    public String buildSummaryForPluginsEnabledPrefs(Collection<String> codes) {
        if (codes == null) codes = Collections.emptySet();
        Constants.ConnectionType[] types = Constants.ConnectionType.values();
        String summaryPrefix = getString(R.string.prefs_summary_global_plugins_enabled_prefix);
        String summarySuffix = getString(R.string.prefs_summary_global_plugins_enabled_suffix);
        StringBuilder summary = new StringBuilder(summaryPrefix);
        for (Constants.ConnectionType type: types) {
            if (codes.contains(type.getCode())) {
                summary.append(summary.toString().equals(getString(R.string.prefs_summary_global_plugins_enabled_prefix)) ? " " : " & ").append(type.getFriendlyName());
            }
        }
        if (summary.toString().equals(summaryPrefix)) {
            summary = new StringBuilder(getString(R.string.prefs_summary_global_plugins_enabled_none));
        } else summary.append(" ").append(summarySuffix);
        return summary.toString();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        Constants.ConnectionType[] types = Constants.ConnectionType.values();
        String[] entries = new String[types.length];
        String[] codes = new String[types.length];
        for (int i=0;i < types.length; i++) {
            entries[i] = types[i].getFriendlyName();
            codes[i] = types[i].getCode();
        }
        pluginsEnabledPrefs = new MultiSelectListPreference(requireContext());
        pluginsEnabledPrefs.setTitle(R.string.prefs_title_global_plugins_enabled);
        pluginsEnabledPrefs.setEntries(entries);
        pluginsEnabledPrefs.setEntryValues(codes);
        pluginsEnabledPrefs.setKey(getString(R.string.prefs_key_global_plugins_enabled));
        Set<String> pluginsEnabledPrefsValue = PreferenceManager.getDefaultSharedPreferences(requireContext()).getStringSet(pluginsEnabledPrefs.getKey(), Collections.emptySet());
        pluginsEnabledPrefs.setSummary(buildSummaryForPluginsEnabledPrefs(pluginsEnabledPrefsValue));
        pluginsEnabledPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            Set<String> prefsValueNew = (Set<String>) newValue;
            if (prefsValueNew == null)
                prefsValueNew = Collections.emptySet();
            Set<String> prefsValueOld = PreferenceManager.getDefaultSharedPreferences(requireContext()).getStringSet(preference.getKey(), Collections.emptySet());
            Set<String> whatAdded = new HashSet<>(prefsValueNew);
            whatAdded.removeAll(prefsValueOld);
            Set<String> whatRemoved = new HashSet<>(prefsValueOld);
            whatRemoved.removeAll(prefsValueNew);
            preference.setSummary(buildSummaryForPluginsEnabledPrefs(prefsValueNew));
            try {
                SettingsActivity activity = (SettingsActivity) getActivity();
                if (activity != null) {
                    for (String code : whatRemoved) {
                        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
                        if (type == null) continue;
                        Class<? extends Fragment> fragment = type.getPreferenceFragmentClass();
                        activity.removeFragment(fragment);
                    }
                    for (String code : whatAdded) {
                        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
                        if (type == null) continue;
                        Class<? extends Fragment> fragment = type.getPreferenceFragmentClass();
                        activity.addFragment(fragment);
                    }
                    mConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            ServiceUtils.MyService.MyBinder binder = (ServiceUtils.MyService.MyBinder) service;
                            mService = (DiscoverService) binder.getService();
                            for (String code: whatRemoved)
                                mService.removeDiscoverer(code);
                            for (String code: whatAdded)
                                mService.addDiscoverer(code);
                            activity.unbindService(mConnection);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            mService = null;
                        }
                    };
                    activity.bindService(new Intent(getContext(),DiscoverService.class), mConnection, BIND_AUTO_CREATE);
                }
            } catch (ClassCastException ignored) {} catch (IllegalAccessException | java.lang.InstantiationException exception) {
                exception.printStackTrace();
            }
            return true;
        });
        getPreferenceScreen().addPreference(pluginsEnabledPrefs);
    }
}
