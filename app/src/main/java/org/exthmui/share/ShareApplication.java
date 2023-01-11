package org.exthmui.share;

import android.app.Application;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

public class ShareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            AppCenter.start(this, getPackageManager()
                            .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                            .metaData.getString("appCenterSecret"),
                    Analytics.class, Crashes.class);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
