package org.exthmui.share.msnearshare;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;


public class NearShareSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.nearshare_preferences, rootKey);
    }
}
