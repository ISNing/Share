package org.exthmui.share.msnearshare;

import static org.exthmui.share.msnearshare.Constants.EXCEPTION_MAPPING;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.microsoft.connecteddevices.AsyncOperationWithProgress;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareFileProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareHelper;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareProgress;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareSender;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareStatus;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.send.ReceiverInfo;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.base.send.SendingWorker;
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.shared.exceptions.trans.PeerDisappearedException;
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.misc.StackTraceUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NearShareMultiSendingWorker extends SendingWorker {

    public static final String TAG = "NearShareSendingWorker";

    public NearShareMultiSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @NonNull
    @Override
    public Result doWork() {
        Data input = getInputData();
        String[] uriStrings = input.getStringArray(Entity.FILE_URIS);
        String[] fileNames = input.getStringArray(Entity.FILE_NAMES);
        long[] fileSizes = input.getLongArray(Entity.FILE_SIZES);
        if (uriStrings == null || fileNames == null || fileSizes == null ||
                uriStrings.length != fileNames.length || fileNames.length != fileSizes.length)
            return genFailureResult(new InvalidInputDataException(getApplicationContext()));
        Uri[] uris = new Uri[uriStrings.length];
        FileInfo[] fileInfos = new FileInfo[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
            if (uriStrings[i] == null)
                return genFailureResult(new InvalidInputDataException(getApplicationContext()));
            uris[i] = Uri.parse(uriStrings[i]);
            fileInfos[i] = new FileInfo();
            fileInfos[i].setFileName(fileNames[i]);
            fileInfos[i].setFileSize(fileSizes[i]);
        }

        final NearShareManager manager = NearShareManager.getInstance(getApplicationContext());
        String peerId = input.getString(Sender.TARGET_PEER_ID);
        IPeer peer = manager.getPeers().get(peerId);
        if (peer == null)
            return genFailureResult(new PeerDisappearedException(getApplicationContext()));

        ReceiverInfo receiverInfo = new ReceiverInfo();

        receiverInfo.setId(peer.getId());
        receiverInfo.setDisplayName(peer.getDisplayName());
        // Other attributes are not supported in Microsoft NearShare

        NearShareSender sender = new NearShareSender();
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

        operation = sender.sendFilesAsync(connectionRequest, fileProviders);

        operation.progress().subscribe((op, progress) -> updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), progress.totalBytesToSend, progress.bytesSent, fileInfos, receiverInfo));

        operation.whenComplete((status, tr) -> {
            switch (status) {
                case COMPLETED:
                    result.set(genSuccessResult());
                    break;
                case DENIED_BY_REMOTE_SYSTEM:
                    result.set(genRejectedResult(getApplicationContext()));
                    break;
                case CANCELLED:
                    result.set(genReceiverCancelledResult(getApplicationContext()));
                    break;
                default:
                    if (tr != null) {
                        Log.e(TAG, "Failed sending files to " + peer.getDisplayName() + ": " + status);
                        Log.i(TAG, StackTraceUtils.getStackTraceString(tr.getStackTrace()));
                        result.set(genFailureResult(Objects.requireNonNull(EXCEPTION_MAPPING.getOrDefault(status, UnknownErrorException.class)).getConstructor(Context.class, Throwable.class).newInstance(getApplicationContext(), tr)));
                    } else {
                        Log.e(TAG, "Failed sending files to " + peer.getDisplayName() + ": " + status);
                        result.set(genFailureResult(Objects.requireNonNull(EXCEPTION_MAPPING.getOrDefault(status, UnknownErrorException.class)).getConstructor(Context.class).newInstance(getApplicationContext())));
                    }
                    break;
            }
            finished.set(true);
        });

        // Block until finished
        while (!finished.get()) {
            if (getForegroundInfoAsync().isCancelled()) {
                operation.cancel(true);
                return genSenderCancelledResult(getApplicationContext());
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