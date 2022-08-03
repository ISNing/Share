package org.exthmui.share.lannsd;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.preferences.IntEditTextPreference;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public class NsdSettingsFragment extends PluginPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey, Object ignored) {
        setPreferencesFromResource(R.xml.nsd_preferences, rootKey);

        IntEditTextPreference timeoutPrefs = findPreference(getString(R.string.prefs_key_lannsd_timeout));
        IntEditTextPreference serverPortTcpPrefs = findPreference(getString(R.string.prefs_key_lannsd_server_port_tcp));
        IntEditTextPreference serverPortUdpPrefs = findPreference(getString(R.string.prefs_key_lannsd_server_port_udp));

        assert timeoutPrefs != null;
        timeoutPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            String newValStr = (String) newValue;
            int newValInt = Integer.parseInt(newValStr);
            if (!NsdUtils.isTimeoutValid(newValInt)) {
                Toast.makeText(requireContext(), R.string.prefs_tip_lannsd_timeout_invalid, Toast.LENGTH_SHORT).show();
                return false;
            }
            timeoutPrefs.setSummary(Integer.toString(newValInt));
            return true;
        });
        timeoutPrefs.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        assert serverPortTcpPrefs != null;
        serverPortTcpPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            String newValStr = (String) newValue;
            int newValInt = Integer.parseInt(newValStr);
            if (!NsdUtils.isServerPortValid(requireContext(), newValInt)) {
                Toast.makeText(requireContext(), R.string.prefs_tip_lannsd_server_port_tcp_invalid, Toast.LENGTH_SHORT).show();
                return false;
            }
            serverPortTcpPrefs.setSummary(newValStr);
            return true;
        });
        assert serverPortUdpPrefs != null;
        serverPortUdpPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            String newValStr = (String) newValue;
            int newValInt = Integer.parseInt(newValStr);
            if (!NsdUtils.isServerPortValid(requireContext(), newValInt)) {
                Toast.makeText(requireContext(), R.string.prefs_tip_lannsd_server_port_udp_invalid, Toast.LENGTH_SHORT).show();
                return false;
            }
            serverPortUdpPrefs.setSummary(newValStr);
            return true;
        });

        serverPortTcpPrefs.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        serverPortUdpPrefs.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        timeoutPrefs.setSummary(timeoutPrefs.getText());
        serverPortTcpPrefs.setSummary(serverPortTcpPrefs.getText());
        serverPortUdpPrefs.setSummary(serverPortUdpPrefs.getText());
    }

    @NonNull
    @Override
    public IConnectionType getType() {
        return new Metadata();
    }
}