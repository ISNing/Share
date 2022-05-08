package org.exthmui.share.udptransport;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public abstract class Utils {
    public static void silentClose(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException ignored) {
        }
    }

    @NonNull
    public static DataInputStream getDataInput(@NonNull Socket socket) throws IOException{
        return new DataInputStream(socket.getInputStream());
    }

    @NonNull
    public static DataOutputStream getDataOutput(@NonNull Socket socket) throws IOException{
        return new DataOutputStream(socket.getOutputStream());
    }

}
