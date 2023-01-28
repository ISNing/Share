package org.exthmui.share.msnearshare;

import android.os.Bundle;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;


public class NearShareSettingsFragment extends PluginPreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey, Object ignored) {
        setPreferencesFromResource(R.xml.nearshare_preferences, rootKey);
    }

    @NonNull
    @Override
    public IConnectionType getType() {
        return new Metadata();
    }
}
