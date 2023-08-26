package org.exthmui.share.shared.base.file;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import org.exthmui.utils.StackTraceUtils;

public class AppInfo {

    public static final String TAG = "AppInfo";

    private PackageInfo packageInfo;

    private ApplicationInfo applicationInfo;

    /**
     * Application name
     */
    private String applicationName;

    /**
     * Version code
     */
    private long versionCode = 0;

    /**
     * Version name
     */
    private String versionName;

    /**
     * Package name
     */
    private String packageName;

    /**
     * Icon
     */
    private Drawable icon;

    /**
     * Sizes of source dirs
     */
    private long[] sizes;

    /**
     * Whether is this application a user app
     */
    private boolean isUserApp;

    /**
     * Whether is this application is installed as split apks
     */
    private boolean isSplit;

    /**
     * Full path to the base APK or split apks for this application
     */
    public String[] sourceDirs;

    /**
     * Whether is this application place in ROM
     */
    private boolean isExternal;

    public AppInfo(PackageInfo packageInfo, PackageManager packageManager) {
        setPackageInfo(packageInfo, packageManager);
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(PackageInfo packageInfo, PackageManager packageManager) {
        this.packageInfo = packageInfo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            setVersionCode(packageInfo.getLongVersionCode());
        else setVersionCode(packageInfo.versionCode);
        setVersionName(packageInfo.versionName);
        setApplicationInfo(packageInfo.applicationInfo, packageManager);
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    private void setApplicationInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo, PackageManager packageManager) {
        setApplicationInfo(applicationInfo);

        setPackageName(packageInfo.packageName);

        Drawable drawable = applicationInfo.loadIcon(packageManager);
        setIcon(drawable);

        String appName = applicationInfo.loadLabel(packageManager).toString();
        setApplicationName(appName);

        String[] splitSourceDirs = applicationInfo.splitSourceDirs;
        String sourceDir = applicationInfo.sourceDir;

        if (splitSourceDirs.length != 0) {
            setSourceDirs(splitSourceDirs);
            setSplit(true);
        } else {
            setSourceDirs(new String[]{sourceDir,});
            setSplit(false);
        }

        int flags = packageInfo.applicationInfo.flags;

        setUserApp((flags & ApplicationInfo.FLAG_SYSTEM) == 0);

        setExternal((flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == 0);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public long getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(long versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public long[] getSizes() {
        return sizes;
    }

    public void setSizes(long[] sizes) {
        this.sizes = sizes;
    }

    public boolean isUserApp() {
        return isUserApp;
    }

    public void setUserApp(boolean userApp) {
        isUserApp = userApp;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean external) {
        isExternal = external;
    }

    public boolean isSplit() {
        return isSplit;
    }

    public void setSplit(boolean split) {
        isSplit = split;
    }

    public String[] getSourceDirs() {
        return sourceDirs;
    }

    public void setSourceDirs(String[] sourceDirs) {
        this.sourceDirs = sourceDirs;

        long[] sizes = new long[sourceDirs.length];

        for (int i = 0; i < sizes.length; i++) {
            try {
                java.io.File file = new java.io.File(sourceDirs[i]);
                sizes[i] = file.length();
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed loading sourceDir: %s", sourceDirs[i]) + "\n" +
                        String.format("Error occurred while loading sourceDir: %s(message: %s)", e, e.getMessage()) + "\n" +
                        StackTraceUtils.getStackTraceString(e.getStackTrace()));
            }
        }

        setSizes(sizes);
    }
}
