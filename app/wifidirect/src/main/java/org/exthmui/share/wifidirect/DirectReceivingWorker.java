package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_CANCEL;
import static org.exthmui.share.wifidirect.Constants.COMMAND_FAILURE;
import static org.exthmui.share.wifidirect.Constants.COMMAND_REJECT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_SUCCESS;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER_END;

import android.annotation.SuppressLint;
import android.content.Context;
import android.system.ErrnoException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.FileUtils;
import org.exthmui.share.shared.ReceiverUtils;
import org.exthmui.share.shared.StackTraceUtils;
import org.exthmui.share.shared.Utils;
import org.exthmui.share.shared.base.ReceivingWorker;
import org.exthmui.share.shared.base.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionRejectListener;
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

        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] canceledBySender = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        canceledBySender[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] isAccepted = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        isAccepted[0] = SettableFuture.create();

        try {
            // Hold port for request
            serverSocketToServer = SSLUtils.genMutualServerSocket(getApplicationContext());
            serverSocketToServer.setReuseAddress(true);
            serverSocketToServer.bind(new InetSocketAddress(serverPort));
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal(), 0, 0, (String) null, null);
            socketToServer = serverSocketToServer.accept();// Handled cancel by closing serverSocket in onStopped

            InetAddress clientAddress = socketToServer.getInetAddress();
            Log.d(TAG, "serverSocketToServer connected. From: " + clientAddress.getHostAddress());

            List<FileTransfer> fileTransferList = new ArrayList<>();
            List<String> fileNameList = new ArrayList<>();
            // Read FileTransfer
            inputStream = socketToServer.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            fileTransferList.add((FileTransfer) objectInputStream.readObject());
            fileNameList.add(fileTransferList.get(0).getFileName());
            objectInputStream.close();
            objectInputStream = null;
            inputStream.close();
            inputStream = null;

            long totalBytesToSend = 0;
            trans:
            while (true) {
                // Read FileTransfer
                inputStream = socketToServer.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
                fileTransferList.add(fileTransfer);
                fileNameList.add(fileTransfer.getFileName());
                objectInputStream.close();
                objectInputStream = null;
                inputStream.close();
                inputStream = null;
                Log.d(TAG, "FileTransfer model received: " + fileTransfer);
                totalBytesToSend += fileTransfer.getFileSize();

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
            int clientPort = fileTransferList.get(0).getClientPort();

            // Wait for acceptation from user
            DirectReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionAcceptListener) event -> {
                Log.d(TAG, "User accepted file");
                ((SettableFuture<Boolean>) isAccepted[0]).set(true);
            });
            DirectReceiver.getInstance(getApplicationContext()).registerListener((OnReceiveActionRejectListener) event -> {
                Log.d(TAG, "User rejected file");
                ((SettableFuture<Boolean>) isAccepted[0]).set(false);
            });
            ReceiverUtils.requestAcceptation(getApplicationContext(), Constants.CONNECTION_CODE_WIFIDIRECT, getId().toString(), fileTransferList.get(0).getPeerName(), fileTransferList.get(0).getFileName(), fileTransferList.get(0).getFileSize(), getId().hashCode());

            // Connect to client
            socketToClient = SSLUtils.genMutualSocket(getApplicationContext());
            socketToClient.bind(null);
            socketToClient.connect((new InetSocketAddress(clientAddress, clientPort)), timeout);
            Log.d(TAG, "socketToClient connected. To: " + clientAddress.getHostAddress() + ":" + clientPort);

            Log.d(TAG, "Trying to send \"" + COMMAND_ACCEPT + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
            dataOutputStream = SSLUtils.getDataOutput(socketToClient);
            updateProgress(Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(), fileTransferList.get(0).getFileSize(), 0, (String[]) fileNameList.toArray(), fileTransferList.get(0).getPeerName());
            if (!isAccepted[0].get()) {// Will block until accepted or rejected
                dataOutputStream.writeUTF(COMMAND_REJECT);
                Log.d(TAG, "User rejected receiving file");
                return genRejectedResult();
            }
            dataOutputStream.writeUTF(COMMAND_ACCEPT + "\n");
            dataOutputStream.close();
            dataOutputStream = null;
            Log.d(TAG, "User accepted receiving file");

            // Check if remote cancelled
            if (canceledBySender[0].isDone()) {
                Log.d(TAG, "Remote cancelled receiving file");
                return genSenderCancelledResult();
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
                return genReceiverCancelledResult();
            }

            DocumentFile destinationDirectory = Utils.getDestinationDirectory(getApplicationContext());

            // Receive file
            if (FileUtils.getSpaceAvailable(getApplicationContext(), destinationDirectory) < totalBytesToSend) {
                return genFailureResult(Constants.TransmissionStatus.NO_ENOUGH_SPACE.getNumVal(), "No enough free disk space");
            }
            DocumentFile[] files = new DocumentFile[fileTransferList.size()];
            for (int i = 0; i < fileTransferList.size(); i++) {
                FileTransfer fileTransfer = fileTransferList.get(i);
                inputStream = socketToServer.getInputStream();

                String fileName = fileTransfer.getFileName();
                if (fileName == null) {
                    fileName = Utils.getDefaultFileName(getApplicationContext());
                }
                DocumentFile file = destinationDirectory.createFile("", fileName);
                files[i] = file;
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
                    updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToSend, bytesReceived, (String[]) fileNameList.toArray(), fileTransfer.getFileName());
                    // Check if remote cancelled
                    if (canceledBySender[0].isDone()) {
                        Log.d(TAG, "Remote cancelled receiving file");
                        return genSenderCancelledResult();
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
                        genReceiverCancelledResult();
                    }
                }
                outputStream.close();
                outputStream = null;

                if (bytesReceived != fileTransfer.getFileSize()) {
                    // Unexpected EOF
                    // Delete file
                    file.delete();
                    return genFailureResult(Constants.TransmissionStatus.NETWORK_ERROR.getNumVal(), "Unexpected EOF");
                }
            }

            socketToServer.close();
            socketToServer = null;
            serverSocketToServer.close();
            serverSocketToServer = null;

            // Validate md5
            for (int i = 0; i < fileTransferList.size(); i++) {
                try {
                    inputStream = getApplicationContext().getContentResolver().openInputStream(files[i].getUri());
                } catch (FileNotFoundException e) {
                    return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "Failed calculating MD5");
                }
                String md5 = FileUtils.getMD5(inputStream);
                inputStream = null;
                if (!fileTransferList.get(i).getMd5().equals(md5)) {
                    Log.e(TAG, "Md5 validation failed");
                    // Send failure command
                    Log.d(TAG, "Trying to send command \"" + COMMAND_FAILURE + "\" -> " + clientAddress.getHostAddress() + ":" + clientPort);
                    dataOutputStream = SSLUtils.getDataOutput(socketToClient);
                    dataOutputStream.writeUTF(COMMAND_FAILURE + "\n");
                    dataOutputStream.close();
                    dataOutputStream = null;
                    // Delete file
                    files[i].delete();
                    return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "File validation failed");
                } else
                    Log.d(TAG, "Md5 validation passed: " + fileTransferList.get(i).getFileName());
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
            return genFailureResult(Constants.TransmissionStatus.TIMED_OUT.getNumVal(), e.getMessage());
        } catch (ErrnoException | FileNotFoundException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(Constants.TransmissionStatus.FILE_IO_ERROR.getNumVal(), e.getMessage());
        } catch (IOException | ExecutionException | InterruptedException | ClassNotFoundException e) {
            Log.i(TAG, e.getMessage());
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), e.getMessage());
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "To Developer: Check your SSL configuration!!!!!!");
            Log.e(TAG, e.getMessage());
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), e.getMessage());
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
