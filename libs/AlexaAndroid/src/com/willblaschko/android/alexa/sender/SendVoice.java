package com.willblaschko.android.alexa.sender;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A subclass of SendData that sends a recorded raw audio to the AVS server, unlike SendText, this does not use an intermediary steps, but the starting/stopping
 * need to be handled manually or programmatically, which Google's voice offering already does automatically (listens for speech thresholds).
 *
 * This is the class that should be used, ideally, instead of SendText, but it offers a little less flexibility as a standalone.
 *
 * Using the byte[] buffer in startRecording(), it's possible to prepend any audio recorded with pre-recorded or generated audio, in order to simplify or complicate the command.
 */
public class SendVoice extends SendData{
    private final static String TAG = "SendVoice";

    private AudioRecord mAudioRecord;

    private boolean mIsRecording = false;

    private Object mLock = new Object();

    private static final int AUDIO_RATE = 16000;
    private static final int BUFFER_SIZE = 800;

    /**
     * The trigger to open a new AudioRecord and start recording with the intention of sending the audio to the AVS server using the stopRecord(). This will have permissions
     * issues in Marshmallow that need to be handled at the Activity level (checking for permission to record audio, and requesting it if we don't already have permissions).
     * @param context local/application context
     * @param url our POST url
     * @param accessToken our user's access token
     * @param buffer a byte[] that allows us to prepend whatever audio is recorded by the user with either generated ore pre-recorded audio, this needs
     *               to be in the same format as the audio being recorded
     * @param callback our callback to notify us when we change states
     * @throws IOException
     */
    public void startRecording(Context context, String url, String accessToken, @Nullable byte[] buffer, @Nullable AsyncCallback<Void, Exception> callback) throws IOException {
        synchronized(mLock) {
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        }
        mCallback = callback;
        mIsRecording = true;

        preparePost(url, accessToken);

        if(buffer != null){
            mOutputStream.write(buffer);
        }

        //record our audio
        recordAudio(mAudioRecord, mOutputStream);
    }

    /**
     * Stop recording the user's audio and send the request to the AVS server, parse the response
     * @return the parsed response from the AVS server based on the recorded audio
     * @throws IOException
     * @throws AvsException
     */
    public AvsResponse stopRecording() throws IOException, AvsException {
        mIsRecording = false;
        synchronized (mLock) {
            if(mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
            }
        }
        return completePost();
    }


    /**
     * Record audio using AudioRecord and our outputStream, but do it using a thread so we don't lock up whatever
     * thread the function was called on
     * @param audioRecord our AudioRecord native
     * @param outputStream our HttpURLConnection outputstream (from SendData class)
     */
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
                    int count = -99;
                    synchronized (mLock) {
                        count = audioRecord.read(data, 0, BUFFER_SIZE);
                    }
                    if (count <= 0) {
                        Log.e(TAG, "audio read fail, error code:" + count);
                        mIsRecording = false;
                        if(mCallback != null){
                            mCallback.failure(new RuntimeException("audio read fail, error code:" + count));
                        }
                        break;
                    }
                    try{
                        outputStream.write(data, 0, count);
                        outputStream.flush();
                    }catch (IOException e){
                        e.printStackTrace();
                        mIsRecording = false;
                        if(mCallback != null){
                            mCallback.failure(e);
                            mCallback.complete();
                        }
                    }catch (NullPointerException e){
                        e.printStackTrace();
                        mIsRecording = false;
                        if(mCallback != null){
                            mCallback.failure(e);
                            mCallback.complete();
                        }
                    }
                }

            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }




}
