package org.exthmui.share.msnearshare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.microsoft.connecteddevices.AsyncOperationWithProgress;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareFileProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareHelper;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareProgress;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareSender;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareStatus;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.StackTraceUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.SendingWorker;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NearShareMultiSendingWorker extends SendingWorker {

    private static final String TAG = "NearShareSendingWorker";

    private NearShareSender mNearShareSender;

    public NearShareMultiSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data input = getInputData();
        String[] uriStrings = input.getStringArray(Entity.FILE_URIS);
        String[] fileNames = input.getStringArray(Entity.FILE_NAMES);
        if (uriStrings == null)
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "No file passed");
        if (fileNames == null)
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "No file passed");
        Uri[] uris = new Uri[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
            if (uriStrings[i] == null)
                return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "No file passed");
            uris[i] = Uri.parse(uriStrings[i]);
        }

        final NearShareManager manager = NearShareManager.getInstance(getApplicationContext());
        String peerId = input.getString(Sender.TARGET_PEER_ID);
        PeerInfo peer = manager.getPeers().get(peerId);
        if (peer == null)
            return genFailureResult(Constants.TransmissionStatus.PEER_DISAPPEARED.getNumVal(), "Could not get a valid Peer object by id:" + peerId);
        if (!manager.isInitialized()) manager.initialize();
        if (mNearShareSender == null)
            this.mNearShareSender = new NearShareSender();
        final RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(((NearSharePeer) peer).remoteSystem);

        final AsyncOperationWithProgress<NearShareStatus, NearShareProgress> operation;

        final NearShareFileProvider[] fileProviders = new NearShareFileProvider[uris.length];

        for (int i = 0; i < uris.length; i++) {
            NearShareFileProvider fileProvider = NearShareHelper.createNearShareFileFromContentUri(
                    uris[i], getApplicationContext());
            fileProviders[i] = fileProvider;
        }

        AtomicReference<Result> result = new AtomicReference<>(null);
        AtomicBoolean finished = new AtomicBoolean(false);

        operation = mNearShareSender.sendFilesAsync(connectionRequest, fileProviders);

        operation.progress().subscribe((op, progress) -> updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), progress.totalBytesToSend, progress.bytesSent, fileNames, peer.getDisplayName()));

        operation.whenComplete((status, tr) -> {
            if (status == NearShareStatus.COMPLETED) {
                result.set(Result.success(getInputData()));
                finished.set(true);
                return;
            }

            HashMap<NearShareStatus, Constants.TransmissionStatus> m = new HashMap<>() {
                {
                    put(NearShareStatus.UNKNOWN, Constants.TransmissionStatus.UNKNOWN_ERROR);
                    put(NearShareStatus.COMPLETED, Constants.TransmissionStatus.COMPLETED);
                    put(NearShareStatus.IN_PROGRESS, Constants.TransmissionStatus.IN_PROGRESS);
                    put(NearShareStatus.TIMED_OUT, Constants.TransmissionStatus.TIMED_OUT);
                    put(NearShareStatus.CANCELLED, Constants.TransmissionStatus.RECEIVER_CANCELLED);
                    put(NearShareStatus.DENIED_BY_REMOTE_SYSTEM, Constants.TransmissionStatus.REJECTED);
                }

                @SuppressLint("ObsoleteSdkInt")
                @Nullable
                @Override
                public Constants.TransmissionStatus getOrDefault(@Nullable Object key, @Nullable Constants.TransmissionStatus defaultValue) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        return super.getOrDefault(key, defaultValue);
                    } else {
                        Constants.TransmissionStatus ret = get(key);
                        return ret != null ? ret : defaultValue;
                    }
                }
            };

            if (tr != null) {
                Log.e(TAG, "Failed sending files to " + peer.getDisplayName() + ": " + status);
                Log.i(TAG, StackTraceUtils.getStackTraceString(tr.getStackTrace()));
                result.set(genFailureResult(Objects.requireNonNull(m.getOrDefault(status, Constants.TransmissionStatus.UNKNOWN_ERROR)).getNumVal(), tr.getLocalizedMessage()));
            } else {
                Log.e(TAG, "Failed sending files to " + peer.getDisplayName() + ": " + status);
                result.set(genFailureResult(Objects.requireNonNull(m.getOrDefault(status, Constants.TransmissionStatus.UNKNOWN_ERROR)).getNumVal(), null));
            }
            finished.set(true);
        });

        // Block until finished
        while (finished.get()) {
            if (getForegroundInfoAsync().isCancelled()) {
                operation.cancel(true);
                return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "User(aka sender) canceled sending file");
            }
        }
        return result.get();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        getForegroundInfoAsync().cancel(true);
    }
}