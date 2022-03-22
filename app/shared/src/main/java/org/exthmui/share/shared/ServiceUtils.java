package org.exthmui.share.shared;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

public class ServiceUtils {


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
            onBind(intent, null);
            return mBinder;
        }

        public void onBind(Intent intent, Object ignored) {
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
