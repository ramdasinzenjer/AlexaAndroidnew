package com.willblaschko.android.alexavoicelibrary;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.avs.AlexaAudioPlayer;
import com.willblaschko.android.alexa.avs.items.AvsClearQueueItem;
import com.willblaschko.android.alexa.avs.items.AvsItem;
import com.willblaschko.android.alexa.avs.items.AvsListenItem;
import com.willblaschko.android.alexa.avs.items.AvsPlayItem;
import com.willblaschko.android.alexa.avs.items.AvsSpeakItem;
import com.willblaschko.android.alexa.avs.items.AvsStopItem;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A quick sample Activity that we can use as the launcher replacement for Android's
 * swipe up from home. It will be transparent and only show the native speech prompt
 * so that a user can send a command to Alexa.
 */
public class VoiceLaunchActivity extends AppCompatActivity {
    private static final String TAG = "VocieLaunchActivity";
    private final static int RESULT_SPEECH = 11;

    //Our Amazon application product ID, this is passed to the server when we authenticate
    //this will only work with the right application signature
    private static final String PRODUCT_ID = "interactive_conversation";


    //Our AlexaManager instance which handles all our calls
    private AlexaManager mAlexaManager;
    private AlexaAudioPlayer mAudioPlayer;
    private List<AvsItem> mAvsItemQueue = new ArrayList<>();
    private boolean mIsInInteraction = false;
    private static long mStartTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_voice_launch);

        //get our AlexaManager instance for convenience
        mAlexaManager = AlexaManager.getInstance(this, PRODUCT_ID);

        //instantiate our audio player
        mAudioPlayer = AlexaAudioPlayer.getInstance(this);

        //Remove the current item and check for more items once we've finished playing
        mAudioPlayer.addCallback(mAlexaAudioPlayerCallback);

        //Run an async check on whether we're logged in or not
        mAlexaManager.checkLoggedIn(mLoggedInCheck);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //remove callback to avoid memory leaks
        mAudioPlayer.removeCallback(mAlexaAudioPlayerCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //listen for a result from our get speech to text request
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if(text.size() > 0) {
                        sendVoiceToAlexa(text.get(0));
                    }
                }
                break;
            }

        }
    }

    /**
     * Launch our listen activity (native Android intent) to get back our string text response
     * this is not the most direct way of doing this, but it allows us to not have to deal with permissions
     * and gives us a nice, clean UI without any extra work on our part.
     *
     * Ideally, we'd have a skinned intent here that gets raw voice from the user and sends that directly to the service.
     * AlexaManager.startRecording() + AlexaManager.stopRecording() can do this, but it is not checking for Android 6.0 permissions
     * this would need to be done outside the library (here).
     */
    private void startListening(){

        if(!mIsInInteraction) {
            mStartTime = System.currentTimeMillis();
            mIsInInteraction = true;
        }
        mAudioPlayer.release();
        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

        try {
            startActivityForResult(intent, RESULT_SPEECH);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(VoiceLaunchActivity.this,
                    "Oops! Your device doesn't support Speech to Text",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * We've finished our single use interaction, it's time to log how long the interaction took
     * and then finish the activity
     */
    private void endInteraction(){
        if(mIsInInteraction) {
            Log.i(TAG, "Whole interaction took: " + (System.currentTimeMillis() - mStartTime));
        }

        VoiceLaunchActivity.this.finish();
    }


    /**
     * Send our speech to text text to the Alexa server for Intent parsing, parse the response using the mVoiceCallback
     * @param text the Speech-To-Text value returned by our startListening() intent. This value can be prepended or appended
     *             with whatever values make the Alexa Intent behave correctly (such as "Open MySuperDuperCarWashApp and..." if the
     *             text is "wash my car").
     */
    private void sendVoiceToAlexa(String text){
        mAlexaManager.sendTextRequest(AlexaManager.REQUEST_TYPE.TYPE_VOICE_RESPONSE, text, mVoiceCallback);
    }

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback mAlexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {
        @Override
        public void playerPrepared(AvsItem pendingItem) {

        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            mAvsItemQueue.remove(completedItem);
            checkQueue();
        }

        @Override
        public boolean playerError(int what, int extra) {
            return false;
        }

        @Override
        public void dataError(Exception e) {

        }
    };

    //an async callback to determine whether we're logged in and then perform actions based on that result
    private AsyncCallback<Boolean, Throwable> mLoggedInCheck = new AsyncCallback<Boolean, Throwable>() {
        @Override
        public void start() {
            //authentication check started
        }

        @Override
        public void success(Boolean result) {

            if(result){
            //we're logged in, do our voice request
            startListening();

            }else{
                //we're not logged in, so ask user to log in

                //call our login function
                mAlexaManager.logIn(new AuthorizationCallback() {
                    @Override
                    public void onCancel() {
                        //login attempt cancelled
                        Toast.makeText(VoiceLaunchActivity.this, "Login action cancelled.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //login attempt successful
                        Toast.makeText(VoiceLaunchActivity.this, "Successfully logged in.", Toast.LENGTH_SHORT).show();


                        startListening();
                    }

                    @Override
                    public void onError(Exception error) {
                        //login attempt met with error
                        Toast.makeText(VoiceLaunchActivity.this, "Error during the login process: "+error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    }
                });
            }
        }

        @Override
        public void failure(Throwable error) {
            //authentication check failed
            Toast.makeText(VoiceLaunchActivity.this, "Error during the authentication check process: "+error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            error.printStackTrace();
        }

        @Override
        public void complete() {
            //authentication check complete

        }
    };

    //async callback for commands sent to Alexa Voice
    private AsyncCallback<List<AvsItem>, Exception> mVoiceCallback = new AsyncCallback<List<AvsItem>, Exception>() {
        @Override
        public void start() {
            Log.i(TAG, "Voice Start");
        }

        @Override
        public void success(List<AvsItem> result) {
            Log.i(TAG, "Voice Success");
            handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            error.printStackTrace();
            Log.i(TAG, "Voice Error");
        }

        @Override
        public void complete() {
            Log.i(TAG, "Voice Complete");
        }
    };

    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
     */
    private void handleResponse(List<AvsItem> response){
        if(response != null){
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for(int i = response.size() - 1; i >= 0; i--){
                if(response.get(i) instanceof AvsClearQueueItem){
                    //clear our queue
                    mAvsItemQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            Log.i(TAG, "Adding "+response.size()+" items to our queue");
            mAvsItemQueue.addAll(response);
            checkQueue();
        }

    }

    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     *
     * We're handling the AvsClearQueueItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private void checkQueue(){


        //if we're out of things, hang up the phone and move on
        if(mAvsItemQueue.size() == 0){
            endInteraction();
            return;
        }

        AvsItem current = mAvsItemQueue.get(0);

        if(current instanceof AvsPlayItem){
            //play a URL
            Log.i(TAG, "Item type AvsPlayItem");
            if(!mAudioPlayer.isPlaying()){
                mAudioPlayer.playItem((AvsPlayItem) current);
            }
        }else if(current instanceof AvsSpeakItem){
            //play a sound file
            Log.i(TAG, "Item type AvsSpeakItem");
            if(!mAudioPlayer.isPlaying()){
                mAudioPlayer.playItem((AvsSpeakItem) current);
            }
        }else if(current instanceof AvsStopItem){
            //stop our play
            Log.i(TAG, "Item type AvsStopItem");
            mAudioPlayer.stop();
            mAvsItemQueue.remove(current);
        }else if(current instanceof AvsClearQueueItem){
            //clear all items
            Log.i(TAG, "Item type AvsClearQueueItem");
            //mAvsItemQueue.clear();
            mAudioPlayer.stop();
            mAvsItemQueue.remove(current);
        }else if(current instanceof AvsListenItem){
            //listen for user input
            Log.i(TAG, "Item type AvsListenItem");
            mAudioPlayer.stop();
            mAvsItemQueue.clear();
            startListening();
        }

    }
}
