package com.willblaschko.android.alexa;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.items.AvsClearQueueItem;
import com.willblaschko.android.alexa.avs.items.AvsItem;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.avs.items.AvsListenItem;
import com.willblaschko.android.alexa.avs.items.AvsSpeakItem;
import com.willblaschko.android.alexa.avs.items.AvsStopItem;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback;
import com.willblaschko.android.alexa.sender.SendText;
import com.willblaschko.android.alexa.sender.SendVoice;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Created by wblaschko on 8/13/15.
 */
public class AlexaManager {
    private static final String TAG = "AlexaHandler";

    private static AlexaManager mInstance;
    private AuthorizationManager mAuthorizationManager;
    private SendVoice mSendVoice;
    private SendText mSendText;
    private VoiceHelper mVoiceHelper;
    private Context mContext;
    private boolean mIsRecording = false;

    private AlexaManager(Context context, String productId){
        mContext = context.getApplicationContext();
        mAuthorizationManager = new AuthorizationManager(mContext, productId);
        mSendVoice = new SendVoice();
        mSendText = new SendText();
        mVoiceHelper = VoiceHelper.getInstance(mContext);
    }

    public static AlexaManager getInstance(Context context, String productId){
        if(mInstance == null){
            mInstance = new AlexaManager(context, productId);
        }
        return mInstance;
    }

    public VoiceHelper getVoiceHelper(){
        return mVoiceHelper;
    }

    public void checkLoggedIn(final AsyncCallback<Boolean, Throwable> callback){
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                callback.success(result);
            }

            @Override
            public void failure(Throwable error) {
                callback.failure(error);
            }

            @Override
            public void complete() {

            }
        });
    }

    public void logIn(@Nullable final AuthorizationCallback callback){
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                if(result){
                    if(callback != null){
                        callback.onSuccess();
                    }
                }else{
                    mAuthorizationManager.authorizeUser(callback);
                }
            }

            @Override
            public void failure(Throwable error) {

            }

            @Override
            public void complete() {

            }
        });

    }

    public boolean isRecording(){
        return mIsRecording;
    }

    public void startRecording(int requestType, @Nullable AsyncCallback<Void, Exception> callback){
        startRecording(requestType, (byte[]) null, callback);
    }

    public void startRecording(final int requestType, @Nullable final String assetFile, @Nullable final AsyncCallback<Void, Exception> callback) throws IOException {

        byte[] bytes = null;
        //if we have an introduction audio clip, add it to the stream here
        if(assetFile != null){
            InputStream input= mContext.getAssets().open(assetFile);
            bytes = IOUtils.toByteArray(input);
            input.close();
        }

        startRecording(requestType, bytes, callback);
    }

    public void startRecording(final int requestType, @Nullable final byte[] assetFile, @Nullable final AsyncCallback<Void, Exception> callback){


        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                if (result) {

                    final String url;
                    switch (requestType) {
                        default:
                            url = "https://access-alexa-na.amazon.com/v1/avs/speechrecognizer/recognize/";
                    }
                    if (callback != null) {
                        callback.start();
                    }
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                                @Override
                                public void onSuccess(String token) {
                                    try {
                                        mIsRecording = true;
                                        mSendVoice.startRecording(mContext, url, token, assetFile, callback);

                                        if (callback != null) {
                                            callback.success(null);
                                        }
                                    } catch (IOException e) {
                                        mIsRecording = false;
                                        e.printStackTrace();
                                        if (callback != null) {
                                            callback.failure(e);
                                        }
                                    } finally {
                                        if (callback != null) {
                                            callback.complete();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Throwable e) {

                                }
                            });
                            return null;
                        }
                    }.execute();
                } else {
                    logIn(new AuthorizationCallback() {
                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onSuccess() {
                            startRecording(requestType, assetFile, callback);
                        }

                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                callback.failure(error);
                            }
                        }
                    });
                }
            }

            @Override
            public void failure(Throwable error) {

            }

            @Override
            public void complete() {

            }
        });
    }

    public void stopRecording(@Nullable final AsyncCallback<List<AvsItem>, Exception> callback){
        mIsRecording = false;
        if(callback != null) {
            callback.start();
        }

        new AsyncTask<Void, Void, AvsResponse>() {
            @Override
            protected AvsResponse doInBackground(Void... params) {
                try {
                    return mSendVoice.stopRecording();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.failure(e);
                    }
                } catch (AvsException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.failure(e);
                    }
                } finally {
                    if (callback != null) {
                        callback.complete();
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(AvsResponse response) {
                try {
                    if (callback != null) {
                        callback.success(parseResponse(response));
                        callback.complete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.failure(e);
                        callback.complete();
                    }
                }
                Log.i(TAG, "Done!");
            }
        }.execute();

    }

    public void sendTextRequest(final int requestType, final String text, final AsyncCallback<List<AvsItem>, Exception> callback){
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                if (result) {

                    final String url;
                    switch (requestType) {
                        default:
                            url = "https://access-alexa-na.amazon.com/v1/avs/speechrecognizer/recognize/";
                    }
                    if (callback != null) {
                        callback.start();
                    }
                    new AsyncTask<Void, Void, AvsResponse>() {
                        @Override
                        protected AvsResponse doInBackground(Void... params) {
                            TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                                @Override
                                public void onSuccess(String token) {

                                    try {
                                        mSendText.sendText(mContext, url, token, text, new AsyncCallback<AvsResponse, Exception>() {
                                            @Override
                                            public void start() {
                                                if (callback != null) {
                                                    callback.start();
                                                }
                                            }

                                            @Override
                                            public void success(AvsResponse result) {
                                                if (callback != null) {
                                                    callback.success(parseResponse(result));
                                                }
                                            }

                                            @Override
                                            public void failure(Exception error) {
                                                if (callback != null) {
                                                    callback.failure(error);
                                                }
                                            }

                                            @Override
                                            public void complete() {
                                                if (callback != null) {
                                                    callback.complete();
                                                }
                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        callback.failure(e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable e) {

                                }
                            });
                            return null;
                        }


                        @Override
                        protected void onPostExecute(AvsResponse avsResponse) {
                            super.onPostExecute(avsResponse);
                        }
                    }.execute();
                } else {
                    logIn(new AuthorizationCallback() {
                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onSuccess() {
                            sendTextRequest(requestType, text, callback);
                        }

                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                callback.failure(error);
                            }
                        }
                    });
                }
            }

            @Override
            public void failure(Throwable error) {

            }

            @Override
            public void complete() {

            }
        });
    }

    private List<AvsItem> parseResponse(AvsResponse response){
        List<AvsItem> items = new ArrayList<>();

        if (response == null) {
            return items;
        }

        JsonObject topObj = response.getJson().readObject();
        JsonObject body = topObj.getJsonObject("messageBody");

        JsonArray directives = body.getJsonArray("directives");
        for (JsonObject directive : directives.getValuesAs(JsonObject.class)) {
            String name = directive.getString("name");
            System.out.println("Directive: " + name);

            if (name.equals("speak")) {
                JsonObject payload = directive.getJsonObject("payload");
                String cid = payload.getString("audioContent");
                cid = "<" + cid.substring(4) + ">";
                System.out.println("CID: " + cid);
                items.add(new AvsSpeakItem(cid, response.getAudio().get(cid)));


            } else if (name.equals("stop")) {
                items.add(new AvsStopItem());
            } else if (name.equals("clearQueue")) {
                items.add(new AvsClearQueueItem());
            } else if (name.equals("listen")) {
                items.add(new AvsListenItem());
            }
        }
        return items;
    }

    public static class REQUEST_TYPE{
        public final static int TYPE_VOICE_RESPONSE = 1;
    }

}
