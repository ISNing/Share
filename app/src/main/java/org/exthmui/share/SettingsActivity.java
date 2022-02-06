package org.exthmui.share;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.exthmui.share.databinding.SettingsActivityBinding;
import org.exthmui.share.misc.Constants;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.services.DiscoverableTileService;
import org.exthmui.share.services.DiscoveringTileService;
import org.exthmui.share.services.ReceiveService;
import org.exthmui.share.shared.ServiceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    SettingsActivityBinding binding;

    private final ServiceUtils.MyServiceConnection mDiscoverConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private DiscoverService mDiscoverService;


    private final ServiceUtils.MyServiceConnection mReceiveConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private ReceiveService mReceiveService;

    private final List<Preference> mGrantPreferencesDiscover = new ArrayList<>();
    private final List<Preference> mGrantPreferencesReceive = new ArrayList<>();

    private GlobalSettingsFragment mGlobalSettingsFragment;
    private final List<Fragment> mFragmentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Bind Service
        mDiscoverConnection.registerOnServiceConnectedListener(service -> mDiscoverService = (DiscoverService) service);
        bindService(new Intent(SettingsActivity.this, DiscoverService.class), mDiscoverConnection, BIND_AUTO_CREATE);
        mReceiveConnection.registerOnServiceConnectedListener(service -> mReceiveService = (ReceiveService) service);
        bindService(new Intent(SettingsActivity.this, ReceiveService.class), mReceiveConnection, BIND_AUTO_CREATE);

        if (savedInstanceState == null) {
            mFragmentList.clear();
            mGlobalSettingsFragment = new GlobalSettingsFragment();
            addFragment(new GlobalSettingsFragment());

            try {
                addFragments();
            } catch (IllegalAccessException exception) {
                exception.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            addGrantPermissionPreferences();
        } else {
            mFragmentList.addAll(getSupportFragmentManager().getFragments());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void addFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(binding.preferencesContainer.getId(), fragment)
                .commit();
        mFragmentList.add(fragment);
    }

    private void addFragments() throws IllegalAccessException, InstantiationException {
        // Add preferences from plugins ang add permission tint to preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> codePluginsEnabled = sharedPreferences.getStringSet(getString(R.string.prefs_key_global_plugins_enabled), Collections.emptySet());
        for (String code : codePluginsEnabled) {
            Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
            if (type == null) continue;
            addFragment(type.getPreferenceFragmentClass());
        }
    }

    /**
     * Add permission tint to preferences
     */
    private void addGrantPermissionPreferences() {
        for (Fragment fragment : mFragmentList) {
            if (!PreferenceFragmentCompat.class.isAssignableFrom(fragment.getClass())) continue;
            PreferenceFragmentCompat preferenceFragment = (PreferenceFragmentCompat) fragment;
            Constants.ConnectionType type = Constants.ConnectionType.parseFromPreferenceFragmentClass(preferenceFragment.getClass());
            if (type == null) continue;
            try {
                Class<? extends PreferenceFragmentCompat> c = type.getPreferenceFragmentClass();
                PreferenceFragmentCompat preferenceFragmentCompat = c.newInstance();

                Preference grantPreferenceDiscover = new Preference(this);
                grantPreferenceDiscover.setTitle(String.format(getString(R.string.prefs_title_global_permissions_not_granted), type.getFriendlyName()));
                grantPreferenceDiscover.setSummary(R.string.prefs_summary_global_permissions_not_granted);
                grantPreferenceDiscover.setOrder(-1);
                grantPreferenceDiscover.setKey(type.getCode());
                if (mDiscoverService == null)
                    mDiscoverConnection.registerOnServiceConnectedListener(myService -> {
                        DiscoverService service = (DiscoverService) myService;
                        if (service.getDiscovererPermissionsNotGranted(type.getCode()).isEmpty()) return;
                        grantPreferenceDiscover.setOnPreferenceClickListener(preference -> {
                            service.grantDiscovererPermissions(type.getCode(), SettingsActivity.this, mGrantPreferencesDiscover.indexOf(grantPreferenceDiscover));
                            return false;
                        });
                        mGrantPreferencesDiscover.add(grantPreferenceDiscover);
                        preferenceFragmentCompat.getPreferenceScreen().addPreference(grantPreferenceDiscover);
                    });
                else if (!mDiscoverService.getDiscovererPermissionsNotGranted(type.getCode()).isEmpty()) {
                    grantPreferenceDiscover.setOnPreferenceClickListener(preference -> {
                        if (mDiscoverService == null)
                            mDiscoverConnection.registerOnServiceConnectedListener(service -> ((DiscoverService) service).grantDiscovererPermissions(type.getCode(), SettingsActivity.this, mGrantPreferencesDiscover.indexOf(grantPreferenceDiscover)));
                        mDiscoverService.grantDiscovererPermissions(type.getCode(), SettingsActivity.this, mGrantPreferencesDiscover.indexOf(grantPreferenceDiscover));
                        return false;
                    });
                    mGrantPreferencesDiscover.add(grantPreferenceDiscover);
                    preferenceFragmentCompat.getPreferenceScreen().addPreference(grantPreferenceDiscover);
                }

                Preference grantPreferenceReceive = new Preference(this);
                grantPreferenceReceive.setTitle(String.format(getString(R.string.prefs_title_global_permissions_not_granted), type.getFriendlyName()));
                grantPreferenceReceive.setSummary(R.string.prefs_summary_global_permissions_not_granted);
                grantPreferenceReceive.setOrder(-1);
                grantPreferenceReceive.setKey(type.getCode());
                if (mReceiveService == null)
                    mReceiveConnection.registerOnServiceConnectedListener(myService -> {
                        ReceiveService service = (ReceiveService) myService;
                        if (service.getReceiverPermissionsNotGranted(type.getCode()).isEmpty()) return;
                        grantPreferenceReceive.setOnPreferenceClickListener(preference -> {
                            service.grantReceiverPermissions(type.getCode(), SettingsActivity.this, mGrantPreferencesReceive.indexOf(grantPreferenceReceive));
                            return false;
                        });
                        mGrantPreferencesReceive.add(grantPreferenceReceive);
                        preferenceFragmentCompat.getPreferenceScreen().addPreference(grantPreferenceReceive);
                    });
                else if (!mReceiveService.getReceiverPermissionsNotGranted(type.getCode()).isEmpty()) {
                    grantPreferenceReceive.setOnPreferenceClickListener(preference -> {
                        if (mReceiveService == null)
                            mReceiveConnection.registerOnServiceConnectedListener(service -> ((ReceiveService) service).grantReceiverPermissions(type.getCode(), SettingsActivity.this, mGrantPreferencesReceive.indexOf(grantPreferenceReceive)));
                        mReceiveService.grantReceiverPermissions(type.getCode(), SettingsActivity.this, mGrantPreferencesReceive.indexOf(grantPreferenceReceive));
                        return false;
                    });
                    mGrantPreferencesReceive.add(grantPreferenceReceive);
                    preferenceFragmentCompat.getPreferenceScreen().addPreference(grantPreferenceReceive);
                }
                mFragmentList.add(preferenceFragmentCompat);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public void addFragment(@NonNull Class<? extends Fragment> fragmentClass) throws IllegalAccessException, InstantiationException {
        addFragment(fragmentClass.newInstance());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (Preference gPreference : mGrantPreferencesDiscover) {
            if (mDiscoverService == null) return;
            String code = gPreference.getKey();
            if (mDiscoverService.getDiscovererPermissionsNotGranted(code).isEmpty()) {
                Constants.ConnectionType t = Constants.ConnectionType.parseFromCode(code);
                if (t == null) return;
                for (Fragment fragment : mFragmentList) {
                    Class<? extends PreferenceFragmentCompat> fragmentClass = t.getPreferenceFragmentClass();
                    if (fragmentClass.isInstance(fragment)) {
                        ((PreferenceFragmentCompat) fragment).getPreferenceScreen().removePreference(gPreference);
                    }
                }
            }
        }
        for (Preference gPreference : mGrantPreferencesReceive) {
            if (mReceiveService == null) return;
            String code = gPreference.getKey();
            if (mReceiveService.getReceiverPermissionsNotGranted(code).isEmpty()) {
                Constants.ConnectionType t = Constants.ConnectionType.parseFromCode(code);
                if (t == null) return;
                for (Fragment fragment : mFragmentList) {
                    Class<? extends PreferenceFragmentCompat> fragmentClass = t.getPreferenceFragmentClass();
                    if (fragmentClass.isInstance(fragment)) {
                        ((PreferenceFragmentCompat) fragment).getPreferenceScreen().removePreference(gPreference);
                    }
                }
            }
        }
        TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoveringTileService.class.getName()));
        TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoverableTileService.class.getName()));
    }

    public void removeFragment(@NonNull Class<? extends Fragment> fragmentClass) {
        for (Fragment fragment : mFragmentList) {
            if (fragment.getClass().isAssignableFrom(fragmentClass)) {
                removeFragment(fragment);
            }
        }
    }

    public void removeFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
        mFragmentList.remove(fragment);
    }

    /**
     * Remove all preferences
     */
    private void removeFragments() {
        for (Fragment fragment : mFragmentList) {
            removeFragment(fragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mDiscoverConnection);
    }
}