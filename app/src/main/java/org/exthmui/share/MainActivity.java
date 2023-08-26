package org.exthmui.share;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.exthmui.share.shared.IShareBroadcastReceiver;
import org.exthmui.utils.ServiceUtils;

public class MainActivity extends AppCompatActivity {
    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
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
    }
}