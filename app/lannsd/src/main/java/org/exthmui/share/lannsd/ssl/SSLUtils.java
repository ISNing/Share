package org.exthmui.share.lannsd.ssl;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public abstract class SSLUtils {
    @NonNull
    public static SSLServerSocket genMutualServerSocket(@NonNull Context context) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return SSLUtils.genSSLServerSocket(Auth.getSSLContextServer(context, true), true);
    }

    public static SSLServerSocket genSSLServerSocket(@NonNull SSLContext sslContext, boolean mutual) throws IOException {
        SSLServerSocketFactory sslServerSocketFactory;
        SSLServerSocket sslServerSocket;
        sslServerSocketFactory = sslContext.getServerSocketFactory();
        //Just create a TCP connection.SSL shake hand does not begin.
        //The first time either side(server or client) try to get a socket input stream
        //or output stream will case the SSL shake hand begin.
        sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket();
        String[] pwdSuits = sslServerSocket.getSupportedCipherSuites();
        sslServerSocket.setEnabledCipherSuites(pwdSuits);
        //Use client mode.Must prove its identity to the client side.
        //Client mode is the default mode.
        sslServerSocket.setUseClientMode(false);
        if(mutual){
            //The communication will stop if the client side doesn't show its identity.
            sslServerSocket.setNeedClientAuth(true);
        }else{
            //The communication will go on although the client side doesn't show its identity.
            sslServerSocket.setWantClientAuth(true);
        }
        return sslServerSocket;
    }

    @NonNull
    public static SSLSocket genMutualSocket(@NonNull Context context) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return SSLUtils.genSSLSocket(Auth.getSSLContextClient(context, true), true);
    }

    @NonNull
    public static SSLSocket genSSLSocket(@NonNull SSLContext sslContext, boolean mutual) throws IOException {
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket();
        String[] pwdSuits = socket.getSupportedCipherSuites();
        socket.setEnabledCipherSuites(pwdSuits);
        if(mutual){
            socket.setUseClientMode(false);
            socket.setNeedClientAuth(true);
        }else{
            socket.setUseClientMode(true);
            socket.setWantClientAuth(true);
        }
        return socket;
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