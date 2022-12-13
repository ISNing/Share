package org.exthmui.share.web;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntRange;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.AsyncServer;
import com.yanzhenjie.andserver.delegate.IOReactorConfigDelegate;
import com.yanzhenjie.andserver.delegate.ListenerEndpoint;
import com.yanzhenjie.andserver.http.URIScheme;

import org.exthmui.share.shared.misc.ServiceUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class WebServerService extends ServiceUtils.MyService {
    public static final String TAG = "WebServerService";
    private static AsyncServer mServer;

    @IntRange(from = 0)
    private int mBindNumber = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        return START_STICKY;
    }

    public void startServer() {
        Log.d(TAG, "Starting server");
        mServer.startup();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
                mServer = AndServer.webServerAsync(getApplicationContext())
                        .setIOReactorConfig(IOReactorConfigDelegate.custom()
                                .setSoReuseAddress(true)
                                .setTcpNoDelay(true).build())
                        .setCanonicalHostName("192.168.101.38")
                        .setListener(new AsyncServer.ServerListener() {
                            @Override
                            public void onStarted() {
                                Future<ListenerEndpoint> endpoint = mServer.listen(new InetSocketAddress(8080), URIScheme.HTTP);
                                try {
                                    Log.d(TAG, String.format("Listening on %s", endpoint.get().getAddress()));
                                } catch (ExecutionException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onStopped() {

                            }

                            @Override
                            public void onException(Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .build();
                Log.d(TAG, "Server created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service going down(onDestroy), stopping server");
        mServer.shutdown();
    }


    private void startForeground() {
        Notification notification = NotificationUtils.buildServiceNotification(this);
        startForeground(this.hashCode(), notification);
    }

    private void stopForeground() {
        stopForeground(true);
    }

    @Override
    public void onBind(Intent intent, Object ignored) {
        Log.d(TAG, "Service going to be bound(onBind)");
        mBindNumber++;
        stopForeground();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Service going to be rebound(onRebind)");
        super.onRebind(intent);
        mBindNumber++;
        stopForeground();
    }

    /**
     * IMPORTANT: To keep service alive when discoverer running, MUST be called before unbind service.
     */
    public void beforeUnbind() {
        if (mBindNumber > 1) return;
        Log.d(TAG, "Checking whether to start service and in foreground or in background(beforeUnbind)");
        if (mServer.isRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, String.format("Server running, API newer than %d, starting service in foreground", Build.VERSION_CODES.O));
                startForegroundService(new Intent(getApplicationContext(), WebServerService.class));
                startForeground();
            } else {
                Log.d(TAG, String.format("Discoverer running, API lower than %d, starting service in background", Build.VERSION_CODES.O));
                startService(new Intent(getApplicationContext(), WebServerService.class));
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service going to be unbound(onUnbind)");
        mBindNumber--;
        return true;
    }
}