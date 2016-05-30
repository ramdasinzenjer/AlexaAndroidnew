package com.willblaschko.android.alexa.interfaces.speechsynthesizer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

import java.io.ByteArrayInputStream;

/**
 * Directive to play a local, returned audio item from the Alexa post/get response
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsSpeakItem implements AvsItem {
    private String mCid;
    private ByteArrayInputStream mAudio;

    public AvsSpeakItem(String cid, ByteArrayInputStream audio) {
        mCid = cid;
        mAudio = audio;
    }

    public String getCid() {
        return mCid;
    }

    public ByteArrayInputStream getAudio() {
        return mAudio;
    }
}
