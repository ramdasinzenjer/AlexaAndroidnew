package com.willblaschko.android.alexa.connection;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 * Create a singleton OkHttp client that, hopefully, will someday be able to make sure all connections are valid according to AVS's strict
 * security policy--this will hopefully fix the Connection Reset By Peer issue.
 *
 * Created by willb_000 on 6/26/2016.
 */
public class ClientUtil {

    private static OkHttpClient mClient;

    public static OkHttpClient getTLS12OkHttpClient(){
        if(mClient == null) {
            OkHttpClient client = null;
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }
                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();
                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);

                client = new OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.MINUTES)
                        //Add Custom SSL Socket Factory which adds TLS 1.1 and 1.2 support for Android 4.1-4.4
                        .sslSocketFactory(new TLSSocketFactoryCompat(), trustManager)
                        .connectionSpecs(specs)
                        .build();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } finally {
                if (client == null) {
                    client = new OkHttpClient();
                }
            }
            mClient = client;
        }
        return mClient;
    }

}
