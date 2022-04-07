package org.exthmui.share.web;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.TimeUnit;

public class WebServerService extends Service {
    public static final String TAG = "WebServerService";
    private static Server mServer;
    private static WebServerService sInstance;

    public static WebServerService getInstance(){
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Log.d(TAG, "Service created");
        new Thread() {
            @Override
            public void run() {
                super.run();
                mServer = AndServer.webServer(getApplicationContext())
                        .port(8080)
                        .timeout(10, TimeUnit.SECONDS)
                        .build();
                Log.d(TAG, "Server created");
            }
        }.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        mServer.startup();
        Log.d(TAG, "Server started");
        return ret;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServer.shutdown();
        Log.d(TAG, "Server stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Server getServer() {
        return mServer;
    }
}