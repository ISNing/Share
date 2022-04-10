package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.COMMAND_ACCEPT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_CANCEL;
import static org.exthmui.share.wifidirect.Constants.COMMAND_FAILURE;
import static org.exthmui.share.wifidirect.Constants.COMMAND_REJECT;
import static org.exthmui.share.wifidirect.Constants.COMMAND_SUCCESS;
import static org.exthmui.share.wifidirect.Constants.COMMAND_TRANSFER_END;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.StackTraceUtils;
import org.exthmui.share.shared.Utils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.SendingWorker;
import org.exthmui.share.shared.base.exceptions.FailedResolvingUriException;
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

public class DirectSendingWorker extends SendingWorker {

    public static final String TAG = "DirectSendingWorker";

    public DirectSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    final AtomicReference<ServerSocket> serverSocketToClientReference = new AtomicReference<>(null);

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Result doWork() {
        Data input = getInputData();
        Uri uri = Uri.parse(input.getString(Entity.FILE_URI));

        DirectManager manager = DirectManager.getInstance(getApplicationContext());

        Entity entity;
        try {
            entity = new Entity(getApplicationContext(), uri);
        } catch (FailedResolvingUriException e) {
            return genFailureResult(Constants.TransmissionStatus.FILE_IO_ERROR.getNumVal(), e.getMessage());
        }
        String peerId = input.getString(Sender.TARGET_PEER_ID);
        DirectPeer peer = (DirectPeer) manager.getPeers().get(peerId);
        if (peer == null)
            return genFailureResult(Constants.TransmissionStatus.PEER_DISAPPEARED.getNumVal(), "Could not get a valid Peer object by id:" + peerId);

        // Connect target device
        {
            CountDownLatch latch = new CountDownLatch(1);

            DirectUtils.connectPeer(getApplicationContext(), peer, latch);
            updateProgress(Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(), 0, 0, entity.getFileName(), peer.getDisplayName());
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

        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] canceledByReceiver = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        canceledByReceiver[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] isAccepted = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        isAccepted[0] = SettableFuture.create();
        @SuppressWarnings("unchecked") final ListenableFuture<Boolean>[] succeeded = (ListenableFuture<Boolean>[]) new ListenableFuture<?>[1];
        succeeded[0] = SettableFuture.create();

        Thread senderServerThread = new Thread() {
            DataInputStream inputStream = null;
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;

            @SuppressLint("RestrictedApi")
            @Override
            public void run() {
                // Read commands
                try {
                    inputStream = SSLUtils.getDataInput(socketToClientReference.get());
                    socketToClientReference.get().setSoTimeout(0);

                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String command;
                    label:
                    while ((command = bufferedReader.readLine()) != null) {
                        switch (command) {
                            case COMMAND_CANCEL:
                                Log.d(TAG, String.format("Received cancel command \"%s\"", command));
                                ((SettableFuture<Boolean>) canceledByReceiver[0]).set(true);
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
                    inputStreamReader.close();
                    inputStreamReader = null;
                    inputStream.close();
                    inputStream = null;
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
                    if (inputStreamReader != null) {
                        try {
                            inputStreamReader.close();
                            inputStreamReader = null;
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
            // Initial fileTransfer object
            entity.calculateMD5(getApplicationContext());
            FileTransfer fileTransfer = new FileTransfer();
            fileTransfer.setFileName(entity.getFileName());
            fileTransfer.setFileSize(entity.getFileSize());
            fileTransfer.setPeerName(Utils.getSelfName(getApplicationContext()));
            fileTransfer.setPeerId(Utils.getSelfId(getApplicationContext()));
            fileTransfer.setMd5(fileTransfer.getMd5());
            fileTransfer.setClientPort(clientPort);

            long totalBytesToSend = fileTransfer.getFileSize();
            long bytesSent = 0;

            socketToServer = SSLUtils.genMutualSocket(getApplicationContext());
            socketToServer.bind(null);
            Log.d(TAG, "Trying to connect to server(receiver): " + serverAddress[0].getHostAddress() + ":" + serverPort);
            socketToServer.connect((new InetSocketAddress(serverAddress[0], serverPort)), timeout);

            // Send fileTransfer
            Log.d(TAG, "Trying to send " + fileTransfer + " -> " + serverAddress[0].getHostAddress() + ":" + serverPort);
            outputStream = socketToServer.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer);
            objectOutputStream.close();
            objectOutputStream = null;

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
                serverSocketToClientReference.get().setSoTimeout(timeout);// Set timeout TODO: check timeout working
                try {
                    socketToClientReference.set(serverSocketToClientReference.get().accept());
                } catch (SocketTimeoutException e){
                    return genFailureResult(Constants.TransmissionStatus.TIMED_OUT.getNumVal(), "Connection timeout(1)");
                }
                if (socketToClientReference.get() == null) continue;
                address = socketToClientReference.get().getInetAddress().getAddress();
            } while (!Arrays.equals(oriAddress, address)); // Ensure the connection is from identical address
            // Connection established
            serverSocketToClientReference.get().setSoTimeout(0);
            updateProgress(Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(), totalBytesToSend, bytesSent, entity.getFileName(), peer.getDisplayName());

            // Monitor command sending
            senderServerThread.start();

            // Read ACCEPT or REJECT command
            isAccepted[0] = SettableFuture.create();// Re-initialize acceptation status

            updateProgress(Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(), totalBytesToSend, bytesSent, entity.getFileName(), peer.getDisplayName());
            if (!isAccepted[0].get()) {// Will block until accepted or rejected
                Log.d(TAG, "User rejected receiving file");
                return genFailureResult(Constants.TransmissionStatus.REJECTED.getNumVal(), "Remote system rejected receiving file");
            }
            Log.d(TAG, "User accepted receiving file");

            // Check if remote cancelled
            if (canceledByReceiver[0].isDone()) {
                Log.d(TAG, "Remote cancelled receiving file");
                return genFailureResult(Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), "Remote system(aka receiver) canceled receiving file");
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
                return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "User(aka sender) canceled sending file");
            }

            // Start send file
            updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToSend, bytesSent, entity.getFileName(), peer.getDisplayName());
            inputStream = entity.getInputStream(getApplicationContext());
            if (inputStream == null)
                return genFailureResult(Constants.TransmissionStatus.FILE_IO_ERROR.getNumVal(), String.format("Failed opening file: %s: Got null InputStream", entity.getFileName()));
            byte[] buf = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                bytesSent += len;
                updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToSend, bytesSent, entity.getFileName(), peer.getDisplayName());

                // Check if remote cancelled
                if (canceledByReceiver[0].isDone())
                    return genFailureResult(Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), "Remote system(aka receiver) canceled receiving file");
                // Check if user cancelled
                if (getForegroundInfoAsync().isCancelled())
                    return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "User(aka sender) canceled sending file");
            }
            inputStream.close();
            inputStream = null;
            outputStream.close();
            outputStream = null;
            socketToServer.close();
            socketToServer = null;

            // Read SUCCESS or FAILURE command
            succeeded[0] = SettableFuture.create();// Re-initialize success status

            if (!succeeded[0].get()) {// Will block until accepted or rejected
                Log.d(TAG, "User failed receiving file");
                return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "Remote system failed receiving file");
            }
            Log.d(TAG, "User succeeded receiving file");

            // Close streams & sockets
            // Close socketToClient
            socketToClientReference.get().close();
            socketToClientReference.set(null);
            // Close serverSocketToClient
            serverSocketToClientReference.get().close();
            serverSocketToClientReference.set(null);

            updateProgress(Constants.TransmissionStatus.COMPLETED.getNumVal(), totalBytesToSend, bytesSent, entity.getFileName(), peer.getDisplayName());
            result.set(Result.success(getInputData()));
            return result.get();
        } catch (SocketTimeoutException e) {
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(Constants.TransmissionStatus.TIMED_OUT.getNumVal(), e.getMessage());
        } catch (IOException | ExecutionException | InterruptedException e) {
            Log.i(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), e.getMessage());
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "To Developer: Check your SSL configuration!!!!!!");
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), e.getMessage());
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