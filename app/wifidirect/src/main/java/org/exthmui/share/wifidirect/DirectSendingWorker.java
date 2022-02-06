package org.exthmui.share.wifidirect;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.SendingWorker;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.exceptions.FailedResolvingUriException;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class DirectSendingWorker extends SendingWorker {

    private static final String TAG = "DirectSendingWorker";

    public static final String I_SERVER_IP = "SERVER_IP";

    public DirectSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private void connect(DirectPeer peer, CountDownLatch latch) {
        WifiP2pDevice targetDevice = peer.getWifiP2pDevice();
        DirectManager manager = DirectManager.getInstance(getApplicationContext());
        WifiP2pManager wifiP2pManager = manager.getWifiP2pManager();
        WifiP2pManager.Channel channel = manager.getChannel();
        WifiP2pConfig config = new WifiP2pConfig();
        if (config.deviceAddress != null && targetDevice != null) {
            config.deviceAddress = targetDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            try {
                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        peer.notifyPeerUpdated();
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    @SuppressWarnings("WrongThread")
    public Result doWork() {
        Data input = getInputData();
        Uri uri = Uri.parse(input.getString(Entity.FILE_URI));

        DirectManager manager = DirectManager.getInstance(getApplicationContext());

        Entity entity;
        try {
            entity = new Entity(getApplicationContext(), uri);
        } catch (FailedResolvingUriException e) {
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), e.getMessage());
        }
        String peerId = input.getString(Sender.TARGET_PEER_ID);
        DirectPeer peer = (DirectPeer) manager.getPeers().get(peerId);
        if(peer == null) return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "Could not get a valid Peer object by id:" + peerId);
        updateProgress(Constants.TransmissionStatus.UNKNOWN.getNumVal(), 0, 0, entity.getFileName(), peer.getDisplayName());

        // connect target device
        {
            CountDownLatch latch = new CountDownLatch(1);

            connect(peer, latch);//TODO: handle if already connected
            updateProgress(Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(), 0, 0, entity.getFileName(), peer.getDisplayName());
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // request for server ip
        final InetAddress[] serverIP = new InetAddress[1];
        {
            CountDownLatch latch = new CountDownLatch(1);

            manager.getWifiP2pManager().requestConnectionInfo(manager.getChannel(), new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                    serverIP[0] = wifiP2pInfo.groupOwnerAddress;
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        AtomicReference<Result> result = new AtomicReference<>(null);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int timeout = DirectUtils.getTimeout(getApplicationContext());
        int serverPort = DirectUtils.getServerPort(getApplicationContext());
        int clientPort = DirectUtils.getClientPort(getApplicationContext());
        int bufferSize = DirectUtils.getBufferSize(getApplicationContext());

        // Initial fileTransfer object
        entity.calculateMD5(getApplicationContext());
        FileTransfer fileTransfer = new FileTransfer();
        fileTransfer.setFileName(entity.getFileName());
        fileTransfer.setFileSize(entity.getFileSize());
        fileTransfer.setPeerName(/* TODO:getSelfName */"ISNing's Phone");
        fileTransfer.setMd5(fileTransfer.getMd5());
        fileTransfer.setClientPort(clientPort);

        InputStream inputStream = null;
        ObjectInputStream objectInputStream;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        Socket socket = null;
        ServerSocket serverSocket;
        try {
            socket = new Socket();
            socket.bind(null);

            // Send fileTransfer
            Log.d(TAG, "Trying to connect to client to sent " + fileTransfer + " -> " + serverIP[0].getHostAddress() + ":"+ serverPort);
            socket.connect((new InetSocketAddress(serverIP[0], serverPort)), timeout);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer);

            // Wait for acceptation
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(clientPort));
            Socket client = serverSocket.accept();

            // Read AcceptedOrRejectedResponse
            inputStream = client.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            AcceptedOrRejectedResponse response = (AcceptedOrRejectedResponse) objectInputStream.readObject();
            if (!response.isAccepted()) {
                Log.d(TAG, "Received "+ response +", User rejected receiving file");
                return genFailureResult(Constants.TransmissionStatus.REJECTED.getNumVal(), "Remote system rejected receiving file");
            }
            // Start send file
            inputStream = entity.getInputStream(getApplicationContext());
            if (inputStream == null) return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "Failed reading file");
            long totalBytesToSend = fileTransfer.getFileSize();
            long bytesSent = 0;
            byte[] buf = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                bytesSent += len;
                updateProgress(Constants.TransmissionStatus.IN_PROGRESS.getNumVal(), totalBytesToSend, bytesSent, entity.getFileName(), peer.getDisplayName());
            }
            outputStream.close();
            objectOutputStream.close();
            inputStream.close();
            socket.close();
            outputStream = null;
            objectOutputStream = null;
            inputStream = null;
            socket = null;
        } catch (Exception e) {
            return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
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
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // TODO: wait for success response
        result.set(Result.success(getInputData()));
        return result.get();
    }
}