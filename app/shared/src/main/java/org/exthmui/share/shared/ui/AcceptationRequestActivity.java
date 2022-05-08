package org.exthmui.share.shared.ui;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_FILE_INFOS;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_PEER_INFO_TRANSFER;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_PLUGIN_CODE;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_REQUEST_ID;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;

public class AcceptationRequestActivity extends AppCompatActivity {
    public static final String TAG = "AcceptationRequestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pluginCode = getIntent().getStringExtra(EXTRA_PLUGIN_CODE);
        SenderInfo senderInfo = (SenderInfo) getIntent().getSerializableExtra(EXTRA_PEER_INFO_TRANSFER);
        FileInfo[] fileInfos = (FileInfo[]) getIntent().getSerializableExtra(EXTRA_FILE_INFOS);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        int notificationId = getIntent().getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        if (pluginCode == null || senderInfo == null || fileInfos == null || requestId == null) {
            Log.e(TAG, "Invalid extras");
            this.finish();
        }
        AcceptationRequestBottomSheetFragment shareFragment = new AcceptationRequestBottomSheetFragment(pluginCode, senderInfo, fileInfos, requestId, notificationId);
        shareFragment.show(getSupportFragmentManager(), shareFragment.getTag());
    }
}