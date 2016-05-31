package com.willblaschko.android.alexavoicelibrary;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexavoicelibrary.actions.BaseListenerFragment;

import java.util.ArrayList;
import java.util.List;

import static com.willblaschko.android.alexavoicelibrary.global.Constants.PRODUCT_ID;

/**
 * Created by will on 5/30/2016.
 */

public abstract class BaseActivity extends AppCompatActivity implements BaseListenerFragment.AvsListenerInterface{

    private final static String TAG = "BaseActivity";

    private final static int STATE_LISTENING = 1;
    private final static int STATE_PROCESSING = 2;
    private final static int STATE_SPEAKING = 3;
    private final static int STATE_PROMPTING = 4;
    private final static int STATE_FINISHED = 0;

    private AlexaManager alexaManager;
    private AlexaAudioPlayer audioPlayer;
    private List<AvsItem> avsQueue = new ArrayList<>();

    private long startTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initAlexaAndroid();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(audioPlayer != null){
            audioPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(audioPlayer != null){
            //remove callback to avoid memory leaks
            audioPlayer.removeCallback(alexaAudioPlayerCallback);
            audioPlayer.release();
        }
    }


    @Override
    public AsyncCallback<AvsResponse, Exception> getRequestCallback() {
        return requestCallback;
    }





    private void initAlexaAndroid(){
        //get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(this, PRODUCT_ID);

        //instantiate our audio player
        audioPlayer = AlexaAudioPlayer.getInstance(this);

        //Remove the current item and check for more items once we've finished playing
        audioPlayer.addCallback(alexaAudioPlayerCallback);
    }

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {
        @Override
        public void playerPrepared(AvsItem pendingItem) {

        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            avsQueue.remove(completedItem);
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

    //async callback for commands sent to Alexa Voice
    private AsyncCallback<AvsResponse, Exception> requestCallback = new AsyncCallback<AvsResponse, Exception>() {
        @Override
        public void start() {
            startTime = System.currentTimeMillis();
            Log.i(TAG, "Voice Start");
            setState(STATE_PROCESSING);
        }

        @Override
        public void success(AvsResponse result) {
            Log.i(TAG, "Voice Success");
            handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            error.printStackTrace();
            Log.i(TAG, "Voice Error");
            setState(STATE_FINISHED);
        }

        @Override
        public void complete() {
            Log.i(TAG, "Voice Complete");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    long totalTime = System.currentTimeMillis() - startTime;
                    Toast.makeText(BaseActivity.this, "Total request time: "+totalTime+" miliseconds", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Total request time: "+totalTime+" miliseconds");
                }
            });
        }
    };

    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
     */
    private void handleResponse(AvsResponse response){
        if(response != null){
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for(int i = response.size() - 1; i >= 0; i--){
                if(response.get(i) instanceof AvsReplaceAllItem || response.get(i) instanceof AvsReplaceEnqueuedItem){
                    //clear our queue
                    avsQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            Log.i(TAG, "Adding "+response.size()+" items to our queue");
            avsQueue.addAll(response);
        }
        checkQueue();
    }


    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     *
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
            setState(STATE_FINISHED);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    long totalTime = System.currentTimeMillis() - startTime;
                    Toast.makeText(BaseActivity.this, "Total interaction time: "+totalTime+" miliseconds", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Total interaction time: "+totalTime+" miliseconds");
                }
            });
            return;
        }

        AvsItem current = avsQueue.get(0);

        Log.i(TAG, "Item type " + current.getClass().getName());

        if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayRemoteItem) current);
            }
        } else if (current instanceof AvsPlayContentItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayContentItem) current);
            }
        } else if (current instanceof AvsSpeakItem) {
            //play a sound file
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsSpeakItem) current);
            }
            setState(STATE_SPEAKING);
        } else if (current instanceof AvsStopItem) {
            //stop our play
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceAllItem) {
            //clear all items
            //mAvsItemQueue.clear();
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            //clear all items
            //mAvsItemQueue.clear();
            avsQueue.remove(current);
        } else if (current instanceof AvsExpectSpeechItem) {

            //listen for user input
            audioPlayer.stop();
            avsQueue.clear();
            startListening();
        } else if (current instanceof AvsSetVolumeItem) {
            setVolume(((AvsSetVolumeItem) current).getVolume());
            avsQueue.remove(current);
        } else if(current instanceof AvsAdjustVolumeItem){
            adjustVolume(((AvsAdjustVolumeItem) current).getAdjustment());
            avsQueue.remove(current);
        } else if(current instanceof AvsSetMuteItem){
            setMute(((AvsSetMuteItem) current).isMute());
            avsQueue.remove(current);
        }

    }

    protected abstract void startListening();

    private void adjustVolume(long adjust){
        setVolume(adjust, true);
    }
    private void setVolume(long volume){
        setVolume(volume, false);
    }
    private void setVolume(final long volume, final boolean adjust){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(adjust){
            vol += volume * max / 100;
        }else{
            vol = volume * max / 100;
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, AudioManager.FLAG_VIBRATE);

        alexaManager.sendVolumeChangedEvent(volume, vol == 0, requestCallback);

        Log.i(TAG, "Volume set to : " + vol +"/"+max+" ("+volume+")");

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(adjust) {
                    Toast.makeText(BaseActivity.this, "Volume adjusted.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(BaseActivity.this, "Volume set to: " + (volume / 10), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void setMute(final boolean isMute){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);

        alexaManager.sendMutedEvent(isMute, requestCallback);

        Log.i(TAG, "Mute set to : "+isMute);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, "Volume " + (isMute ? "muted" : "unmuted"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setState(final int state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state){
                    case(STATE_LISTENING):
                        stateListening();
                        break;
                    case(STATE_PROCESSING):
                        stateProcessing();
                        break;
                    case(STATE_SPEAKING):
                        stateSpeaking();
                        break;
                    case(STATE_FINISHED):
                        stateFinished();
                        break;
                    case(STATE_PROMPTING):
                        statePrompting();
                        break;
                    default:
                        stateNone();
                        break;
                }
            }
        });
    }

    protected abstract void stateListening();
    protected abstract void stateProcessing();
    protected abstract void stateSpeaking();
    protected abstract void stateFinished();
    protected abstract void statePrompting();
    protected abstract void stateNone();

}
