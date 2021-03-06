package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_CANCEL;
import static org.exthmui.share.wifidirect.Constants.COMMAND_FAILURE;
import static org.exthmui.share.wifidirect.Constants.COMMAND_REJECT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_SUCCESS;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER_END;
import static org.exthmui.share.wifidirect.Constants.FILE_INFO_EXTRA_KEY_MD5;
import static org.exthmui.share.wifidirect.Constants.SHARE_PROTOCOL_VERSION_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

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
import org.exthmui.share.wifidirect.ssl.SSLUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
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
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class DirectMultiSendingWorker extends SendingWorker {

    public static final String TAG = "DirectSendingWorker";

    public DirectMultiSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @Nullable
    final AtomicReference<ServerSocket> serverSocketToClientReference = new AtomicReference<>(null);

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

        long totalBytesToSend = 0;
        long bytesSent = 0;

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
            totalBytesToSend += fileInfos[i].getFileSize();
        }

        DirectManager manager = DirectManager.getInstance(getApplicationContext());

        String peerId = input.getString(Sender.TARGET_PEER_ID);
        DirectPeer peer = (DirectPeer) manager.getPeers().get(peerId);
        if (peer == null)
            return genFailureResult(new PeerDisappearedException(getApplicationContext()));

        ReceiverInfo receiverInfo = new ReceiverInfo(peer, peer.getServerPort());

        // Connect target device
        {
            CountDownLatch latch = new CountDownLatch(1);

            DirectUtils.connectPeer(getApplicationContext(), peer, latch);
            updateProgress(Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(), 0, 0, fileInfos, receiverInfo);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Request for server ip
        final InetAddress[] serverAddress = new InetAddress[1];
        {
            CountDownLatch latch = new CountDownLatch(1);

            manager.getWifiP2pManager().requestConnectionInfo(manager.getChannel(), wifiP2pInfo -> {
                serverAddress[0] = wifiP2pInfo.groupOwnerAddress;
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        AtomicReference<Result> result = new AtomicReference<>(null);

        int timeout = DirectUtils.getTimeout(getApplicationContext());
        int serverPort = peer.getServerPort();
        int clientPort = DirectUtils.getClientPort(getApplicationContext());
        int bufferSize = DirectUtils.getBufferSize(getApplicationContext());

        InputStream inputStream = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        Socket socketToServer = null;
        DataOutputStream dataOutputStream = null;
        final AtomicReference<Socket> socketToClientReference = new AtomicReference<>(null);

        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] cancelledByReceiver = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        cancelledByReceiver[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] isAccepted = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        isAccepted[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] succeeded = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        succeeded[0] = SettableFuture.create();

        Thread senderServerThread = new Thread() {
            @Nullable
            DataInputStream dataInputStream = null;
            @Nullable
            BufferedReader bufferedReader = null;

            @SuppressLint("RestrictedApi")
            @Override
            public void run() {
                // Read commands
                try {
                    dataInputStream = SSLUtils.getDataInput(socketToClientReference.get());
                    socketToClientReference.get().setSoTimeout(0);

                    bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
                    String command;
                    label:
                    while ((command = bufferedReader.readLine()) != null) {
                        switch (command) {
                            case COMMAND_CANCEL:
                                Log.d(TAG, String.format("Received cancel command \"%s\"", command));
                                ((SettableFuture<Boolean>) cancelledByReceiver[0]).set(true);
                                break label;
                            case COMMAND_ACCEPT:
                                Log.d(TAG, String.format("Received  command \"%s\"", command));
                                ((SettableFuture<Boolean>) isAccepted[0]).set(true);
                                break;
                            case COMMAND_REJECT:
                                Log.d(TAG, String.format("Received command \"%s\"", command));
                                ((SettableFuture<Boolean>) isAccepted[0]).set(false);
                                break;
                            case COMMAND_SUCCESS:
                                Log.d(TAG, String.format("Received command \"%s\"", command));
                                ((SettableFuture<Boolean>) succeeded[0]).set(true);
                                break;
                            case COMMAND_FAILURE:
                                Log.d(TAG, String.format("Received command \"%s\"", command));
                                ((SettableFuture<Boolean>) succeeded[0]).set(false);
                                break;
                            default:
                                Log.w(TAG, String.format("Received unknown command \"%s\"", command));
                        }
                        if (interrupted()) break;
                    }
                    bufferedReader.close();
                    bufferedReader = null;
                    dataInputStream.close();
                    dataInputStream = null;
                    socketToClientReference.get().close();
                    socketToClientReference.set(null);
                    serverSocketToClientReference.get().close();
                    serverSocketToClientReference.set(null);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                            bufferedReader = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                            dataInputStream = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (socketToClientReference.get() != null) {
                        try {
                            socketToClientReference.get().close();
                            socketToClientReference.set(null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (serverSocketToClientReference.get() != null) {
                        try {
                            serverSocketToClientReference.get().close();
                            serverSocketToClientReference.set(null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        try {
            // Initial SenderInfo object
            SenderInfo senderInfo = new SenderInfo();
            senderInfo.setDisplayName(Utils.getSelfName(getApplicationContext()));
            senderInfo.setId(Utils.getSelfId(getApplicationContext()));
            senderInfo.setProtocolVersion(SHARE_PROTOCOL_VERSION_1);
            senderInfo.setUid(0);//TODO: Get from account sdk
            senderInfo.setAccountServerSign("");//TODO: Get from account sdk
            senderInfo.setClientPort(clientPort);

            socketToServer = SSLUtils.genMutualSocket(getApplicationContext());
            socketToServer.bind(null);
            Log.d(TAG, "Trying to connect to server(receiver): " + serverAddress[0].getHostAddress() + ":" + serverPort);
            socketToServer.connect((new InetSocketAddress(serverAddress[0], serverPort)), timeout);

            // Send senderInfo
            Log.d(TAG, "Trying to send " + senderInfo + " -> " + serverAddress[0].getHostAddress() + ":" + serverPort);
            outputStream = socketToServer.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(senderInfo);
            objectOutputStream.close();
            objectOutputStream = null;
            outputStream.close();
            outputStream = null;

            // Send fileTransfer
            Log.d(TAG, "Trying to send " + fileInfos[0] + " -> " + serverAddress[0].getHostAddress() + ":" + serverPort);
            outputStream = socketToServer.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileInfos[0]);
            objectOutputStream.close();
            objectOutputStream = null;
            outputStream.close();
            outputStream = null;

            for (FileInfo fileInfo : fileInfos) {
                Log.d(TAG, "Trying to send command \"" + COMMAND_TRANSFER + "\" -> " + serverAddress[0].getHostAddress() + ":" + clientPort);
                dataOutputStream = SSLUtils.getDataOutput(socketToServer);
                dataOutputStream.writeUTF(COMMAND_TRANSFER + "\n");
                dataOutputStream.close();
                dataOutputStream = null;

                // Send fileTransfer
                Log.d(TAG, "Trying to send " + fileInfo + " -> " + serverAddress[0].getHostAddress() + ":" + serverPort);
                outputStream = socketToServer.getOutputStream();
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(fileInfo);
                objectOutputStream.close();
                objectOutputStream = null;
                outputStream.close();
                outputStream = null;
            }

            Log.d(TAG, "Trying to send command \"" + COMMAND_TRANSFER_END + "\" -> " + serverAddress[0].getHostAddress() + ":" + clientPort);
            dataOutputStream = SSLUtils.getDataOutput(socketToServer);
            dataOutputStream.writeUTF(COMMAND_TRANSFER_END + "\n");
            dataOutputStream.close();
            dataOutputStream = null;

            // Start sender server
            byte[] oriAddress = serverAddress[0].getAddress();
            byte[] address = null;
            // Bind ServerSocket
            serverSocketToClientReference.set(SSLUtils.genMutualServerSocket(getApplicationContext()));
            serverSocketToClientReference.get().setReuseAddress(true);
            serverSocketToClientReference.get().bind(new InetSocketAddress(clientPort));
            Log.d(TAG, "ServerSocketToClient successfully bound to port: " + clientPort);
            do {
                if (socketToClientReference.get() != null) {
                    socketToClientReference.get().close();// Disconnect if connected
                    socketToClientReference.set(null);
                }
                // Set timeout TODO: check timeout working
                serverSocketToClientReference.get().setSoTimeout(timeout);
                try {
                    socketToClientReference.set(serverSocketToClientReference.get().accept());
                } catch (SocketTimeoutException e) {
                    return genFailureResult(new TimedOutException(getApplicationContext(), e));
                }
                if (socketToClientReference.get() == null) continue;
                address = socketToClientReference.get().getInetAddress().getAddress();
                // Ensure the connection is from identical address
            } while (!Arrays.equals(oriAddress, address));
            // Connection established
            serverSocketToClientReference.get().setSoTimeout(0);
            updateProgress(Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(), totalBytesToSend, bytesSent, fileInfos, receiverInfo);

            // Monitor command sending
            senderServerThread.start();

            // Read ACCEPT or REJECT command
            // Re-initialize acceptation status
            isAccepted[0] = SettableFuture.create();

            updateProgress(Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(), totalBytesToSend, bytesSent, fileInfos, receiverInfo);
            // Will block until accepted or rejected
            if (!isAccepted[0].get()) {
                Log.d(TAG, "User rejected receiving file");
                return genRejectedResult(getApplicationContext());
            }
            Log.d(TAG, "User accepted receiving file");

            // Check if remote cancelled
            if (cancelledByReceiver[0].isDone()) {
                Log.d(TAG, "Remote cancelled receiving file");
                return genReceiverCancelledResult(getApplicationContext());
            }
            // Check if user cancelled
            if (getForegroundInfoAsync().isCancelled()) {
                Log.d(TAG, "User cancelled receiving file");
                // Send cancel command
                Log.d(TAG, "Trying to send command \"" + COMMAND_CANCEL + "\" -> " + serverAddress[0].getHostAddress() + ":" + clientPort);
                dataOutputStream = SSLUtils.getDataOutput(socketToServer);
                dataOutputStream.writeUTF(COMMAND_CANCEL + "\n");
                dataOutputStream.close();
                dataOutputStream = null;
                return genSenderCancelledResult(getApplicationContext());
            }

            for (Entity entity : entities) {
                // Start send file
                updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToSend, bytesSent, fileInfos, receiverInfo);
                outputStream = socketToServer.getOutputStream();
                inputStream = entity.getInputStream(getApplicationContext());
                if (inputStream == null)
                    return genFailureResult(new FileIOErrorException(String.format("Failed opening file: %s: Got null InputStream", entity.getFileName())));
                byte[] buf = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                    bytesSent += len;
                    updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToSend, bytesSent, fileInfos, receiverInfo);

                    // Check if remote cancelled
                    if (cancelledByReceiver[0].isDone())
                        return genReceiverCancelledResult(getApplicationContext());
                    // Check if user cancelled
                    if (getForegroundInfoAsync().isCancelled())
                        return genSenderCancelledResult(getApplicationContext());
                }
                inputStream.close();
                inputStream = null;
                outputStream.close();
                outputStream = null;
            }

            socketToServer.close();
            socketToServer = null;

            // Read SUCCESS or FAILURE command
            // Re-initialize success status
            succeeded[0] = SettableFuture.create();

            if (!succeeded[0].get()) {// Will block until accepted or rejected
                Log.d(TAG, "User failed receiving file");
                return genFailureResult(new RemoteErrorException(getApplicationContext()));
            }
            Log.d(TAG, "User succeeded receiving file");

            // Close streams & sockets
            // Close socketToClient
            socketToClientReference.get().close();
            socketToClientReference.set(null);
            // Close serverSocketToClient
            serverSocketToClientReference.get().close();
            serverSocketToClientReference.set(null);

            updateProgress(Constants.TransmissionStatus.COMPLETED.getNumVal(), totalBytesToSend, bytesSent, fileInfos, receiverInfo);
            result.set(Result.success(getInputData()));
            return result.get();
        } catch (SocketTimeoutException e) {
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new TimedOutException(getApplicationContext(), e));
        } catch (@NonNull IOException | ExecutionException | InterruptedException e) {
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new UnknownErrorException(getApplicationContext(), e));
        } catch (@NonNull UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "To Developer: Check your SSL configuration!!!!!!");
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(new UnknownErrorException(getApplicationContext(), e));
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
            if (socketToServer != null) {
                try {
                    socketToServer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socketToClientReference.get() != null) {
                try {
                    socketToClientReference.get().close();
                    socketToClientReference.set(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocketToClientReference.get() != null) {
                try {
                    serverSocketToClientReference.get().close();
                    serverSocketToClientReference.set(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        getForegroundInfoAsync().cancel(true);
        if (serverSocketToClientReference.get() != null) {
            try {
                serverSocketToClientReference.get().close();
                serverSocketToClientReference.set(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}