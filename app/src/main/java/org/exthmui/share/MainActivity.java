package org.exthmui.share;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.exthmui.share.shared.IShareBroadcastReceiver;
import org.exthmui.share.shared.misc.ServiceUtils;
import org.exthmui.share.web.WebServerService;

public class MainActivity extends AppCompatActivity {
    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    WebServerService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
            mConnection.registerOnServiceConnectedListener(service -> {
                mService = (WebServerService) service;
                mService.startServer();
            });
            mConnection.registerOnServiceDisconnectedListener(name -> mService = null);
            bindService(new Intent(getApplicationContext(), WebServerService.class), mConnection, BIND_AUTO_CREATE);

            Intent dialogIntent = new Intent()
                    .setAction(IShareBroadcastReceiver.ACTION_ENABLE_RECEIVER)
                    .setPackage(this.getApplicationContext().getPackageName());
            sendBroadcast(dialogIntent);
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.beforeUnbind();
            unbindService(mConnection);
            mService = null;
        }
    }
}