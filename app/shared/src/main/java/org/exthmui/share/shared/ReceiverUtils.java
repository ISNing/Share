package org.exthmui.share.shared;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.exthmui.share.shared.ShareBroadcastReceiver.ACTION_ACCEPT;
import static org.exthmui.share.shared.ShareBroadcastReceiver.ACTION_ACCEPTATION_DIALOG;
import static org.exthmui.share.shared.ShareBroadcastReceiver.ACTION_REJECT;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_FILE_NAME;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_FILE_SIZE;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_PEER_NAME;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_PLUGIN_CODE;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_REQUEST_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.Formatter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.UUID;

public class ReceiverUtils {
    private static final String RECEIVE_CHANNEL_ID = "org.exthmui.share.notification.channel.RECEIVE";

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_receive_name);
            String description = context.getString(R.string.notification_channel_receive_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(RECEIVE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static PendingIntent buildDialogPendingIntent(Context context, String pluginCode, String requestId, String peerName, String fileName, long fileSize) {
        Intent dialogIntent = new Intent(context, ShareBroadcastReceiver.class);
        dialogIntent.setAction(ACTION_ACCEPTATION_DIALOG);
        dialogIntent.putExtra(EXTRA_PLUGIN_CODE, pluginCode);
        dialogIntent.putExtra(EXTRA_REQUEST_ID, requestId);
        dialogIntent.putExtra(EXTRA_PEER_NAME, peerName);
        dialogIntent.putExtra(EXTRA_FILE_NAME, fileName);
        dialogIntent.putExtra(EXTRA_FILE_SIZE, fileSize);
        dialogIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        return PendingIntent.getBroadcast(context, 0, dialogIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent buildAcceptPendingIntent(Context context, String pluginCode, String requestId) {
        Intent acceptIntent = new Intent(context, ShareBroadcastReceiver.class);
        acceptIntent.setAction(ACTION_ACCEPT);
        acceptIntent.putExtra(EXTRA_PLUGIN_CODE, pluginCode);
        acceptIntent.putExtra(EXTRA_REQUEST_ID, requestId);
        acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        return PendingIntent.getBroadcast(context, 0, acceptIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent buildRejectPendingIntent(Context context, String pluginCode, String requestId) {
        Intent rejectIntent = new Intent(context, ShareBroadcastReceiver.class);
        rejectIntent.setAction(ACTION_REJECT);
        rejectIntent.putExtra(EXTRA_PLUGIN_CODE, pluginCode);
        rejectIntent.putExtra(EXTRA_REQUEST_ID, requestId);
        rejectIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        return PendingIntent.getBroadcast(context, 0, rejectIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    public static void requestAcceptation(Context context, String pluginCode, String requestId, String peerName, String fileName, long fileSizeBytes) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_request)
                .setContentTitle(context.getString(R.string.notification_title_receive_request))
                .setContentText(String.format(context.getString(R.string.notification_content_receive_request), peerName, fileName, Formatter.formatFileSize(context, fileSizeBytes)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(buildDialogPendingIntent(context, pluginCode, requestId, peerName, fileName, fileSizeBytes))
                .addAction(R.drawable.ic_action_accept, context.getString(R.string.notification_action_accept),
                        buildAcceptPendingIntent(context, pluginCode, requestId))
                .addAction(R.drawable.ic_action_reject, context.getString(R.string.notification_action_reject),
                        buildRejectPendingIntent(context, pluginCode, requestId));
        builder.build();
    }

    public static void buildRequestDialog(Context context, String pluginCode, String requestId, String peerName, String fileName, long fileSize) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.dialog_accept_or_reject_request);
        TextView title = dialog.findViewById(R.id.dialog_request_title);
        TextView sizeText = dialog.findViewById(R.id.dialog_request_size);
        MaterialButton acceptButton = dialog.findViewById(R.id.dialog_request_accept_button);
        MaterialButton rejectButton = dialog.findViewById(R.id.dialog_request_reject_button);

        assert title != null;
        title.setText(String.format(context.getString(R.string.dialog_title_accept_or_reject_request), peerName, fileName));
        assert sizeText != null;
        sizeText.setText(String.format(context.getString(R.string.dialog_accept_or_reject_request_size), Formatter.formatFileSize(context, fileSize)));

        assert acceptButton != null;
        acceptButton.setOnClickListener(v -> {
            PendingIntent pendingIntent = buildAcceptPendingIntent(context, pluginCode, requestId);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });

        assert rejectButton != null;
        rejectButton.setOnClickListener(v -> {
            PendingIntent pendingIntent = buildRejectPendingIntent(context, pluginCode, requestId);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });
        dialog.show();
    }

    public static Notification buildReceivingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesReceived, @Nullable String fileName, @Nullable String senderName, boolean indeterminate) {
        createNotificationChannel(context);

        String title;
        if (fileName == null)
            fileName = context.getString(R.string.notification_placeholder_unknown);
        if (senderName == null)
            senderName = context.getString(R.string.notification_placeholder_unknown);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            title = context.getString(R.string.notification_title_receive_waiting);
        else
            title = String.format(context.getString(R.string.notification_title_receive_receiving), fileName, senderName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_CHANNEL_ID).setContentTitle(title)
                .setContentTitle(title)
                .setContentText(context.getString(Constants.TransmissionStatus.parse(statusCode).getFriendlyStringRes()))
                .setSmallIcon(R.drawable.ic_notification_receive)
                .setOngoing(true);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent);
        if (statusCode != Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.setProgress((int) totalBytesToSend, (int) bytesReceived, indeterminate);
        return builder.build();
    }

    public static Notification buildReceivingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable String senderName, boolean indeterminate) {
        createNotificationChannel(context);

        String title;
        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i] == null)
                fileNames[i] = context.getString(R.string.notification_placeholder_unknown);
        }
        if (senderName == null)
            senderName = context.getString(R.string.notification_placeholder_unknown);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            title = context.getString(R.string.notification_title_receive_waiting);
        else if (fileNames.length == 2)
            title = String.format(context.getString(R.string.notification_title_sending_two), fileNames[0], fileNames[1], senderName);
        else
            title = String.format(context.getString(R.string.notification_title_sending_multi), fileNames[0], fileNames[1], fileNames.length - 2, senderName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_CHANNEL_ID).setContentTitle(title)
                .setContentTitle(title)
                .setContentText(context.getString(Constants.TransmissionStatus.parse(statusCode).getFriendlyStringRes()))
                .setSmallIcon(R.drawable.ic_notification_receive)
                .setOngoing(true);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent);
        if (statusCode != Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.setProgress((int) totalBytesToSend, (int) bytesReceived, indeterminate);
        return builder.build();
    }
}
