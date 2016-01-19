package com.willblaschko.android.alexa;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by will on 12/7/2015.
 */
public class VoiceHelper{

    private final static String TAG = "VoiceHelper";

    private static VoiceHelper mInstance;
    private Context mContext;
    private TextToSpeech mTextToSpeech;
    private boolean mIsIntialized = false;

    Map<String, SpeechFromTextCallback> mCallbacks = new HashMap<>();

    private VoiceHelper(Context context){
        mContext = context.getApplicationContext();
        mTextToSpeech = new TextToSpeech(mContext, mInitListener);
        mTextToSpeech.setPitch(.8f);
        mTextToSpeech.setSpeechRate(1.5f);
        mTextToSpeech.setOnUtteranceProgressListener(mUtteranceProgressListener);
    }

    public static VoiceHelper getInstance(Context context){
        if(mInstance == null){
            mInstance = new VoiceHelper(context);
        }

        return mInstance;
    }

    private TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if(status == TextToSpeech.SUCCESS){
                mIsIntialized = true;
            }else{
                throw new IllegalStateException("Unable to initialize Text to Speech engine");
            }
        }
    };

    private UtteranceProgressListener mUtteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {

        }

        @Override
        public void onDone(String utteranceId) {
            SpeechFromTextCallback callback = mCallbacks.get(utteranceId);
            if(callback != null){

                File cacheFile = getCacheFile(utteranceId);
                try {
                    byte[] data = FileUtils.readFileToByteArray(cacheFile);
                    callback.onSuccess(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                cacheFile.delete();
                mCallbacks.remove(utteranceId);
            }
        }

        @Override
        public void onError(String utteranceId) {
            this.onError(utteranceId, TextToSpeech.ERROR);
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            super.onError(utteranceId, errorCode);
            SpeechFromTextCallback callback = mCallbacks.get(utteranceId);
            if(callback != null){
                callback.onError(new Exception("Unable to process request, error code: "+errorCode));
            }
        }
    };

    public void getSpeechFromText(String text, SpeechFromTextCallback callback){
        String utteranceId = AuthorizationManager.createCodeVerifier();

        mCallbacks.put(utteranceId, callback);

        TextToSpeech textToSpeech = getTextToSpeech();

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

        textToSpeech.synthesizeToFile(text, params, getCacheFile(utteranceId).toString());
    }

    private TextToSpeech getTextToSpeech(){
        if(!mIsIntialized){
            throw new IllegalStateException("Text to Speech engine is not initalized");
        }
        return mTextToSpeech;
    }

    private File getCacheFile(String utteranceId){
        return new File(getCacheDir(), utteranceId+".wav");
    }

    private File getCacheDir(){
        return mContext.getCacheDir();
    }

    public interface SpeechFromTextCallback{
        void onSuccess(byte[] data);
        void onError(Exception e);
    }
}
