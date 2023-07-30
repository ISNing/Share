package org.exthmui.share;

import android.app.Application;

import com.google.android.material.color.DynamicColors;


public class ShareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
