package org.exthmui.share.wifidirect.ssl;

import android.content.Context;

import org.exthmui.share.wifidirect.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class Auth {
    public static SSLContext getSSLContextClient(Context context, boolean mutual) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        InputStream inputStreamSSLConf = context.getResources().openRawResource(R.raw.sslconf);
        Properties properties = new Properties();
        properties.load(inputStreamSSLConf);
        String storeType = properties.getProperty("storeType");
        String algorithm = properties.getProperty("algorithm");
        String protocol = properties.getProperty("protocol");
        String sCertificatePwd = properties.getProperty("serverCertificatePwd");
        String sMainPwd = properties.getProperty("serverMainPwd");
        String cCertificatePwd = properties.getProperty("clientCertificatePwd");
        String cMainPwd = properties.getProperty("clientMainPwd");

        //KeyStore class is used to save certificate.
        char[] c_pwd = sCertificatePwd.toCharArray();
        KeyStore keyStore = KeyStore.getInstance(storeType);
        InputStream inputStreamServerCert = context.getResources().openRawResource(R.raw.server_rsa);
        keyStore.load(inputStreamServerCert, c_pwd);
        inputStreamServerCert.close();

        //TrustManagerFactory class is used to create TrustManager class.
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
        trustManagerFactory.init(keyStore);
        //TrustManager class is used to decide weather to trust the certificate or not.
        TrustManager[] tms = trustManagerFactory.getTrustManagers();

        KeyManager[] kms = null;
        if (mutual) {
            //KeyStore class is used to save certificate.
            c_pwd = cCertificatePwd.toCharArray();
            keyStore = KeyStore.getInstance(storeType);
            InputStream inputStreamClientCert = context.getResources().openRawResource(R.raw.client_rsa);
            keyStore.load(inputStreamClientCert, c_pwd);
            inputStreamClientCert.close();

            //KeyManagerFactory class is used to create KeyManager class.
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
            char[] m_pwd = cMainPwd.toCharArray();
            keyManagerFactory.init(keyStore, m_pwd);
            //KeyManager class is used to choose a certificate to prove the identity of the client side.
            kms = keyManagerFactory.getKeyManagers();
        }

        //SSLContext class is used to set all the properties about secure communication.
        //Such as protocol type and so on.
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(kms, tms, null);

        return sslContext;
    }

    public static SSLContext getSSLContextServer(Context context, boolean mutual) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        InputStream inputStreamSSLConf = context.getResources().openRawResource(R.raw.sslconf);
        Properties properties = new Properties();
        properties.load(inputStreamSSLConf);
        inputStreamSSLConf.close();
        String storeType = properties.getProperty("storeType");
        String algorithm = properties.getProperty("algorithm");
        String protocol = properties.getProperty("protocol");
        String sCertificatePwd = properties.getProperty("serverCertificatePwd");
        String sMainPwd = properties.getProperty("serverMainPwd");
        String cCertificatePwd = properties.getProperty("clientCertificatePwd");
        String cMainPwd = properties.getProperty("clientMainPwd");

        //KeyStore class is used to save certificate.
        char[] c_pwd = sCertificatePwd.toCharArray();
        KeyStore keyStore = KeyStore.getInstance(storeType);
        InputStream inputStreamServerCert = context.getResources().openRawResource(R.raw.server_rsa);
        keyStore.load(inputStreamServerCert, c_pwd);
        inputStreamServerCert.close();

        //KeyManagerFactory class is used to create KeyManager class.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
        char[] m_pwd = sMainPwd.toCharArray();
        keyManagerFactory.init(keyStore, m_pwd);
        //KeyManager class is used to choose a certificate
        //to prove the identity of the server side.
        KeyManager[] kms = keyManagerFactory.getKeyManagers();

        TrustManager[] tms = null;
        if(mutual){
            //KeyStore class is used to save certificate.
            c_pwd = cCertificatePwd.toCharArray();
            keyStore = KeyStore.getInstance(storeType);
            InputStream inputStreamClientCert = context.getResources().openRawResource(R.raw.client_rsa);
            keyStore.load(inputStreamClientCert, c_pwd);
            inputStreamClientCert.close();

            //TrustManagerFactory class is used to create TrustManager class.
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
            trustManagerFactory.init(keyStore);
            //TrustManager class is used to decide weather to trust the certificate
            //or not.
            tms = trustManagerFactory.getTrustManagers();
        }

        //SSLContext class is used to set all the properties about secure communication.
        //Such as protocol type and so on.
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(kms, tms, null);

        return sslContext;
    }
}