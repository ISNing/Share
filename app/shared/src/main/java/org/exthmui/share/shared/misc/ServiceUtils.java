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
        public class MyBinder extends Binder {
            @NonNull
            public Service getService() {
                return MyService.this;
            }
        }

        private final MyBinder mBinder = new MyBinder();

        @NonNull
        @Override
        public IBinder onBind(Intent intent) {
            onBind(intent, null);
            return mBinder;
        }

        public void onBind(Intent intent, Object ignored) {
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

        @Nullable
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
            for (OnServiceDisconnectedListener l:
                    mOnServiceDisconnectedListeners) {
                l.onServiceDisconnected(name);
            }
        }
    }
}
