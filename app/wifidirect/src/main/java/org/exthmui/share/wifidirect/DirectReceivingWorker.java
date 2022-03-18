package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_CANCEL;
import static org.exthmui.share.wifidirect.Constants.COMMAND_FAILURE;
import static org.exthmui.share.wifidirect.Constants.COMMAND_REJECT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_SUCCESS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.FileUtils;
import org.exthmui.share.shared.ReceiverUtils;
import org.exthmui.share.shared.Utils;
import org.exthmui.share.shared.base.ReceivingWorker;
import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.wifidirect.ssl.SSLUtils;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DirectReceivingWorker extends ReceivingWorker {

    private static final String TAG = "DirectReceivingWorker";

    public DirectReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private ServerSocket serverSocketToServer = null;

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Result doWork() {

        InputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        Socket socketToClient = null;
        Socket socketToServer = null;
        int timeout = DirectUtils.getTimeout(getApplicationContext());
        int serverPort = DirectUtils.getServerPort(getApplicationContext());
        int bufferSize = DirectUtils.getBufferSize(getApplicationContext());

        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] canceledBySender = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        canceledBySender[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] isAccepted = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        isAccepted[0] = SettableFuture.create();

        try {
            // Hold port for request
            serverSocketToServer = SSLUtils.genMutualServerSocket(getApplicationContext());
            serverSocketToServer.setReuseAddress(true);
            serverSocketToServer.bind(new InetSocketAddress(serverPort));
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal(), 0, 0, null, null);
            socketToServer = serverSocketToServer.accept();// Handled cancel by closing serverSocket in onStopped

            InetAddress clientAddress = socketToServer.getInetAddress();
            Log.d(TAG, "Client connected. IP: " + clientAddress.getHostAddress());

            // Read FileTransfer
            inputStream = socketToServer.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
            int clientPort = fileTransfer.getClientPort();

            Log.d(TAG, "FileTransfer model received: " + fileTransfer);

            // Wait for acceptation from user
            DirectReceiver.getInstance(getApplicationContext()).registerListener(new OnReceiveActionAcceptListener() {
                @Override
                public void onReceiveActionAccept(ReceiveActionAcceptEvent event) {
                    Log.d(TAG, "User accepted file");
                    ((SettableFuture<Boolean>) isAccepted[0]).set(true);
                }
            });
            DirectReceiver.getInstance(getApplicationContext()).registerListener(new OnReceiveActionRejectListener() {
                @Override
                public void onReceiveActionReject(ReceiveActionRejectEvent event) {
                    Log.d(TAG, "User rejected file");
                    ((SettableFuture<Boolean>) isAccepted[0]).set(false);
                }
            });
            ReceiverUtils.requestAcceptation(getApplicationContext(), Constants.CONNECTION_CODE_WIFIDIRECT, getId().toString(), fileTransfer.getPeerName(), fileTransfer.getFileName(), fileTransfer.getFileSize());

            // For sending acceptation result
            socketToClient = SSLUtils.genMutualSocket(getApplicationContext());
            socketToClient.bind(null);

            Log.d(TAG, "Trying to send \"" + COMMAND_ACCEPT + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream = SSLUtils.getDataOutput(socketToClient);
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(), fileTransfer.getFileSize(), 0, fileTransfer.getFileName(), fileTransfer.getPeerName());
            if (!isAccepted[0].get()) {// Will block until accepted or rejected
                dataOutputStream.writeUTF(COMMAND_REJECT);
                Log.d(TAG, "User rejected receiving file");
                return genFailureResult(Constants.TransmissionStatus.REJECTED.getNumVal(), "Remote system rejected receiving file");
            }
            dataOutputStream.writeUTF(COMMAND_ACCEPT + "\n");
            dataOutputStream.close();
            dataOutputStream = null;
            Log.d(TAG, "User accepted receiving file");

            // Check if remote cancelled
            if (canceledBySender[0].isDone()) {
                Log.d(TAG, "Remote cancelled receiving file");
                return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "Remote system(aka receiver) canceled receiving file");
            }
            // Check if user cancelled
            if (getForegroundInfoAsync().isCancelled()) {
                Log.d(TAG, "User cancelled receiving file");
                // Send cancel command
                Log.d(TAG, "Trying to send command \""+ COMMAND_CANCEL + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                dataOutputStream = SSLUtils.getDataOutput(socketToClient);
                dataOutputStream.writeUTF(COMMAND_CANCEL + "\n");
                dataOutputStream.close();
                dataOutputStream = null;
                return genFailureResult(Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), "User(aka sender) canceled sending file");
            }

            DocumentFile destinationDirectory = Utils.getDestinationDirectory(getApplicationContext());

            // Receive file
            if (FileUtils.getSpaceAvailable(getApplicationContext(), destinationDirectory) < fileTransfer.getFileSize()) {
                return genFailureResult(Constants.TransmissionStatus.NO_ENOUGH_SPACE.getNumVal(), "No enough free disk space");
            }
            String fileName = fileTransfer.getFileName();
            if (fileName == null) {
                fileName = Utils.getDefaultFileName(getApplicationContext());
            }
            DocumentFile file = destinationDirectory.createFile("", fileName);
            if (file == null) {
                return genFailureResult(Constants.TransmissionStatus.FILE_IO_ERROR.getNumVal(), "Failed creating file");
            }
            outputStream = getApplicationContext().getContentResolver().openOutputStream(file.getUri());
            byte[] buf = new byte[bufferSize];
            int len;
            long bytesReceived = 0;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                bytesReceived += len;
                updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(),fileTransfer.getFileSize(), bytesReceived, fileTransfer.getFileName(), fileTransfer.getFileName());
                // Check if remote cancelled
                if (canceledBySender[0].isDone()) {
                    Log.d(TAG, "Remote cancelled receiving file");
                    return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "Remote system(aka receiver) canceled receiving file");
                }
                // Check if user cancelled
                if (getForegroundInfoAsync().isCancelled()) {
                    Log.d(TAG, "User cancelled receiving file");
                    // Send cancel command
                    Log.d(TAG, "Trying to send command \""+ COMMAND_CANCEL + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                    dataOutputStream = SSLUtils.getDataOutput(socketToClient);
                    dataOutputStream.writeUTF(COMMAND_CANCEL + "\n");
                    dataOutputStream.close();
                    dataOutputStream = null;
                    // Delete file
                    file.delete();
                    return genFailureResult(Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), "User(aka sender) canceled sending file");
                }
            }

            objectInputStream.close();
            objectInputStream = null;
            inputStream.close();
            inputStream = null;
            socketToServer.close();
            socketToServer = null;
            serverSocketToServer.close();
            serverSocketToServer = null;
            outputStream.close();
            outputStream = null;

            // Validate md5
            try {
                inputStream = getApplicationContext().getContentResolver().openInputStream(file.getUri());
            } catch (FileNotFoundException e) {
                return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "Failed calculating MD5");
            }
            String md5 = FileUtils.getMD5(inputStream);
            inputStream = null;
            if (!fileTransfer.getMd5().equals(md5)) {
                Log.e(TAG, "Md5 validation failed");
                // Send failure command
                Log.d(TAG, "Trying to send command \""+ COMMAND_FAILURE + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                dataOutputStream = SSLUtils.getDataOutput(socketToClient);
                dataOutputStream.writeUTF(COMMAND_FAILURE + "\n");
                dataOutputStream.close();
                dataOutputStream = null;
                // Delete file
                file.delete();
                return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "File validation failed");
            } else Log.d(TAG, "Md5 validation passed");

            // Send receiving result
            Log.d(TAG, "Trying to send command \""+ COMMAND_SUCCESS + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream = SSLUtils.getDataOutput(socketToClient);
            dataOutputStream.writeUTF(COMMAND_SUCCESS + "\n");
            dataOutputStream.close();
            dataOutputStream = null;
            // Close socketToClient
            socketToClient.close();
            socketToClient = null;
        } catch (Exception ex) {
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), ex.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                    outputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                    dataOutputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                    objectInputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socketToServer != null) {
                try {
                    socketToServer.close();
                    socketToServer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socketToClient != null) {
                try {
                    socketToClient.close();
                    socketToClient = null;
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
        return Result.failure();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        getForegroundInfoAsync().cancel(true);
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
