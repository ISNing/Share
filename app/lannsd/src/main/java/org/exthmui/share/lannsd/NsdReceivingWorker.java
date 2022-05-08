package org.exthmui.share.lannsd;

import static org.exthmui.share.lannsd.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.lannsd.Constants.COMMAND_CANCEL;
import static org.exthmui.share.lannsd.Constants.COMMAND_FAILURE;
import static org.exthmui.share.lannsd.Constants.COMMAND_REJECT;
import static org.exthmui.share.lannsd.Constants.COMMAND_SUCCESS;
import static org.exthmui.share.lannsd.Constants.FILE_INFO_EXTRA_KEY_MD5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.system.ErrnoException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.lannsd.ssl.SSLUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.ReceivingWorker;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.exceptions.FailedResolvingUriException;
import org.exthmui.share.shared.exceptions.trans.FileIOErrorException;
import org.exthmui.share.shared.exceptions.trans.NetworkErrorException;
import org.exthmui.share.shared.exceptions.trans.NoEnoughSpaceException;
import org.exthmui.share.shared.exceptions.trans.TimedOutException;
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException;
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.FileUtils;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.misc.ReceiverUtils;
import org.exthmui.share.shared.misc.StackTraceUtils;
import org.exthmui.share.shared.misc.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
    private ServerSocket serverSocketToServer = null;

    @SuppressLint({"RestrictedApi", "MissingPermission"})
    @NonNull
    @Override
    public Result doWork() {
        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        Socket socketToClient = null;
        Socket socketToServer = null;
        int timeout = NsdUtils.getTimeout(getApplicationContext());
        int serverPort = NsdUtils.getServerPort(getApplicationContext());
        int bufferSize = NsdUtils.getBufferSize(getApplicationContext());

        SenderInfo senderInfo = null;
        FileInfo[] fileInfos = null;

        final AtomicReference<Boolean> cancelledBySender = new AtomicReference<>(false);
        final SettableFuture<Boolean> isAccepted = SettableFuture.create();

        try {
            // Hold port for request
            serverSocketToServer = new ServerSocket();
//                    SSLUtils.genMutualServerSocket(getApplicationContext());TODO
            serverSocketToServer.setReuseAddress(true);

            serverSocketToServer.bind(new InetSocketAddress(serverPort));
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal(), 0, 0, new FileInfo[]{null,}, null);
            NsdReceiver receiver = NsdReceiver.getInstance(getApplicationContext());
            NsdReceiver.OnListeningPortListener listeningPortListener = receiver.getOnListeningPortListener();
            listeningPortListener.onListening(serverPort);
            socketToServer = serverSocketToServer.accept();// Handled cancel by closing serverSocket in onStopped

            InetAddress clientAddress = socketToServer.getInetAddress();
            Log.d(TAG, "serverSocketToServer connected. From: " + clientAddress.getHostAddress());

            // Read SenderInfo
            inputStream = socketToServer.getInputStream();
            dataInputStream = SSLUtils.getDataInput(socketToServer);
            senderInfo = GSON.fromJson(dataInputStream.readUTF(), SenderInfo.class);

            String fileInfosStr = dataInputStream.readUTF();
            Log.d(TAG, "FileInfo models received: " + fileInfosStr);
            fileInfos = GSON.fromJson(fileInfosStr, FileInfo[].class);
            long totalBytesToReceive = 0;
            for (FileInfo fileInfo: fileInfos) totalBytesToReceive += fileInfo.getFileSize();

            int clientPort = senderInfo.getClientPort();

            // Wait for acceptation from user
            NsdReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionAcceptListener) event -> {
                Log.d(TAG, "User accepted file");
                isAccepted.set(true);
            });
            NsdReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionRejectListener) event -> {
                Log.d(TAG, "User rejected file");
                isAccepted.set(false);
            });
            ReceiverUtils.requestAcceptation(getApplicationContext(), Constants.CONNECTION_CODE_LANNSD, getId().toString(), senderInfo, fileInfos, getId().hashCode());

            // Connect to client
            socketToClient = new Socket();
//                    SSLUtils.genMutualSocket(getApplicationContext());TODO
            socketToClient.bind(null);
            socketToClient.connect((new InetSocketAddress(clientAddress, clientPort)), timeout);
            Log.d(TAG, "socketToClient connected. To: " + clientAddress.getHostAddress() + ":" + clientPort);

            dataOutputStream = SSLUtils.getDataOutput(socketToClient);
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(), totalBytesToReceive, 0, fileInfos, senderInfo);
            if (!isAccepted.get()) {// Will block until accepted or rejected
                dataOutputStream.writeUTF(COMMAND_REJECT);
                Log.d(TAG, "User rejected receiving file");
                Log.d(TAG, "Trying to send \"" + COMMAND_REJECT + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                return genRejectedResult(getApplicationContext(), senderInfo, fileInfos);
            }
            Log.d(TAG, "User accepted receiving file");
            Log.d(TAG, "Trying to send \"" + COMMAND_ACCEPT + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream.writeUTF(COMMAND_ACCEPT);

            // Check if remote cancelled
            if (cancelledBySender.get()) {
                Log.d(TAG, "Remote cancelled receiving file");
                return genSenderCancelledResult(getApplicationContext(), senderInfo, fileInfos);
            }
            // Check if user cancelled
            if (getForegroundInfoAsync().isCancelled()) {
                Log.d(TAG, "User cancelled receiving file");
                // Send cancel command
                Log.d(TAG, "Trying to send command \"" + COMMAND_CANCEL + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                dataOutputStream.writeUTF(COMMAND_CANCEL);
                return genReceiverCancelledResult(getApplicationContext(), senderInfo, fileInfos);
            }

            DocumentFile destinationDirectory = Utils.getDestinationDirectory(getApplicationContext());

            // Receive file
            if (FileUtils.getSpaceAvailable(getApplicationContext(), destinationDirectory) < totalBytesToReceive) {
                return genFailureResult(new NoEnoughSpaceException(getApplicationContext()), senderInfo, fileInfos);
            }
            DocumentFile[] files = new DocumentFile[fileInfos.length];
            for (int i = 0; i < fileInfos.length; i++) {
                FileInfo fileInfo = fileInfos[i];

                String fileName = fileInfo.getFileName();
                if (fileName == null) {
                    fileName = Utils.getDefaultFileName(getApplicationContext());
                }
                DocumentFile file = destinationDirectory.createFile("", fileName);
                files[i] = file;
                if (file == null) {
                    return genFailureResult(new FileIOErrorException("Failed creating file"), senderInfo, fileInfos);
                }
                outputStream = getApplicationContext().getContentResolver().openOutputStream(file.getUri());
                byte[] buf = new byte[bufferSize];
                int len;
                long bytesReceived = 0;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                    bytesReceived += len;
                    updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToReceive, bytesReceived, fileInfos, senderInfo);
                    // Check if remote cancelled
                    if (cancelledBySender.get()) {
                        Log.d(TAG, "Remote cancelled receiving file");
                        return genSenderCancelledResult(getApplicationContext(), senderInfo, fileInfos);
                    }
                    // Check if user cancelled
                    if (getForegroundInfoAsync().isCancelled()) {
                        Log.d(TAG, "User cancelled receiving file");
                        // Send cancel command
                        Log.d(TAG, "Trying to send command \"" + COMMAND_CANCEL + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                        dataOutputStream.writeUTF(COMMAND_CANCEL);
                        // Delete file
                        file.delete();
                        return genReceiverCancelledResult(getApplicationContext(), senderInfo, fileInfos);
                    }
                }
                outputStream.flush();
                outputStream.close();
                outputStream = null;

                if (bytesReceived != fileInfo.getFileSize()) {
                    // Unexpected EOF
                    // Delete file
                    file.delete();
                    return genFailureResult(new NetworkErrorException("Unexpected EOF"), senderInfo, fileInfos);
                }
            }

            // Validate md5
            for (int i = 0; i < fileInfos.length; i++) {
                try {
                    inputStream = getApplicationContext().getContentResolver().openInputStream(files[i].getUri());
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Md5 validation failed");
                    // Send failure command
                    Log.d(TAG, "Trying to send command \"" + COMMAND_FAILURE + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                    dataOutputStream.writeUTF(COMMAND_FAILURE);
                    // Delete file
                    files[i].delete();
                    return genFailureResult(new UnknownErrorException("File validation failed"), senderInfo, fileInfos);
                }
                String md5 = FileUtils.getMD5(inputStream);
                inputStream = null;
                if (!StringUtils.equals(fileInfos[i].getExtra(FILE_INFO_EXTRA_KEY_MD5), md5)) {
                    Log.e(TAG, "Md5 validation failed");
                    // Send failure command
                    Log.d(TAG, "Trying to send command \"" + COMMAND_FAILURE + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                    dataOutputStream.writeUTF(COMMAND_FAILURE);
                    // Delete file
                    files[i].delete();
                    return genFailureResult(new UnknownErrorException("File validation failed"), senderInfo, fileInfos);
                } else
                    Log.d(TAG, "Md5 validation passed: " + fileInfos[i].getFileName());
            }
            // Send receiving result
            Log.d(TAG, "Trying to send command \"" + COMMAND_SUCCESS + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream.writeUTF(COMMAND_SUCCESS);

            List<Entity> entities = new ArrayList<>(fileInfos.length);
            for (DocumentFile file: files) {
                entities.add(new Entity(getApplicationContext(), file.getUri()));
            }
            return genSuccessResult(senderInfo, entities);
        } catch (SocketTimeoutException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new TimedOutException(getApplicationContext(), e), senderInfo, fileInfos);
        } catch (@NonNull ErrnoException | FileNotFoundException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new FileIOErrorException(getApplicationContext(), e), senderInfo, fileInfos);
        } catch (@NonNull IOException | ExecutionException | InterruptedException | FailedResolvingUriException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new UnknownErrorException(getApplicationContext(), e), senderInfo, fileInfos);
//        } catch (@NonNull UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
//            Log.e(TAG, "To Developer: Check your SSL configuration!!!!!!");
//            Log.e(TAG, e.getMessage());
//            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
//            return genFailureResult(new UnknownErrorException(getApplicationContext(), e), senderInfo, fileInfos);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socketToServer != null) {
                try {
                    socketToServer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socketToClient != null) {
                try {
                    socketToClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocketToServer != null) {
                try {
                    serverSocketToServer.close();
                    serverSocketToServer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        getForegroundInfoAsync().cancel(true);//TODO
        if (serverSocketToServer != null) {
            try {
                serverSocketToServer.close();
                serverSocketToServer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
