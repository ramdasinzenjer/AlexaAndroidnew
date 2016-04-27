package com.willblaschko.android.alexa;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.avs.items.AvsClearQueueItem;
import com.willblaschko.android.alexa.avs.items.AvsItem;
import com.willblaschko.android.alexa.avs.items.AvsListenItem;
import com.willblaschko.android.alexa.avs.items.AvsPlayItem;
import com.willblaschko.android.alexa.avs.items.AvsSpeakItem;
import com.willblaschko.android.alexa.avs.items.AvsStopItem;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback;
import com.willblaschko.android.alexa.sender.SendAudio;
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
 * The overarching instance that handles all the state when requesting intents to the Alexa Voice Service servers, it creates all the required instances and confirms that users are logged in
 * and authenticated before allowing them to send intents.
 *
 * Beyond initialization, mostly it supplies wrapped helper functions to the other classes to assure authentication state.
 */
public class AlexaManager {
    private static final String TAG = "AlexaHandler";

    private static AlexaManager mInstance;
    private AuthorizationManager mAuthorizationManager;
    private SendVoice mSendVoice;
    private SendText mSendText;
    private SendAudio mSendAudio;
    private VoiceHelper mVoiceHelper;
    private Context mContext;
    private boolean mIsRecording = false;

    private AlexaManager(Context context, String productId){
        mContext = context.getApplicationContext();
        mAuthorizationManager = new AuthorizationManager(mContext, productId);
        mSendVoice = new SendVoice();
        mSendText = new SendText();
        mSendAudio = new SendAudio();
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

    /**
     * Check if the user is logged in to the Amazon service, uses an async callback with a boolean to return response
     * @param callback state callback
     */
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

    /**
     * Send a log in request to the Amazon Authentication Manager
     * @param callback state callback
     */
    public void logIn(@Nullable final AuthorizationCallback callback){
        //check if we're already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                //if we are, return a success
                if(result){
                    if(callback != null){
                        callback.onSuccess();
                    }
                }else{
                    //otherwise start the authorization process
                    mAuthorizationManager.authorizeUser(callback);
                }
            }

            @Override
            public void failure(Throwable error) {
                if(callback != null) {
                    callback.onError(new Exception(error));
                }
            }

            @Override
            public void complete() {

            }
        });

    }

    /**
     * Helper function to check if we're currently recording
     * @return
     */
    public boolean isRecording(){
        return mIsRecording;
    }

    //helper function to start our recording
    public void startRecording(int requestType, @Nullable AsyncCallback<Void, Exception> callback){
        startRecording(requestType, (byte[]) null, callback);
    }

    //helper function to start our recording
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

    /**
     * Paired with stopRecording()--these need to be triggered manually or programmatically as a pair.
     *
     * This operation is done off the main thread and may need to be brought back to the main thread on callbacks.
     *
     * Check to see if the user is logged in, and if not, we request login, when they log in, or if they already are, we start recording audio
     * to pass to the Amazon AVS server. This audio can be pre-pended by the byte[] assetFile, which needs to match the audio requirements of
     * the rest of the service.
     * @param requestType our request type, currently there is only one "speechrecognizer"
     * @param assetFile our nullable byte[] that prepends audio to the record request
     * @param callback our state callback
     */
    public void startRecording(final int requestType, @Nullable final byte[] assetFile, @Nullable final AsyncCallback<Void, Exception> callback){

        //check if user is logged in
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                //if the user is already logged in
                if (result) {

                    final String url;
                    switch (requestType) {
                        default:
                            url = "https://access-alexa-na.amazon.com/v1/avs/speechrecognizer/recognize/";
                    }
                    if (callback != null) {
                        callback.start();
                    }
                    //perform this off the main thread
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            //get our user's access token
                            TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                                @Override
                                public void onSuccess(String token) {
                                    //we are authenticated, let's record some audio!
                                    try {
                                        mIsRecording = true;
                                        mSendVoice.startRecording(mContext, url, token, assetFile, callback);

                                        if (callback != null) {
                                            callback.success(null);
                                        }
                                    } catch (IOException e) {
                                        mIsRecording = false;
                                        e.printStackTrace();
                                        //bubble up
                                        if (callback != null) {
                                            callback.failure(e);
                                        }
                                    } finally {
                                        //bubble up
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
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    //user is not logged in, log them in
                    logIn(new AuthorizationCallback() {
                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onSuccess() {
                            //start the call all over again
                            startRecording(requestType, assetFile, callback);
                        }

                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                //bubble up the error
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

    /**
     * Paired with startRecording()--these need to be triggered manually or programmatically as a pair.
     *
     * This operation is done off the main thread and may need to be brought back to the main thread on callbacks.
     *
     * Stop our current audio being recorded and send the post request off to the server.
     *
     * Warning: this does not check whether we're currently logged in. The world could explode if called without startRecording()
     * @param callback
     */
    public void stopRecording(@Nullable final AsyncCallback<List<AvsItem>, Exception> callback){
        if (!mIsRecording) {
            if(callback != null) {
                callback.failure(new RuntimeException("recording not started"));
            }
            return;
        }

        mIsRecording = false;
        if(callback != null) {
            callback.start();
        }

        //make sure we're doing this off the main thread
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    //stop recording audio and get a response
                    AvsResponse response = mSendVoice.stopRecording();

                    //parse that response
                    try {
                        if (callback != null) {
                            callback.success(parseResponse(response));
                            callback.complete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (callback != null) {
                            //bubble up the error
                            callback.failure(e);
                            callback.complete();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    //bubble up the error
                    if (callback != null) {
                        callback.failure(e);
                    }
                } catch (AvsException e) {
                    e.printStackTrace();
                    //bubble up the error
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

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    /**
     * Send a text string request to the AVS server, this is run through Text-To-Speech to create the raw audio file needed by the AVS server.
     *
     * This allows the developer to pre/post-pend or send any arbitrary text to the server, versus the startRecording()/stopRecording() combination which
     * expects input from the user. This operation, because of the extra steps is generally slower than the above option.
     *
     * @param requestType our request type, currently there is only one "speechrecognizer"
     * @param text the arbitrary text that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendTextRequest(final int requestType, final String text, final AsyncCallback<List<AvsItem>, Exception> callback){
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    final String url;
                    switch (requestType) {
                        default:
                            url = "https://access-alexa-na.amazon.com/v1/avs/speechrecognizer/recognize/";
                    }
                    if (callback != null) {
                        callback.start();
                    }

                    //do this off the main thread
                    new AsyncTask<Void, Void, AvsResponse>() {
                        @Override
                        protected AvsResponse doInBackground(Void... params) {
                            //get our access token
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
                                                //parse our response
                                                if (callback != null) {
                                                    callback.success(parseResponse(result));
                                                }
                                            }

                                            @Override
                                            public void failure(Exception error) {
                                                //bubble up the error
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
                                        //bubble up the error
                                        if(callback != null) {
                                            callback.failure(e);
                                        }
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
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(new AuthorizationCallback() {
                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onSuccess() {
                            //call our function again
                            sendTextRequest(requestType, text, callback);
                        }

                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                //bubble up the error
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


    /**
     * Send raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param requestType our request type, currently there is only one "speechrecognizer"
     * @param data the audio data that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendAudioRequest(final int requestType, final byte[] data, final AsyncCallback<List<AvsItem>, Exception> callback){
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    final String url;
                    switch (requestType) {
                        default:
                            url = "https://access-alexa-na.amazon.com/v1/avs/speechrecognizer/recognize/";
                    }
                    //get our access token
                    TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                        @Override
                        public void onSuccess(final String token) {
                            //do this off the main thread
                            new AsyncTask<Void, Void, AvsResponse>() {
                                @Override
                                protected AvsResponse doInBackground(Void... params) {


                                    try {
                                        mSendAudio.sendAudio(mContext, url, token, data, new AsyncCallback<AvsResponse, Exception>() {
                                            @Override
                                            public void start() {
                                                if (callback != null) {
                                                    callback.start();
                                                }
                                            }

                                            @Override
                                            public void success(AvsResponse result) {
                                                //parse our response
                                                if (callback != null) {
                                                    callback.success(parseResponse(result));
                                                }
                                            }

                                            @Override
                                            public void failure(Exception error) {
                                                //bubble up the error
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
                                        //bubble up the error
                                        if(callback != null) {
                                            callback.failure(e);
                                        }
                                    }

                                    return null;
                                }


                                @Override
                                protected void onPostExecute(AvsResponse avsResponse) {
                                    super.onPostExecute(avsResponse);
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        @Override
                        public void onFailure(Throwable e) {

                        }
                    });
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(new AuthorizationCallback() {
                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onSuccess() {
                            //call our function again
                            sendAudioRequest(requestType, data, callback);
                        }

                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                //bubble up the error
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

    /**
     * Parse the AvsResponse response sent (and already partially parsed) by the AVS server, this has a variety of commands or
     * objects for the control to play
     * @param response the parsed but unsorted response
     * @return a List<AvsItem> of items for the application to handle with the user
     */
    private List<AvsItem> parseResponse(AvsResponse response){

        //if(BuildConfig.DEBUG){
        //Log.i(TAG, new Gson().toJson(response));
        //}

        List<AvsItem> items = new ArrayList<>();

        if (response == null) {
            return items;
        }

        //get our Json object items and get the message body
        JsonObject topObj = response.getJson().readObject();
        JsonObject body = topObj.getJsonObject("messageBody");

        //get the array of directives we're passing back as AvsItems
        JsonArray directives = body.getJsonArray("directives");

        //loop through each one
        for (JsonObject directive : directives.getValuesAs(JsonObject.class)) {
            String name = directive.getString("name");

            //logging for sanity
            if(BuildConfig.DEBUG) {
                Log.i(TAG, "Directive: " + name);
            }


            if (name.equals("speak")) {
                //if it's a speak item, we extract the byte[]
                JsonObject payload = directive.getJsonObject("payload");
                String cid = payload.getString("audioContent");
                cid = "<" + cid.substring(4) + ">";
                System.out.println("CID: " + cid);
                items.add(new AvsSpeakItem(cid, response.getAudio().get(cid)));
            } else if (name.equals("stop")) {
                //if it's a stop item
                items.add(new AvsStopItem());
            } else if (name.equals("clearQueue")) {
                //if it's a clear queue item
                items.add(new AvsClearQueueItem());
            } else if (name.equals("listen")) {
                //if it's a listen item
                items.add(new AvsListenItem());
            } else if (name.equals("play")){
                JsonObject payload = directive.getJsonObject("payload");
                items.addAll(parsePlayItems(response, payload.getJsonObject("audioItem")));
            }
            //todo add mute item here (boolean to mute/not), not sure exact directive name
        }
        return items;
    }

    /**
     * Parse the individual play items, some will be made into Speak items instead, because they follow that
     * format better, since we already have their byte[] data
     * @param response
     * @param media
     * @return a list of <AvsItem> to add to our queue
     */
    private List<AvsItem> parsePlayItems(AvsResponse response, JsonObject media){

        List<AvsItem> items = new ArrayList<>();

        JsonArray streams = media.getJsonArray("streams");
        for (int i = 0; i < streams.size(); i++) {
            JsonObject stream = streams.getJsonObject(i);
            String strUrl = stream.getString("streamUrl");
            String streamId = stream.getString("streamId");
            System.out.println("URL: " + strUrl);
            System.out.println("StreamId: " + streamId);
            int offset = 0;
            int progressDelay = 0;
            int progressInterval = 0;
            try {
                // no offset means start at beginning
                offset = stream.getInt("offsetInMilliseconds");
            } catch (Exception e) {
            }
            System.out.println("Offset: " + offset);
            if (stream.getBoolean("progressReportRequired")) {
                JsonObject progressReport = stream.getJsonObject("progressReport");
                progressDelay = progressReport.getInt("progressReportDelayInMilliseconds");
                progressInterval = progressReport.getInt("progressReportIntervalInMilliseconds");
                System.out.println("ProgressDelay: " + progressDelay);
                System.out.println("ProgressInterval: " + progressInterval);
            }

            // if it starts with "cid:", the audio is included in the response as a part
            if (strUrl.startsWith("cid:")) {
                try {
                    String cid = "<" + strUrl.substring(4) + ">";
                    items.add(new AvsSpeakItem(cid, response.getAudio().get(cid)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                items.add(new AvsPlayItem(strUrl, streamId, offset, progressDelay, progressInterval));
            }
        }

        return items;
    }

    public static class REQUEST_TYPE{
        public final static int TYPE_VOICE_RESPONSE = 1;
    }

}
