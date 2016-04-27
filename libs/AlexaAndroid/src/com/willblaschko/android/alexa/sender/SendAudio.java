package com.willblaschko.android.alexa.sender;

import android.content.Context;
import android.util.Log;

import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;

import java.io.IOException;

/**
 * Created by will on 4/17/2016.
 */
public class SendAudio extends SendData {

    private final static String TAG = "SendAudio";

    long start = 0;

    public void sendAudio(final Context context, final String url, final String accessToken, final byte[] data, final AsyncCallback<AvsResponse, Exception> callback) throws IOException {
        if(callback != null){
            callback.start();
        }
        Log.i(TAG, "Starting SendAudio procedure");
        start = System.currentTimeMillis();


        //call the parent class's preparePost() in order to prepare our URL POST
        try {
            preparePost(url, accessToken);
        } catch (IOException e) {
            e.printStackTrace();
            if(callback != null) {
                callback.failure(e);
                callback.complete();
            }
        }
        try {
            mOutputStream.write(data);

            Log.i(TAG, "Audio sent");
            Log.i(TAG, "Audio sending process took: " + (System.currentTimeMillis() - start));
            if(callback != null) {
                callback.success(completePost());
                callback.complete();
            }

        } catch (IOException e) {
            onError(callback, e);
        } catch (AvsException e) {
            onError(callback, e);
        }





    }
    public void onError(final AsyncCallback<AvsResponse, Exception> callback, Exception e) {
        if(callback != null){
            callback.failure(e);
            callback.complete();
        }
    }
}
