package com.willblaschko.android.alexa.sender;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.willblaschko.android.alexa.VoiceHelper;
import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;

import java.io.IOException;

/**
 * Created by will on 12/7/2015.
 */
public class SendText extends SendData {

    private final static String TAG = "SendText";

    long start = 0;

    public void sendText(Context context, String url, String accessToken, String text, final AsyncCallback<AvsResponse, Exception> callback) throws IOException {

        Log.i(TAG, "Starting SendText procedure");
        start = System.currentTimeMillis();

        if(!TextUtils.isEmpty(text)){
            text = text + ".....";
        }
        preparePost(url, accessToken);
        VoiceHelper voiceHelper = VoiceHelper.getInstance(context);
        voiceHelper.getSpeechFromText(text, new VoiceHelper.SpeechFromTextCallback() {
            @Override
            public void onSuccess(final byte[] data){

                Log.i(TAG, "We have audio");

                try {
                    mOutputStream.write(data);

                    Log.i(TAG, "Audio sent");
                    Log.i(TAG, "Audio creation process took: " + (System.currentTimeMillis() - start));

                    callback.success(completePost());

                } catch (IOException e) {
                    onError(e);
                } catch (AvsException e) {
                    onError(e);
                }
            }


            @Override
            public void onError(Exception e) {
                callback.failure(e);
            }
        });
    }
}
