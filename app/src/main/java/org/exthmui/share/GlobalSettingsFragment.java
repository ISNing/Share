package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.share.misc.Constants;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.services.ReceiveService;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.Utils;
import org.exthmui.share.shared.preferences.ClickableStringPreference;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlobalSettingsFragment extends PreferenceFragmentCompat {
    MultiSelectListPreference pluginsEnabledPrefs;
    ServiceConnection mDiscoverConnection;
    DiscoverService mDiscoverService;
    ServiceConnection mReceiveConnection;
    ReceiveService mReceiveService;

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
                    mDiscoverConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            ServiceUtils.MyService.MyBinder binder = (ServiceUtils.MyService.MyBinder) service;
                            mDiscoverService = (DiscoverService) binder.getService();
                            for (String code : whatRemoved)
                                mDiscoverService.removeDiscoverer(code);
                            for (String code : whatAdded)
                                mDiscoverService.addDiscoverer(code);
                            mDiscoverService.beforeUnbind();
                            requireContext().unbindService(mDiscoverConnection);
                            mDiscoverService = null;
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            mDiscoverService = null;
                        }
                    };
                    activity.bindService(new Intent(getContext(), DiscoverService.class), mDiscoverConnection, BIND_AUTO_CREATE);
                    mReceiveConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            ServiceUtils.MyService.MyBinder binder = (ServiceUtils.MyService.MyBinder) service;
                            mReceiveService = (ReceiveService) binder.getService();
                            for (String code : whatRemoved)
                                mReceiveService.removeReceiver(code);
                            for (String code : whatAdded)
                                mReceiveService.addReceiver(code);
                            mReceiveService.beforeUnbind();
                            requireContext().unbindService(mReceiveConnection);
                            mReceiveService = null;
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            mReceiveService = null;
                        }
                    };
                    requireContext().bindService(new Intent(getContext(), DiscoverService.class), mDiscoverConnection, BIND_AUTO_CREATE);
                    requireContext().bindService(new Intent(getContext(), ReceiveService.class), mReceiveConnection, BIND_AUTO_CREATE);
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

        EditTextPreference deviceNamePrefs = findPreference(getString(R.string.prefs_key_global_device_name));
        assert deviceNamePrefs != null;
        deviceNamePrefs.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        deviceNamePrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue != null){
                String newValueStr = (String) newValue;
                if (!newValueStr.isEmpty()) {
                    deviceNamePrefs.setSummary(newValueStr);
                    return true;
                } else {
                    deviceNamePrefs.setText(null);
                    deviceNamePrefs.callChangeListener(null);
                    return false;
                }
            } else {
                deviceNamePrefs.setSummary(Utils.getDeviceNameOnBoard(requireContext()));
                return true;
            }
        });
        if (deviceNamePrefs.getText() != null)
            deviceNamePrefs.setSummary(deviceNamePrefs.getText());
        else deviceNamePrefs.setSummary(Utils.getDeviceNameOnBoard(requireContext()));

        ClickableStringPreference peerIdPrefs = findPreference(getString(R.string.prefs_key_global_peer_id));
        assert peerIdPrefs != null;
        peerIdPrefs.setOnPreferenceClickListener(preference -> {
            peerIdPrefs.setValue(Utils.genPeerId());
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(requireContext(),
                    getString(R.string.toast_global_peer_id_regenerated), duration);
            toast.show();
            return true;
        });
        peerIdPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            if (Utils.isDevelopmentModeEnabled(requireContext().getContentResolver()))
                peerIdPrefs.setSummary(getString(R.string.prefs_summary_global_peer_id_show, peerIdPrefs.getValue()));
            return true;
        });
        if (peerIdPrefs.getValue() == null)
            peerIdPrefs.setValue(Utils.genPeerId());
        if (Utils.isDevelopmentModeEnabled(requireContext().getContentResolver()))
            peerIdPrefs.setSummary(getString(R.string.prefs_summary_global_peer_id_show, peerIdPrefs.getValue()));
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
