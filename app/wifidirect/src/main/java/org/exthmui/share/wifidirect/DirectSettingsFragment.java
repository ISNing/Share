package org.exthmui.share.wifidirect;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.preferences.IntEditTextPreference;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

public class DirectSettingsFragment extends PluginPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey, Object ignored) {
        setPreferencesFromResource(R.xml.direct_preferences, rootKey);

        IntEditTextPreference timeoutPrefs = findPreference(getString(R.string.prefs_key_wifidirect_timeout));
        IntEditTextPreference serverPortPrefs = findPreference(getString(R.string.prefs_key_wifidirect_server_port));
        IntEditTextPreference clientPortPrefs = findPreference(getString(R.string.prefs_key_wifidirect_client_port));
        IntEditTextPreference bufferSizePrefs = findPreference(getString(R.string.prefs_key_wifidirect_buffer_size));

        assert timeoutPrefs != null;
        timeoutPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String newValStr = (String) newValue;
                int newValInt = Integer.parseInt(newValStr);
                if (!DirectUtils.isTimeoutValid( newValInt)) {
                    Toast.makeText(requireContext(), R.string.prefs_tip_wifidirect_timeout_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }
                timeoutPrefs.setSummary(Integer.toString(newValInt));
                return true;
            }
        });
        timeoutPrefs.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        });
        assert serverPortPrefs != null;
        serverPortPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String newValStr = (String) newValue;
                int newValInt = Integer.parseInt(newValStr);
                if (!DirectUtils.isServerPortValid(requireContext(), newValInt)) {
                    Toast.makeText(requireContext(), R.string.prefs_tip_wifidirect_server_port_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }
                serverPortPrefs.setSummary(newValStr);
                return true;
            }
        });
        serverPortPrefs.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        });
        assert clientPortPrefs != null;
        clientPortPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String newValStr = (String) newValue;
                int newValInt = Integer.parseInt(newValStr);
                if (!DirectUtils.isClientPortValid(requireContext(), newValInt)) {
                    Toast.makeText(requireContext(), R.string.prefs_tip_wifidirect_client_port_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }
                clientPortPrefs.setSummary(newValStr);
                return true;
            }
        });
        clientPortPrefs.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
            }
        });
        assert bufferSizePrefs != null;
        bufferSizePrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String newValStr = (String) newValue;
                int newValInt = Integer.parseInt(newValStr);
                bufferSizePrefs.setSummary(Integer.toString(newValInt));
                return true;
            }
        });
        bufferSizePrefs.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        });

        timeoutPrefs.setSummary(timeoutPrefs.getText());
        serverPortPrefs.setSummary(serverPortPrefs.getText());
        clientPortPrefs.setSummary(clientPortPrefs.getText());
        bufferSizePrefs.setSummary(bufferSizePrefs.getText());
    }

    @NonNull
    @Override
    public IConnectionType getType() {
        return new Metadata();
    }
}