package org.exthmui.share.shared.misc;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class ServiceUtils {

    public interface OnServiceConnectedListener extends EventListener {
        void onServiceConnected(MyService service);
    }

    public interface OnServiceDisconnectedListener extends EventListener {
        void onServiceDisconnected(ComponentName name);
    }

    public static abstract class MyService extends Service {
        public static class MyBinder extends Binder {
            private MyService mService;
            private MyBinder(MyService s) {
                mService = s;
            }
            @NonNull
            public Service getService() {
                return mService;
            }
        }

        private final MyBinder mBinder = new MyBinder(this);

        @NonNull
        @Override
        public IBinder onBind(Intent intent) {
            onBind(intent, null);
            return mBinder;
        }

        public void onBind(Intent intent, Object ignored) {
        }

        @Override
        public void onDestroy() {
            mBinder.mService = null;
            super.onDestroy();
        }
    }


    public static class MyServiceConnection implements ServiceConnection {
        private final Set<OnServiceConnectedListener> mOnServiceConnectedListeners = new CopyOnWriteArraySet<>();
        private final Set<OnServiceDisconnectedListener> mOnServiceDisconnectedListeners = new CopyOnWriteArraySet<>();

        public void registerOnServiceConnectedListener(OnServiceConnectedListener listener) {
            mOnServiceConnectedListeners.add(listener);
        }

        public void unregisterOnServiceConnectedListener(OnServiceConnectedListener listener) {
            mOnServiceConnectedListeners.remove(listener);
        }

        public void registerOnServiceDisconnectedListener(OnServiceDisconnectedListener listener) {
            mOnServiceDisconnectedListeners.add(listener);
        }

        public void unregisterOnServiceDisconnectedListener(OnServiceDisconnectedListener listener) {
            mOnServiceDisconnectedListeners.remove(listener);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.MyBinder binder = (MyService.MyBinder) service;
            MyService myService = (MyService) binder.getService();
            for (OnServiceConnectedListener l:
                    mOnServiceConnectedListeners) {
                l.onServiceConnected(myService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            for (OnServiceDisconnectedListener l:
                    mOnServiceDisconnectedListeners) {
                l.onServiceDisconnected(name);
            }
        }
    }
}
