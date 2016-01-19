package com.willblaschko.android.alexa.sender;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by wblaschko on 8/13/15.
 */
public class SendVoice extends SendData{
    private final static String TAG = "SendVoice";

    private AudioRecord mAudioRecord;

    private boolean mIsRecording = false;


    private static final int AUDIO_RATE = 16000;
    private static final int BUFFER_SIZE = 800;


    public void startRecording(Context context, String url, String accessToken, @Nullable byte[] buffer, @Nullable AsyncCallback<Void, Exception> callback) throws IOException {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        mCallback = callback;
        mIsRecording = true;

        preparePost(url, accessToken);

        if(buffer != null){
            mOutputStream.write(buffer);
        }

        //record our audio
        recordAudio(mAudioRecord, mOutputStream);
    }

    public AvsResponse stopRecording() throws IOException, AvsException {
        mIsRecording = false;
        if(mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
        return completePost();
    }






    private void recordAudio(final AudioRecord audioRecord, final OutputStream outputStream){
        audioRecord.startRecording();
        if(outputStream == null){
            mIsRecording = false;
            return;
        }

        Thread recordingThread = new Thread(new Runnable() {
            public void run() {
                byte[] data = new byte[BUFFER_SIZE];
                while(mIsRecording) {
                    int count = audioRecord.read(data, 0, BUFFER_SIZE);
                    try{
                        outputStream.write(data, 0, count);
                        outputStream.flush();
                    }catch (IOException e){
                        e.printStackTrace();
                        mIsRecording = false;
                        if(mCallback != null){
                            mCallback.failure(e);
                        }
                    }catch (NullPointerException e){
                        e.printStackTrace();
                        mIsRecording = false;
                        if(mCallback != null){
                            mCallback.failure(e);
                        }
                    }
                }

            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }




}
