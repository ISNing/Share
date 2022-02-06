package org.exthmui.share.wifidirect;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.ReceiverUtils;
import org.exthmui.share.shared.Utils;
import org.exthmui.share.shared.base.ReceivingWorker;
import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionRejectListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class DirectReceivingWorker extends ReceivingWorker {
    private static final String TAG = "DirectReceivingWorker";
    public DirectReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        InputStream inputStream;
        ObjectInputStream objectInputStream;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        Socket socket = null;
        ServerSocket serverSocket;
        File file = null;
        FileOutputStream fileOutputStream;
        int timeout = DirectUtils.getTimeout(getApplicationContext());
        int serverPort = DirectUtils.getServerPort(getApplicationContext());
        int bufferSize = DirectUtils.getBufferSize(getApplicationContext());
        try {
            // Hold port for request
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(serverPort));
            Socket client = serverSocket.accept();
            updateProgress(0, 0, null, null);

            InetAddress clientIP = client.getInetAddress();
            Log.d(TAG, "Client connected. IP: " + clientIP.getHostAddress());
            updateProgress(0, 0, null, "IP: " + clientIP.getHostAddress());

            // Read FileTransfer
            inputStream = client.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
            updateProgress(fileTransfer.getFileSize(), 0, fileTransfer.getFileName(), fileTransfer.getPeerName());
            int clientPort = fileTransfer.getClientPort();

            Log.d(TAG, "FileTransfer model received: " + fileTransfer);

            final boolean[] accepted = {false};

            {
                CountDownLatch latch = new CountDownLatch(1);
                DirectReceiver.getInstance(getApplicationContext()).registerListener(new OnReceiveActionAcceptListener() {
                    @Override
                    public void onReceiveActionAccept(ReceiveActionAcceptEvent event) {
                        Log.d(TAG, "User accepted file");
                        accepted[0] = true;
                        latch.countDown();
                    }
                });
                DirectReceiver.getInstance(getApplicationContext()).registerListener(new OnReceiveActionRejectListener() {
                    @Override
                    public void onReceiveActionReject(ReceiveActionRejectEvent event) {
                        Log.d(TAG, "User rejected file");
                        accepted[0] = false;
                        latch.countDown();
                    }
                });

                ReceiverUtils.requestAcceptation(getApplicationContext(), Constants.CONNECTION_CODE_WIFIDIRECT, getId().toString(), fileTransfer.getPeerName(), fileTransfer.getFileName(), fileTransfer.getFileSize());
                latch.await();
            }
            AcceptedOrRejectedResponse response = new AcceptedOrRejectedResponse(accepted[0]);

            socket = new Socket();
            socket.bind(null);

            Log.d(TAG, "Trying to connect to client to sent " + response + " -> " + clientIP.getHostAddress() + ":"+ clientPort);
            socket.connect((new InetSocketAddress(clientIP, clientPort)), timeout);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(response);

            if (!accepted[0]) {
                serverSocket.close();
                updateProgress(0, 0, null, null);
                return genFailureResult(Constants.TransmissionStatus.REJECTED.getNumVal(), "Remote system rejected receiving file");
            }

            // Receive file
            if (Utils.getDestinationDirectory(getApplicationContext()).getTotalSpace() < fileTransfer.getFileSize()) {
                return genFailureResult(Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal(), "No enough free disk space");
            }
            file = new File(Utils.getDestinationDirectory(getApplicationContext()).getPath() + "/" + fileTransfer.getFileName());
            fileOutputStream = new FileOutputStream(file);
            byte[] buf = new byte[bufferSize];
            int len;
            long bytesReceived = 0;
            while ((len = inputStream.read(buf)) != -1) {
                fileOutputStream.write(buf, 0, len);
                bytesReceived += len;
                updateProgress(fileTransfer.getFileSize(), bytesReceived, fileTransfer.getFileName(), fileTransfer.getFileName());
            }
            serverSocket.close();
            inputStream.close();
            objectInputStream.close();
            fileOutputStream.close();
            serverSocket = null;
            inputStream = null;
            objectInputStream = null;
            fileOutputStream = null;
            // TODO: validate MD5
        } catch (Exception e) {
            Log.e(TAG, "文件接收 Exception: " + e.getMessage());
        }
        return Result.failure();
    }
}
