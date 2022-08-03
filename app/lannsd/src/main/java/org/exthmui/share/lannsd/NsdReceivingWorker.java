package org.exthmui.share.lannsd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.gson.Gson;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.ReceivingWorker;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.misc.ReceiverUtils;
import org.exthmui.share.udptransport.UDPReceiver;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class NsdReceivingWorker extends ReceivingWorker {

    public static final String TAG = "NsdReceivingWorker";

    private static final Gson GSON = new Gson();

    public NsdReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @Nullable
    private final ServerSocket serverSocketToServer = null;

    @NonNull
    @Override
    public Result doWork() {
        int timeout = NsdUtils.getTimeout(getApplicationContext());

        final AtomicReference<Boolean> cancelledBySender = new AtomicReference<>(false);
        final AtomicReference<Boolean> completed = new AtomicReference<>(false);
        final AtomicReference<TransmissionResult> generalResult = new AtomicReference<>(null);
        final AtomicReference<Map<String, TransmissionResult>> resultMap = new AtomicReference<>(null);
        @SuppressLint("RestrictedApi") final SettableFuture<Boolean> isAccepted = SettableFuture.create();

        Data input = getInputData();
        byte connId = input.getByte(org.exthmui.share.lannsd.Constants.WORKER_INPUT_KEY_CONN_ID, (byte) 0);
        if (connId == 0)
            return genFailureResult(new InvalidInputDataException(getApplicationContext()), null);
        NsdReceiver receiver = NsdReceiver.getInstance(getApplicationContext());
        UDPReceiver udpReceiver = receiver.getUdpReceiver();
        if (udpReceiver == null) return genSilentResult();
        UDPReceiver.ConnectionHandler handler = udpReceiver.getHandler(connId);

        if (handler == null)
            return genFailureResult(new InvalidInputDataException(getApplicationContext()), null);

        handler.setListener(new UDPReceiver.ReceivingListener() {
            @Override
            public void requestAcceptationAsync(SenderInfo senderInfo, FileInfo[] fileInfos, CompletableFuture<Set<String>> idsAccepted) {
                // Wait for acceptation from user
                NsdReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionAcceptListener) event -> {
                    Log.d(TAG, "User accepted file");
                    Set<String> ids = new HashSet<>();
                    for (FileInfo fileInfo : fileInfos) {
                        ids.add(fileInfo.getId());
                    }
                    idsAccepted.complete(ids);
                });
                NsdReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionRejectListener) event -> {
                    Log.d(TAG, "User rejected file");
                    idsAccepted.complete(Collections.emptySet());
                });
                ReceiverUtils.requestAcceptation(getApplicationContext(),
                        Constants.CONNECTION_CODE_LANNSD, getId().toString(), senderInfo,
                        fileInfos, getId().hashCode());
            }

            @Override
            public void onProgressUpdate(int status, long totalBytesToSend, long bytesReceived,
                                         @NonNull SenderInfo senderInfo,
                                         @NonNull FileInfo[] fileInfos, String curFileId,
                                         long curFileBytesToSend, long curFileBytesReceived) {
                updateProgress(status, totalBytesToSend, bytesReceived, fileInfos, senderInfo,
                        curFileId, curFileBytesToSend, curFileBytesReceived);
            }

            @Override
            public void onComplete(TransmissionResult result, Map<String, TransmissionResult> r) {
                generalResult.set(result);
                resultMap.set(r);
                completed.set(true);
            }
        });

        while (!completed.get()) {
            // Check if user cancelled
            if (getForegroundInfoAsync().isCancelled()) {
                Log.d(TAG, "User cancelled receiving file");
                handler.cancel();
            }
        }

        if (generalResult.get() == null || generalResult.get().getStatus() != Constants.TransmissionStatus.COMPLETED) {
            return genFailureResult((TransmissionException) generalResult.get(), handler.getSenderInfo(), handler.getFileInfos(),
                    resultMap.get());
        }

        NsdReceiver nsdReceiver = NsdReceiver.getInstance(getApplicationContext());

        List<Entity> entities = new ArrayList<>(handler.getFileInfos().length);
        for (FileInfo fileInfo : handler.getFileInfos()) {
            entities.add(nsdReceiver.getEntity(fileInfo.getId()));
        }
        return genSuccessResult(handler.getSenderInfo(), entities);
    }

    @Override
    public void onStopped() {
        super.onStopped();
        getForegroundInfoAsync().cancel(true);//TODO
    }
}
