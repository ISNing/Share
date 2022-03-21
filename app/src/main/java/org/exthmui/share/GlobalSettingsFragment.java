package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.exthmui.share.misc.Constants;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.preferences.ClickableStringPreference;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GlobalSettingsFragment extends PreferenceFragmentCompat {
    MultiSelectListPreference pluginsEnabledPrefs;
    ServiceConnection mConnection;
    DiscoverService mService;

    ClickableStringPreference mDestinationDirectoryPrefs;

    ActivityResultLauncher<?> mDestinationDirectoryActivityResultLauncher;

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
            @SuppressWarnings("unchecked")
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
                    mConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            ServiceUtils.MyService.MyBinder binder = (ServiceUtils.MyService.MyBinder) service;
                            mService = (DiscoverService) binder.getService();
                            for (String code: whatRemoved)
                                mService.removeDiscoverer(code);
                            for (String code : whatAdded)
                                mService.addDiscoverer(code);
                            activity.unbindService(mConnection);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            mService = null;
                        }
                    };
                    activity.bindService(new Intent(getContext(), DiscoverService.class), mConnection, BIND_AUTO_CREATE);
                    for (String code : whatRemoved) {
                        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
                        if (type == null) continue;
                        Class<? extends PluginPreferenceFragmentCompat> fragment = type.getPreferenceFragmentClass();
                        activity.removeFragment(fragment);
                    }
                    for (String code : whatAdded) {
                        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
                        if (type == null) continue;
                        Class<? extends PluginPreferenceFragmentCompat> fragmentClass = type.getPreferenceFragmentClass();
                        activity.addFragment(fragmentClass);
                    }
                }
            } catch (ClassCastException ignored) {
            } catch (IllegalAccessException | java.lang.InstantiationException exception) {
                exception.printStackTrace();
            }
            return true;
        });
        getPreferenceScreen().addPreference(pluginsEnabledPrefs);

        mDestinationDirectoryPrefs = findPreference(getString(R.string.prefs_key_global_destination_directory));
        assert mDestinationDirectoryPrefs != null;
        mDestinationDirectoryPrefs.setOnPreferenceClickListener(preference -> {
            mDestinationDirectoryActivityResultLauncher.launch(null);
            return true;
        });
        if (mDestinationDirectoryPrefs.getValue() == null) {
            mDestinationDirectoryPrefs.setSummary(getString(R.string.prefs_summary_global_destination_directory_default));
        } else mDestinationDirectoryPrefs.setSummary(mDestinationDirectoryPrefs.getValue());

        EditTextPreference defaultFileNamePrefs = findPreference(getString(R.string.prefs_key_global_default_file_name));
        assert defaultFileNamePrefs != null;
        defaultFileNamePrefs.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        defaultFileNamePrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            defaultFileNamePrefs.setSummary((String) newValue);
            return true;
        });
        defaultFileNamePrefs.setSummary(defaultFileNamePrefs.getText());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDestinationDirectoryActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            mDestinationDirectoryPrefs.setValue(uri.toString());
            mDestinationDirectoryPrefs.setSummary(uri.toString());
        });
    }
}
