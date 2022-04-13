package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_CANCEL;
import static org.exthmui.share.wifidirect.Constants.COMMAND_FAILURE;
import static org.exthmui.share.wifidirect.Constants.COMMAND_REJECT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_SUCCESS;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER_END;
import static org.exthmui.share.wifidirect.Constants.FILE_INFO_EXTRA_KEY_MD5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.system.ErrnoException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.ReceivingWorker;
import org.exthmui.share.shared.base.receive.SenderInfo;
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
import org.exthmui.share.wifidirect.ssl.SSLUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DirectReceivingWorker extends ReceivingWorker {

    public static final String TAG = "DirectReceivingWorker";

    public DirectReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    private ServerSocket serverSocketToServer = null;

    @SuppressLint({"RestrictedApi", "MissingPermission"})
    @NonNull
    @Override
    public Result doWork() {
        InputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        DataInputStream dataInputStream = null;
        BufferedReader bufferedReader = null;
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        Socket socketToClient = null;
        Socket socketToServer = null;
        int timeout = DirectUtils.getTimeout(getApplicationContext());
        int serverPort = DirectUtils.getServerPort(getApplicationContext());
        int bufferSize = DirectUtils.getBufferSize(getApplicationContext());

        SenderInfo senderInfo = null;
        FileInfo[] fileInfos = null;

        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] cancelledBySender = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        cancelledBySender[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] isAccepted = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        isAccepted[0] = SettableFuture.create();

        try {
            // Hold port for request
            serverSocketToServer = SSLUtils.genMutualServerSocket(getApplicationContext());
            serverSocketToServer.setReuseAddress(true);
            serverSocketToServer.bind(new InetSocketAddress(serverPort));
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal(), 0, 0, new FileInfo[]{null,}, null);
            socketToServer = serverSocketToServer.accept();// Handled cancel by closing serverSocket in onStopped

            InetAddress clientAddress = socketToServer.getInetAddress();
            Log.d(TAG, "serverSocketToServer connected. From: " + clientAddress.getHostAddress());

            List<FileInfo> fileInfoList = new ArrayList<>();
            // Read SenderInfo
            inputStream = socketToServer.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            senderInfo = (SenderInfo) objectInputStream.readObject();
            objectInputStream.close();
            objectInputStream = null;
            inputStream.close();
            inputStream = null;

            long totalBytesToReceive = 0;
            trans:
            while (true) {
                // Read FileTransfer
                inputStream = socketToServer.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                FileInfo fileInfo = (FileInfo) objectInputStream.readObject();
                fileInfoList.add(fileInfo);
                objectInputStream.close();
                objectInputStream = null;
                inputStream.close();
                inputStream = null;
                Log.d(TAG, "FileTransfer model received: " + fileInfo);
                totalBytesToReceive += fileInfo.getFileSize();

                dataInputStream = SSLUtils.getDataInput(socketToServer);
                socketToServer.setSoTimeout(0);

                bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
                String command;
                while ((command = bufferedReader.readLine()) != null) {
                    switch (command) {
                        case COMMAND_TRANSFER:// Transfer command, get ready for receiving next FileTransfer model
                            Log.d(TAG, String.format("Received transfer command \"%s\"", command));
                            continue trans;
                        case COMMAND_TRANSFER_END:// Transfer End command, no more model will be sent
                            Log.d(TAG, String.format("Received transfer end command \"%s\"", command));
                            break trans;
                        default:
                            Log.w(TAG, String.format("Received unknown command \"%s\"", command));
                    }
                }
            }
            int clientPort = senderInfo.getClientPort();
            fileInfos = fileInfoList.toArray(new FileInfo[0]);

            // Wait for acceptation from user
            DirectReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionAcceptListener) event -> {
                Log.d(TAG, "User accepted file");
                ((SettableFuture<Boolean>) isAccepted[0]).set(true);
            });
            DirectReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionRejectListener) event -> {
                Log.d(TAG, "User rejected file");
                ((SettableFuture<Boolean>) isAccepted[0]).set(false);
            });
            ReceiverUtils.requestAcceptation(getApplicationContext(), Constants.CONNECTION_CODE_WIFIDIRECT, getId().toString(), senderInfo, fileInfos, getId().hashCode());

            // Connect to client
            socketToClient = SSLUtils.genMutualSocket(getApplicationContext());
            socketToClient.bind(null);
            socketToClient.connect((new InetSocketAddress(clientAddress, clientPort)), timeout);
            Log.d(TAG, "socketToClient connected. To: " + clientAddress.getHostAddress() + ":" + clientPort);

            Log.d(TAG, "Trying to send \"" + COMMAND_ACCEPT + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream = SSLUtils.getDataOutput(socketToClient);
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(), totalBytesToReceive, 0, fileInfos, senderInfo);
            if (!isAccepted[0].get()) {// Will block until accepted or rejected
                dataOutputStream.writeUTF(COMMAND_REJECT);
                Log.d(TAG, "User rejected receiving file");
                return genRejectedResult(getApplicationContext(), senderInfo, fileInfos);
            }
            dataOutputStream.writeUTF(COMMAND_ACCEPT + "\n");
            dataOutputStream.close();
            dataOutputStream = null;
            Log.d(TAG, "User accepted receiving file");

            // Check if remote cancelled
            if (cancelledBySender[0].isDone()) {
                Log.d(TAG, "Remote cancelled receiving file");
                return genSenderCancelledResult(getApplicationContext(), senderInfo, fileInfos);
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
                return genReceiverCancelledResult(getApplicationContext(), senderInfo, fileInfos);
            }

            DocumentFile destinationDirectory = Utils.getDestinationDirectory(getApplicationContext());

            // Receive file
            if (FileUtils.getSpaceAvailable(getApplicationContext(), destinationDirectory) < totalBytesToReceive) {
                return genFailureResult(new NoEnoughSpaceException(getApplicationContext()), senderInfo, fileInfos);
            }
            DocumentFile[] files = new DocumentFile[fileInfoList.size()];
            for (int i = 0; i < fileInfoList.size(); i++) {
                FileInfo fileInfo = fileInfoList.get(i);
                inputStream = socketToServer.getInputStream();

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
                while ((len = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                    bytesReceived += len;
                    updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToReceive, bytesReceived, fileInfos, senderInfo);
                    // Check if remote cancelled
                    if (cancelledBySender[0].isDone()) {
                        Log.d(TAG, "Remote cancelled receiving file");
                        return genSenderCancelledResult(getApplicationContext(), senderInfo, fileInfos);
                    }
                    // Check if user cancelled
                    if (getForegroundInfoAsync().isCancelled()) {
                        Log.d(TAG, "User cancelled receiving file");
                        // Send cancel command
                        Log.d(TAG, "Trying to send command \"" + COMMAND_CANCEL + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                        dataOutputStream = SSLUtils.getDataOutput(socketToClient);
                        dataOutputStream.writeUTF(COMMAND_CANCEL + "\n");
                        dataOutputStream.close();
                        dataOutputStream = null;
                        // Delete file
                        file.delete();
                        return genReceiverCancelledResult(getApplicationContext(), senderInfo, fileInfos);
                    }
                }
                outputStream.close();
                outputStream = null;

                if (bytesReceived != fileInfo.getFileSize()) {
                    // Unexpected EOF
                    // Delete file
                    file.delete();
                    return genFailureResult(new NetworkErrorException("Unexpected EOF"), senderInfo, fileInfos);
                }
            }

            socketToServer.close();
            socketToServer = null;
            serverSocketToServer.close();
            serverSocketToServer = null;

            // Validate md5
            for (int i = 0; i < fileInfoList.size(); i++) {
                try {
                    inputStream = getApplicationContext().getContentResolver().openInputStream(files[i].getUri());
                } catch (FileNotFoundException e) {
                    return genFailureResult(new UnknownErrorException("Failed calculating MD5"), senderInfo, fileInfos);
                }
                String md5 = FileUtils.getMD5(inputStream);
                inputStream = null;
                if (!StringUtils.equals(fileInfoList.get(i).getExtra(FILE_INFO_EXTRA_KEY_MD5), md5)) {
                    Log.e(TAG, "Md5 validation failed");
                    // Send failure command
                    Log.d(TAG, "Trying to send command \"" + COMMAND_FAILURE + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                    dataOutputStream = SSLUtils.getDataOutput(socketToClient);
                    dataOutputStream.writeUTF(COMMAND_FAILURE + "\n");
                    dataOutputStream.close();
                    dataOutputStream = null;
                    // Delete file
                    files[i].delete();
                    return genFailureResult(new UnknownErrorException("File validation failed"), senderInfo, fileInfos);
                } else
                    Log.d(TAG, "Md5 validation passed: " + fileInfoList.get(i).getFileName());
            }
            // Send receiving result
            Log.d(TAG, "Trying to send command \"" + COMMAND_SUCCESS + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream = SSLUtils.getDataOutput(socketToClient);
            dataOutputStream.writeUTF(COMMAND_SUCCESS + "\n");
            dataOutputStream.close();
            dataOutputStream = null;
            // Close socketToClient
            socketToClient.close();
            socketToClient = null;
        } catch (SocketTimeoutException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new TimedOutException(getApplicationContext(), e), senderInfo, fileInfos);
        } catch (ErrnoException | FileNotFoundException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new FileIOErrorException(getApplicationContext(), e), senderInfo, fileInfos);
        } catch (IOException | ExecutionException | InterruptedException | ClassNotFoundException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new UnknownErrorException(getApplicationContext(), e), senderInfo, fileInfos);
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "To Developer: Check your SSL configuration!!!!!!");
            Log.e(TAG, e.getMessage());
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new UnknownErrorException(getApplicationContext(), e), senderInfo, fileInfos);
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
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
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
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
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
