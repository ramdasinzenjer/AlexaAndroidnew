package com.willblaschko.android.alexa.interfaces.system;

import android.util.Log;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.SendEvent;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Open Down Channel {@link com.willblaschko.android.alexa.data.Event} to open a persistent connection with the Alexa server. Currently doesn't seem to work as expected.
 *
 * {@link com.willblaschko.android.alexa.data.Event}
 *
 * @author will on 5/21/2016.
 */
public class OpenDownchannel extends SendEvent {

    private static final String TAG = "OpenDownchannel";

    public OpenDownchannel(final String url, final String accessToken, final AsyncCallback<AvsResponse, Exception> callback) throws IOException {
        if(callback != null){
            callback.start();
        }
        Log.i(TAG, "Starting Open Downchannel procedure");
        long start = System.currentTimeMillis();

        //call the parent class's prepareConnection() in order to prepare our URL POST
        try {
            prepareConnection(url, accessToken);
            if(callback != null) {
                callback.success(completeGet());
                callback.complete();
            }else{
                completeGet();
            }
            Log.i(TAG, "Downchannel open");
            Log.i(TAG, "Open Downchannel process took: " + (System.currentTimeMillis() - start));
        } catch (IOException e) {
            onError(callback, e);
        } catch (AvsException e) {
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
