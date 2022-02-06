package org.exthmui.share.wifidirect;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class DirectSettingsFragment extends PreferenceFragmentCompat {

    // TODO: Add value check and summary setting
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.direct_preferences, rootKey);
    }
}