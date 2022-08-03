package org.exthmui.share.msnearshare;

import static org.exthmui.share.msnearshare.Constants.EXCEPTION_MAPPING;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
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
import org.exthmui.share.shared.base.send.SendingWorker;
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.shared.exceptions.trans.PeerDisappearedException;
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.RejectedException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
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
    public Result doWork(Entity[] entities, String peerId, String peerName) {
        FileInfo[] fileInfos = new FileInfo[entities.length];
        for (int i = 0; i < entities.length; i++) {
            if (entities[i] == null)
                return genFailureResult(new InvalidInputDataException(getApplicationContext()), null);
            fileInfos[i] = new FileInfo(entities[i]);
        }

        final NearShareManager manager = NearShareManager.getInstance(getApplicationContext());
        IPeer peer = manager.getPeers().get(peerId);
        if (peer == null)
            return genFailureResult(new PeerDisappearedException(getApplicationContext()), null);

        ReceiverInfo receiverInfo = new ReceiverInfo();

        receiverInfo.setId(peer.getId());
        receiverInfo.setDisplayName(peer.getDisplayName());
        // Other attributes are not supported in Microsoft NearShare

        NearShareSender sender = new NearShareSender();
        final RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(((NearSharePeer) peer).remoteSystem);

        final AsyncOperationWithProgress<NearShareStatus, NearShareProgress> operation;

        final NearShareFileProvider[] fileProviders = new NearShareFileProvider[entities.length];

        for (int i = 0; i < entities.length; i++) {
            NearShareFileProvider fileProvider = NearShareHelper.createNearShareFileFromContentUri(
                    entities[i].getUri(), getApplicationContext());
            fileProviders[i] = fileProvider;
        }

        AtomicReference<Result> result = new AtomicReference<>(null);
        AtomicBoolean finished = new AtomicBoolean(false);

        operation = sender.sendFilesAsync(connectionRequest, fileProviders);

        operation.progress().subscribe((op, progress) -> updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), progress.totalBytesToSend, progress.bytesSent, fileInfos, receiverInfo, null, 0, 0));

        operation.whenComplete((status, tr) -> {
            switch (status) {
                case COMPLETED:
                    result.set(genSuccessResult());
                    break;
                case DENIED_BY_REMOTE_SYSTEM:
                    result.set(genFailureResult(new RejectedException(getApplicationContext()), null));
                    break;
                case CANCELLED:
                    result.set(genFailureResult(new ReceiverCancelledException(getApplicationContext()), null));
                    break;
                default:
                    if (tr != null) {
                        Log.e(TAG, "Failed sending files to " + peer.getDisplayName() + ": " + status);
                        Log.i(TAG, StackTraceUtils.getStackTraceString(tr.getStackTrace()));
                        result.set(genFailureResult(Objects.requireNonNull(EXCEPTION_MAPPING.getOrDefault(status, UnknownErrorException.class)).getConstructor(Context.class, Throwable.class).newInstance(getApplicationContext(), tr), null));
                    } else {
                        Log.e(TAG, "Failed sending files to " + peer.getDisplayName() + ": " + status);
                        result.set(genFailureResult(Objects.requireNonNull(EXCEPTION_MAPPING.getOrDefault(status, UnknownErrorException.class)).getConstructor(Context.class).newInstance(getApplicationContext()), null));
                    }
                    break;
            }
            finished.set(true);
        });

        // Block until finished
        while (!finished.get()) {
            if (getForegroundInfoAsync().isCancelled()) {
                operation.cancel(true);
                return genFailureResult(new SenderCancelledException(getApplicationContext()), null);
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