package com.willblaschko.android.alexa.interfaces;

import android.util.Log;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.connection.TLSSocketFactoryCompat;
import com.willblaschko.android.alexa.interfaces.response.ResponseParser;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * An abstract class that supplies a DataOutputStream which is used to send a POST request to the AVS server
 * with a voice data intent, it handles the response with completePost() (called by extending classes)
 */
public abstract class SendEvent {

    private final static String TAG = "SendEvent";

    //the output stream that extending classes will use to pass data to the AVS server
    protected ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();

    protected AsyncCallback<Void, Exception> mCallback;

    //OkHttpClient for transfer of data
    OkHttpClient mClient;
    Request.Builder mRequestBuilder = new Request.Builder();
    MultipartBody.Builder mBodyBuilder;

    /**
     * Set up all the headers that we need in our OkHttp POST/GET, this prepares the connection for
     * the event or the raw audio that we'll need to pass to the AVS server
     * @param url the URL we're posting to, this is either the default {@link com.willblaschko.android.alexa.data.Directive} or {@link com.willblaschko.android.alexa.data.Event} URL
     * @param accessToken the access token of the user who has given consent to the app
     */
    protected void prepareConnection(String url, String accessToken) {

        mClient = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            mClient = new OkHttpClient.Builder()
                    //Add Custom SSL Socket Factory which adds TLS 1.1 and 1.2 support for Android 4.1-4.4
                    .sslSocketFactory(new TLSSocketFactoryCompat(), trustManager)
                    .build();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } finally {
            if(mClient == null){
                mClient = new OkHttpClient();
            }
        }

        //set the request URL
        mRequestBuilder.url(url);

        //set our authentication access token header
        mRequestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        mBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", "metadata", RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), getEvent()));

        //reset our output stream
        mOutputStream = new ByteArrayOutputStream();
    }

    /**
     * When finished adding voice data to the output, we close it using completePost() and it is sent off to the AVS server
     * and the response is parsed and returned
     * @return AvsResponse with all the data returned from the server
     * @throws IOException if the OkHttp request can't execute
     * @throws AvsException if we can't parse the response body into an {@link AvsResponse} item
     * @throws RuntimeException
     */
    protected AvsResponse completePost() throws IOException, AvsException, RuntimeException {
        addFormDataParts(mBodyBuilder);
        mRequestBuilder.post(mBodyBuilder.build());
        return parseResponse();
    }

    protected AvsResponse completeGet() throws IOException, AvsException, RuntimeException {
        mRequestBuilder.get();
        return parseResponse();
    }

    private AvsResponse parseResponse() throws IOException, AvsException, RuntimeException{
        Request request = mRequestBuilder.build();
        Response response = mClient.newCall(request).execute();

        Headers headers = response.headers();
        String header = headers.get("content-type");
        String boundary = "";

        AvsResponse val = new AvsResponse();

        if(header != null) {

            Pattern pattern = Pattern.compile("boundary=(.*?);");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                boundary = matcher.group(1);
            }
            val = ResponseParser.parseResponse(response.body().byteStream(), boundary);
        }else{
            String body = response.body().string();
            Log.i(TAG, body);
        }
        response.body().close();

        return val;
    }

    /**
     * When override, our extending classes can add their own data to the POST
     * @param builder with audio data
     */
    protected void addFormDataParts(MultipartBody.Builder builder){

    };

    /**
     * Get our JSON {@link com.willblaschko.android.alexa.data.Event} for this call
     * @return the JSON representation of the {@link com.willblaschko.android.alexa.data.Event}
     */
    @NotNull
    protected abstract String getEvent();

}