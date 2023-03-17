package org.exthmui.share.lannsd;

import static org.exthmui.share.lannsd.Constants.SHARE_PROTOCOL_VERSION_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.exthmui.share.lannsd.exceptions.FailedResolvingPeerException;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.base.send.ReceiverInfo;
import org.exthmui.share.shared.base.send.SendingTask;
import org.exthmui.share.shared.exceptions.trans.FileIOErrorException;
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.shared.exceptions.trans.PeerDisappearedException;
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.RejectedException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.Utils;
import org.exthmui.share.taskMgr.Result;
import org.exthmui.share.taskMgr.entities.TaskEntity;
import org.exthmui.share.udptransport.UDPSender;
import org.exthmui.utils.StackTraceUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class NsdSendingTask extends SendingTask {

    public static final String TAG = "NsdSendingTask";

    public NsdSendingTask(@NonNull Context context, Bundle inputData) {
        super(context, inputData);
    }

    public NsdSendingTask(@NonNull TaskEntity taskEntity) {
        super(taskEntity);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Result doWork(Entity[] entities, String peerId, String peerName) {
        FileInfo[] fileInfos = new FileInfo[entities.length];
        for (int i = 0; i < entities.length; i++) {
            if (entities[i] == null)
                return genFailureResult(new InvalidInputDataException(getApplicationContext()), null);
            fileInfos[i] = new FileInfo(entities[i]);
            if (NsdUtils.isMd5ValidationEnabled(getApplicationContext()))
                try {
                    entities[i].calculateMD5(getApplicationContext());
                    fileInfos[i].putExtra(org.exthmui.share.udptransport.Constants.FILE_INFO_EXTRA_KEY_MD5, entities[i].getMD5());
                } catch (IOException e) {
                    Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
                    return genFailureResult(new FileIOErrorException(getApplicationContext(), e), null);
                }
            else Log.d(TAG,
                    String.format("Md5 Validation is disabled, skipping generating md5 for %s(%s)",
                            entities[i].getFileName(), fileInfos[i].getId()));
        }

        // Load Peer
        NsdManager manager = NsdManager.getInstance(getApplicationContext());

        final NsdPeer[] peer = {(NsdPeer) manager.getPeers().get(peerId)};
        if (peer[0] == null)
            return genFailureResult(new PeerDisappearedException(getApplicationContext()), null);

        if (!peer[0].isAttributesLoaded()) {
            final boolean[] succeeded = new boolean[1];
            CountDownLatch latch = new CountDownLatch(1);
            NsdUtils.resolvePeer(getApplicationContext(), peer[0], new NsdUtils.ResolveListener() {
                @Override
                public void onResolveFailed(NsdPeer p, int errorCode) {
                    succeeded[0] = false;
                    peer[0] = p;
                    latch.countDown();
                }

                @Override
                public void onServiceResolved(NsdPeer p) {
                    succeeded[0] = true;
                    peer[0] = p;
                    latch.countDown();
                }
            });
            try {
                latch.await();
                if (!succeeded[0])
                    return genFailureResult(new FailedResolvingPeerException(getApplicationContext()), null);
            } catch (InterruptedException e) {
                return genFailureResult(new FailedResolvingPeerException(getApplicationContext(), e), null);
            }
        }
        // End Load Peer

        // Initialize ReceiverInfo
        ReceiverInfo receiverInfo = new ReceiverInfo(peer[0], peer[0].getServerPort());

        AtomicReference<Result> result = new AtomicReference<>(null);

        int timeout = NsdUtils.getTimeout(getApplicationContext());
        int serverPort = peer[0].getServerPort();

        // Initial SenderInfo object
        SenderInfo senderInfo = new SenderInfo();
        senderInfo.setDisplayName(Utils.getSelfName(getApplicationContext()));
        senderInfo.setId(Utils.getSelfId(getApplicationContext()));
        senderInfo.setProtocolVersion(SHARE_PROTOCOL_VERSION_1);
        senderInfo.setUid(0);//TODO: Get from account sdk
        senderInfo.setAccountServerSign("");//TODO: Get from account sdk

        UDPSender sender = new UDPSender(getApplicationContext(), new UDPSender.SendingListener() {
            @Override
            public void onAccepted(String[] fileIdsAccepted) {

            }

            @Override
            public void onProgressUpdate(int status, long totalBytesToSend, long bytesSent,
                                         String curFileId, long curFileBytesToSend, long curFileBytesSent) {
                updateProgress(status, totalBytesToSend, bytesSent, fileInfos, receiverInfo,
                        curFileId, curFileBytesToSend, curFileBytesSent, totalBytesToSend == 0);
            }

            @Override
            public void onComplete(int status, Map<String, Pair<Integer, String>> resultMap) {
                if ((status | Constants.TransmissionStatus.COMPLETED.getNumVal()) == status)
                    result.set(genSuccessResult());
                if ((status | Constants.TransmissionStatus.REJECTED.getNumVal()) == status)
                    result.set(genFailureResult(new RejectedException(getApplicationContext()), null));
                else if ((status | Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal()) == status)
                    result.set(genFailureResult(new SenderCancelledException(getApplicationContext()), null));
                else if ((status | Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal()) == status)
                    result.set(genFailureResult(new ReceiverCancelledException(getApplicationContext()), null));
                else if ((status | Constants.TransmissionStatus.ERROR.getNumVal()) == status)
                    result.set(genFailureResult(new UnknownErrorException(getApplicationContext()), null));//TODO:Improvement required
            }
        });
        try {
            sender.initialize();
        } catch (SocketException e) {
            Log.e(TAG, String.format("Error occurred while sending: %s(message: %s)", e, e.getMessage()));
            Log.w(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
        }
        sender.sendAsync(entities, fileInfos, senderInfo, new InetSocketAddress(peer[0].getAddress(), serverPort));

        while (result.get() == null) {
            // Check if user cancelled
            if (isCancelled()) {
                sender.cancel();
            }
        }
        return result.get();
    }
}
