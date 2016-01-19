/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.willblaschko.android.alexa.avs.items;

import java.io.ByteArrayInputStream;

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
