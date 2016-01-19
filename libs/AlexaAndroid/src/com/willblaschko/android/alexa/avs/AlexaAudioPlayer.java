package com.willblaschko.android.alexa.avs;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import com.willblaschko.android.alexa.avs.items.AvsItem;
import com.willblaschko.android.alexa.avs.items.AvsPlayItem;
import com.willblaschko.android.alexa.avs.items.AvsSpeakItem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by will on 12/9/2015.
 */
public class AlexaAudioPlayer {

    public static final String TAG = "AlexaAudioPlayer";

    private static AlexaAudioPlayer mInstance;

    private MediaPlayer mMediaPlayer;
    private Context mContext;
    private AvsItem mItem;
    private List<Callback> mCallbacks = new ArrayList<>();

    private AlexaAudioPlayer(Context context){
       mContext = context.getApplicationContext();
    }

    public static AlexaAudioPlayer getInstance(Context context){
        if(mInstance == null){
            mInstance = new AlexaAudioPlayer(context);
        }
        return mInstance;
    }

    private MediaPlayer getMediaPlayer(){
        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
        }
        return mMediaPlayer;
    }

    public void addCallback(Callback callback){
        if(!mCallbacks.contains(callback)){
            mCallbacks.add(callback);
        }
    }

    public void removeCallback(Callback callback){
        mCallbacks.remove(callback);
    }

    public void playItem(AvsSpeakItem item){
        play(item);
    }
    public void playItem(AvsPlayItem item){
        play(item);
    }

    private void play(AvsItem item){
        if(isPlaying()){
            Log.w(TAG, "Already playing an item, did you mean to play another?");
        }
        mItem = item;
        if(getMediaPlayer().isPlaying()){
            getMediaPlayer().stop();
        }
        if(mItem instanceof AvsPlayItem){
            AvsPlayItem playItem = (AvsPlayItem) item;
            try {
                getMediaPlayer().reset();
                getMediaPlayer().setDataSource(playItem.getUrl());
            } catch (IOException e) {
                e.printStackTrace();
                bubbleUpError(e);
            }
        }else if(mItem instanceof AvsSpeakItem){
            AvsSpeakItem playItem = (AvsSpeakItem) item;
            File path=new File(mContext.getCacheDir()+"/playfile.3gp");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                fos.write(read(playItem.getAudio()));
                fos.close();
                getMediaPlayer().reset();
                getMediaPlayer().setDataSource(mContext.getCacheDir() + "/playfile.3gp");
            } catch (IOException e) {
                e.printStackTrace();
                bubbleUpError(e);
            }

        }
        getMediaPlayer().prepareAsync();
    }

    private byte[] read(ByteArrayInputStream bais) throws IOException {
        byte[] array = new byte[bais.available()];
        bais.read(array);
        return array;
    }

    public boolean isPlaying(){
        return getMediaPlayer().isPlaying();
    }

    public void pause(){
        getMediaPlayer().pause();
    }

    public void play(){
        getMediaPlayer().start();
    }

    public void stop(){
        getMediaPlayer().stop();
    }

    public void release(){
        getMediaPlayer().release();
        mMediaPlayer = null;
    }

    public interface Callback{
        void playerPrepared(AvsItem pendingItem);
        void itemComplete(AvsItem completedItem);
        boolean playerError(int what, int extra);
        void dataError(Exception e);
    }

    private void bubbleUpError(Exception e){
        for(Callback callback: mCallbacks){
            callback.dataError(e);
        }
    }

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            for(Callback callback: mCallbacks){
                callback.itemComplete(mItem);
            }
        }
    };
    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            for(Callback callback: mCallbacks){
                callback.playerPrepared(mItem);
            }
            mMediaPlayer.start();
        }
    };
    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            for(Callback callback: mCallbacks){
                boolean response = callback.playerError(what, extra);
                if(response){
                    return response;
                }
            }
            return false;
        }
    };
}
