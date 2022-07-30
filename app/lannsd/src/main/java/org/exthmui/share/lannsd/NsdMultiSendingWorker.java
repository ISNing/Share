package org.exthmui.share.lannsd;

import static org.exthmui.share.lannsd.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.lannsd.Constants.COMMAND_CANCEL;
import static org.exthmui.share.lannsd.Constants.COMMAND_FAILURE;
import static org.exthmui.share.lannsd.Constants.COMMAND_REJECT;
import static org.exthmui.share.lannsd.Constants.COMMAND_SUCCESS;
import static org.exthmui.share.lannsd.Constants.FILE_INFO_EXTRA_KEY_MD5;
import static org.exthmui.share.lannsd.Constants.SHARE_PROTOCOL_VERSION_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.exthmui.share.lannsd.exceptions.FailedResolvingPeerException;
import org.exthmui.share.lannsd.ssl.SSLUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.base.send.ReceiverInfo;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.base.send.SendingWorker;
import org.exthmui.share.shared.exceptions.FailedResolvingUriException;
import org.exthmui.share.shared.exceptions.trans.FileIOErrorException;
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.shared.exceptions.trans.PeerDisappearedException;
import org.exthmui.share.shared.exceptions.trans.RemoteErrorException;
import org.exthmui.share.shared.exceptions.trans.TimedOutException;
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.misc.StackTraceUtils;
import org.exthmui.share.shared.misc.Utils;
import org.exthmui.share.udptransport.UDPSender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NsdMultiSendingWorker extends SendingWorker {

    public static final String TAG = "NsdMultiSendingWorker";

    private static final Gson GSON = new Gson();

    public NsdMultiSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @SuppressLint("RestrictedApi")
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
        Entity[] entities = new Entity[uris.length];

        for (int i = 0; i < uriStrings.length; i++) {
            if (uriStrings[i] == null)
                return genFailureResult(new InvalidInputDataException(getApplicationContext()));
            uris[i] = Uri.parse(uriStrings[i]);
            try {
                entities[i] = new Entity(getApplicationContext(), uris[i]);
            } catch (FailedResolvingUriException e) {
                return genFailureResult(new FileIOErrorException(getApplicationContext(), e));
            }
            fileInfos[i] = new FileInfo();
            fileInfos[i].setFileName(fileNames[i]);
            fileInfos[i].setFileSize(fileSizes[i]);
            try {
                entities[i].calculateMD5(getApplicationContext());
            } catch (IOException e) {
                Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
                return genFailureResult(new FileIOErrorException(getApplicationContext(), e));
            }
            fileInfos[i].putExtra(FILE_INFO_EXTRA_KEY_MD5, entities[i].getMD5());
        }

        // Load Peer
        NsdManager manager = NsdManager.getInstance(getApplicationContext());

        String peerId = input.getString(Sender.TARGET_PEER_ID);
        final NsdPeer[] peer = {(NsdPeer) manager.getPeers().get(peerId)};
        if (peer[0] == null)
            return genFailureResult(new PeerDisappearedException(getApplicationContext()));

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
                    return genFailureResult(new FailedResolvingPeerException(getApplicationContext()));
            } catch (InterruptedException e) {
                return genFailureResult(new FailedResolvingPeerException(getApplicationContext(), e));
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
            public void onProgressUpdate(int status, long totalBytesToSend, long bytesSent, long curFileBytesToSend, long curFileBytesSent, String curFileId) {
                updateProgress(status, totalBytesToSend, bytesSent, fileInfos, receiverInfo);//TODO:Improvement required
            }

            @Override
            public void onComplete(int status, Map<String, Pair<Integer, String>> resultMap) {
                if ((status & Constants.TransmissionStatus.COMPLETED.getNumVal()) == status)
                    result.set(genSuccessResult());
                else if ((status & Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal()) == status)
                    result.set(genSenderCancelledResult(getApplicationContext()));
                else if ((status & Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal()) == status)
                    result.set(genReceiverCancelledResult(getApplicationContext()));
                else if ((status & Constants.TransmissionStatus.ERROR.getNumVal()) == status)
                    result.set(genFailureResult(new UnknownErrorException(getApplicationContext())));
            }
        });
        try {
            sender.initialize();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        sender.sendAsync(entities, fileInfos, senderInfo, new InetSocketAddress(peer[0].getAddress(), peer[0].getServerPort()));

        while (result.get() == null) {
            // Check if user cancelled
            if (getForegroundInfoAsync().isCancelled()) {
                sender.cancel();
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