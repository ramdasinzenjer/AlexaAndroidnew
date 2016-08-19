package com.willblaschko.android.alexa.interfaces.system;

import android.util.Log;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.SendEvent;
import com.willblaschko.android.alexa.interfaces.response.ResponseParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Open Down Channel {@link com.willblaschko.android.alexa.data.Event} to open a persistent connection with the Alexa server. Currently doesn't seem to work as expected.
 *
 * {@link com.willblaschko.android.alexa.data.Event}
 *
 * @author will on 5/21/2016.
 */
public class OpenDownchannel extends SendEvent {

    private static final String TAG = "OpenDownchannel";

    public OpenDownchannel(final String url, final String accessToken,
                           final AsyncCallback<AvsResponse, Exception> callback) throws IOException {
        if(callback != null){
            callback.start();
        }
        Log.i(TAG, "Starting Open Downchannel procedure");
        long start = System.currentTimeMillis();
        try {
            OkHttpClient client = ClientUtil.getTLS12OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            final Response response = client.newCall(request).execute();

            Headers headers = response.headers();
            String header = headers.get("content-type");
            String boundary = "";

            Pattern pattern = Pattern.compile("boundary=(.*?);");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                boundary = matcher.group(1);
            }

            final BufferedSource source = response.body().source();
            Buffer buffer = new Buffer();
            while (!source.exhausted()) {
                response.body().source().read(buffer, 8192);
                AvsResponse val = new AvsResponse();

                try {
                    //final String test = buffer.readString(Charset.defaultCharset());
                    val = ResponseParser.parseResponse(buffer.inputStream(), boundary);
                } catch (Exception exp) {
                    exp.printStackTrace();
                }

                if (callback != null) {
                    callback.success(val);
                }
            }

            Log.i(TAG, "Downchannel open");
            Log.i(TAG, "Open Downchannel process took: " + (System.currentTimeMillis() - start));
        } catch (IOException e) {
            onError(callback, e);
        }
    }

    private void onError(final AsyncCallback<AvsResponse, Exception> callback, Exception e) {
        if(callback != null){
            callback.failure(e);
            callback.complete();
        }
    }


    @Override
    @NotNull
    protected String getEvent() {
        return "";
    }
}
