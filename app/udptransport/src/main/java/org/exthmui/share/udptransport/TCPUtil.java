package org.exthmui.share.udptransport;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TCPUtil {
    public static final String SUB_TAG = "TCPUtil";

    private static final Gson GSON = new Gson();

    public static final String PREFIX_COMMAND = "CMD_";
    public static final String PREFIX_JSON = "JSON_";
    public static final String PREFIX_BARE = "BARE_";

    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            2, 1L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));
    public final List<String> cmdBlocked = new ArrayList<>();
    public final List<String> jsonBlocked = new ArrayList<>();
    public final List<String> bareBlocked = new ArrayList<>();
    @NonNull
    private final Socket socket;
    public String TAG = SUB_TAG;
    @Nullable
    private DataInputStream in;
    @Nullable
    private DataOutputStream out;

    @NonNull
    public CompletableFuture<String> cmdReceived = new CompletableFuture<>();
    @NonNull
    public CompletableFuture<String> jsonReceived = new CompletableFuture<>();
    @NonNull
    public CompletableFuture<String> bareReceived = new CompletableFuture<>();

    @Nullable
    private CommandListener commandListener;

    private boolean stringWatcherStopFlag = true;

    private final Runnable stringWatcher = () -> {
        String str;
        while (!stringWatcherStopFlag) {
            try {
                str = in.readUTF();
                if (str.startsWith(PREFIX_COMMAND))
                    if (commandListener == null) if (!cmdReceived.isDone())
                        cmdReceived.complete(str.replaceFirst(PREFIX_COMMAND, ""));
                    else cmdBlocked.add(str.replaceFirst(PREFIX_COMMAND, ""));
                    else {
                        if (cmdReceived.isDone()) {
                            try {
                                commandListener.onReceiveCommand(cmdReceived.get().replaceFirst(PREFIX_COMMAND, ""));
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!cmdBlocked.isEmpty()) for (String cmd: cmdBlocked)
                            commandListener.onReceiveCommand(cmd.replaceFirst(PREFIX_COMMAND, ""));
                        commandListener.onReceiveCommand(str.replaceFirst(PREFIX_COMMAND, ""));
                    }
                if (str.startsWith(PREFIX_JSON)) if (!jsonReceived.isDone())
                    jsonReceived.complete(str.replaceFirst(PREFIX_JSON, ""));
                else jsonBlocked.add(str.replaceFirst(PREFIX_JSON, ""));
                if (str.startsWith(PREFIX_BARE)) if (!bareReceived.isDone())
                    bareReceived.complete(str.replaceFirst(PREFIX_BARE, ""));
                else bareBlocked.add(str.replaceFirst(PREFIX_BARE, ""));
            } catch (EOFException ignored) {
            } catch (SocketException e) {
                // Ignore socket closing
                if (!Objects.equals(e.getMessage(), "Socket closed"))
                    e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public TCPUtil() throws SocketException {
        this(new Socket());
        socket.setTcpNoDelay(true);
        socket.setReuseAddress(true);
    }

    public TCPUtil(@NonNull Socket socket) {
        this.socket = socket;
    }

    public void connect(SocketAddress tcpAddress) throws IOException {
        Log.d(TAG, "Trying to connect to receiver under tcp socket: " + tcpAddress);
        socket.connect(tcpAddress);
        initialize();
    }

    public void initialize() throws IOException {
        in = Utils.getDataInput(socket);
        out = Utils.getDataOutput(socket);
        stringWatcherStopFlag = false;
        threadPool.execute(stringWatcher);
    }

    public void writeCommand(String command) throws IOException {
        assert out != null;
        Log.d(TAG, "Trying to send COMMAND \"" + command + "\" -> " + socket.getInetAddress());
        out.writeUTF(PREFIX_COMMAND + command);
    }

    public void writeJson(Object object) throws IOException {
        String jsonStr = GSON.toJson(object);
        Log.d(TAG, "Trying to send JSON \"" + jsonStr + "\" -> " + socket.getInetAddress());
        assert out != null;
        out.writeUTF(PREFIX_JSON + jsonStr);
    }

    public void writeBare(String s) throws IOException {
        assert out != null;
        Log.d(TAG, "Trying to send BARE \"" + s + "\" -> " + socket.getInetAddress());
        out.writeUTF(PREFIX_BARE + s);
    }

    public String readCommand() {
        String str = null;
        do {
            try {
                str = cmdReceived.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (!cmdReceived.isDone()) cmdReceived.cancel(true);
                cmdReceived = new CompletableFuture<>();
            }
            if (!cmdBlocked.isEmpty()) {
                cmdReceived.complete(cmdBlocked.get(0));
                cmdBlocked.remove(0);
            }
        } while (str == null);
        return str;
    }

    public <T> T readJson(Class<T> classOfT) {
        String json = null;
        do {
            try {
                json = jsonReceived.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (!jsonReceived.isDone()) jsonReceived.cancel(true);
                jsonReceived = new CompletableFuture<>();
            }
            if (!jsonBlocked.isEmpty()) {
                jsonReceived.complete(jsonBlocked.get(0));
                jsonBlocked.remove(0);
            }
        } while (json == null);
        return GSON.fromJson(json, classOfT);
    }

    public <T> T readJson(Class<T> classOfT, Type typeOfT) throws IOException {
        String json = null;
        do {
            try {
                json = jsonReceived.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (!jsonReceived.isDone()) jsonReceived.cancel(true);
                jsonReceived = new CompletableFuture<>();
            }
            if (!jsonBlocked.isEmpty()) {
                jsonReceived.complete(jsonBlocked.get(0));
                jsonBlocked.remove(0);
            }
        } while (json == null);
        return GSON.fromJson(json, typeOfT);
    }

    public String readBare() {
        String str = null;
        do {
            try {
                str = bareReceived.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (!bareReceived.isDone()) bareReceived.cancel(true);
                bareReceived = new CompletableFuture<>();
            }
            if (!bareBlocked.isEmpty()) {
                bareReceived.complete(bareBlocked.get(0));
                bareBlocked.remove(0);
            }
        } while (str == null);
        return str;
    }

    public void releaseResources() {
        Utils.silentClose(in);
        in = null;
        Utils.silentClose(out);
        out = null;
        Utils.silentClose(socket);
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public void setCommandListener(@Nullable CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public void setTAG(String TAG) {
        this.TAG = String.format("%s/%s", TAG, SUB_TAG);
    }

    public interface CommandListener {
        void onReceiveCommand(String cmd);
    }
}
