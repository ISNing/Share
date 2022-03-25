package org.exthmui.share.shared.ui;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_FILE_NAME;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_FILE_SIZE;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_PEER_NAME;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_PLUGIN_CODE;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_REQUEST_ID;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AcceptationRequestActivity extends AppCompatActivity {
    public static final String TAG = "AcceptationRequestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pluginCode = getIntent().getStringExtra(EXTRA_PLUGIN_CODE);
        String peerName = getIntent().getStringExtra(EXTRA_PEER_NAME);
        String fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        long fileSize = getIntent().getLongExtra(EXTRA_FILE_SIZE, -1);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        int notificationId = getIntent().getIntExtra(EXTRA_NOTIFICATION_ID, -1);
//        if (pluginCode == null | peerName == null | fileName == null | fileSize <= 0 | requestId == null) {
//            Log.e(TAG, "Invalid extras");
//            this.finish();
//        }
        AcceptationRequestBottomSheetFragment shareFragment = new AcceptationRequestBottomSheetFragment(pluginCode, peerName, fileName, fileSize, requestId, notificationId);
        shareFragment.show(getSupportFragmentManager(), shareFragment.getTag());
    }
}