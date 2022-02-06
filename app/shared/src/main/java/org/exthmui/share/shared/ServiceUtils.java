package org.exthmui.share.shared;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceUtils {
    public static boolean isServiceRunning(Context context,String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(30);

        if (!(serviceList.size()>0)) return false;

        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className)) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    public interface OnServiceConnectedListener extends EventListener {
        void onServiceConnected(MyService service);
    }

    public static abstract class MyService extends Service {
        public class MyBinder extends Binder {
            public Service getService() {
                return MyService.this;
            }
        }

        private final MyBinder mBinder = new MyBinder();

        @Override
        public IBinder onBind(Intent intent) {
            return mBinder;
        }
    }



    public static class MyServiceConnection implements ServiceConnection {
        private final Set<OnServiceConnectedListener> mOnServiceConnectedListeners = new HashSet<>();

        public void registerOnServiceConnectedListener(OnServiceConnectedListener listener) {
            mOnServiceConnectedListeners.add(listener);
        }

        public void unregisterOnServiceConnectedListener(OnServiceConnectedListener listener) {
            mOnServiceConnectedListeners.remove(listener);
        }

        private MyService mService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.MyBinder binder = (MyService.MyBinder) service;
            mService = (MyService) binder.getService();
            for (OnServiceConnectedListener l:
                    mOnServiceConnectedListeners) {
                l.onServiceConnected(mService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService=null;
        }
    }
}
