package org.exthmui.share;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.exthmui.share.misc.Constants;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.services.DiscoverableTileService;
import org.exthmui.share.services.DiscoveringTileService;
import org.exthmui.share.services.ReceiveService;
import org.exthmui.share.shared.misc.ServiceUtils;
import org.exthmui.share.shared.preferences.PluginPreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private final ServiceUtils.MyServiceConnection mDiscoverConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private DiscoverService mDiscoverService;


    private final ServiceUtils.MyServiceConnection mReceiveConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private ReceiveService mReceiveService;

    private GlobalSettingsFragment mGlobalSettingsFragment;
    private final List<Fragment> mFragmentList = new ArrayList<>();

    LinearLayout mPreferencesContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        mPreferencesContainer = findViewById(R.id.preferences_container);
        // Bind Service
        mDiscoverConnection.registerOnServiceConnectedListener(service -> mDiscoverService = (DiscoverService) service);
        mDiscoverConnection.registerOnServiceDisconnectedListener(name -> mDiscoverService = null);
        bindService(new Intent(SettingsActivity.this, DiscoverService.class), mDiscoverConnection, BIND_AUTO_CREATE);
        mReceiveConnection.registerOnServiceConnectedListener(service -> mReceiveService = (ReceiveService) service);
        mReceiveConnection.registerOnServiceDisconnectedListener(name -> mReceiveService = null);
        bindService(new Intent(SettingsActivity.this, ReceiveService.class), mReceiveConnection, BIND_AUTO_CREATE);

        if (savedInstanceState == null) {
            mFragmentList.clear();
            mGlobalSettingsFragment = new GlobalSettingsFragment();
            addFragment(new GlobalSettingsFragment());

            try {
                addFragments();
            } catch (@NonNull IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        } else {
            mFragmentList.addAll(getSupportFragmentManager().getFragments());
            checkGrantPermissionPreferences();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void addFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(mPreferencesContainer.getId(), fragment)
                .commit();
        mFragmentList.add(fragment);
        if (PluginPreferenceFragmentCompat.class.isAssignableFrom(fragment.getClass())) {
            checkGrantPermissionPreferences((PluginPreferenceFragmentCompat) fragment);
        }
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
     * Add/Remove permission tint to preferences
     */
    private void checkGrantPermissionPreferences() {
        for (Fragment fragment : mFragmentList) {
            if (!PluginPreferenceFragmentCompat.class.isAssignableFrom(fragment.getClass())) continue;
            checkGrantPermissionPreferences((PluginPreferenceFragmentCompat) fragment);
        }
    }

    private void checkGrantPermissionPreferences(@NonNull PluginPreferenceFragmentCompat preferenceFragment) {
            Constants.ConnectionType type = Constants.ConnectionType.parseFromPreferenceFragmentClass(preferenceFragment.getClass());
            if (type == null) return;
            if (mDiscoverService == null)
                mDiscoverConnection.registerOnServiceConnectedListener(myService -> {
                    DiscoverService service = (DiscoverService) myService;
                    preferenceFragment.checkDiscoverPermissions(service);
                });
            else preferenceFragment.checkDiscoverPermissions(mDiscoverService);

            if (mReceiveService == null)
                mReceiveConnection.registerOnServiceConnectedListener(myService -> {
                    ReceiveService service = (ReceiveService) myService;
                    preferenceFragment.checkReceivePermissions(service);
                });
            else preferenceFragment.checkReceivePermissions(mReceiveService);
    }

    public void addFragment(@NonNull Class<? extends Fragment> fragmentClass) throws IllegalAccessException, InstantiationException {
        addFragment(fragmentClass.newInstance());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkGrantPermissionPreferences();
        TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoveringTileService.class.getName()));
        TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoverableTileService.class.getName()));
    }

    public void removeFragment(@NonNull Class<? extends Fragment> fragmentClass) {
        List<Fragment> fragmentList = new ArrayList<>(mFragmentList);
        for (Fragment fragment : fragmentList) {
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
        if (mDiscoverService != null) {
            mDiscoverService.beforeUnbind();
        }
        unbindService(mDiscoverConnection);
        mDiscoverService = null;
        if (mReceiveService != null) {
            mReceiveService.beforeUnbind();
        }
        unbindService(mReceiveConnection);
        mReceiveService = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}