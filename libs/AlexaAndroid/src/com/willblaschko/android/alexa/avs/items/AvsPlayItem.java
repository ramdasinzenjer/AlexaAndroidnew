/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.willblaschko.android.alexa.avs.items;

public class AvsPlayItem implements AvsItem {
    private String mUrl;
    private String mStreamId;
    private int mStartOffset;
    private int mProgressStartOffset;
    private int mProgressInterval;

    public AvsPlayItem(String url, String streamId, int startOffset, int progressStartOffset,
                       int progressInterval) {
        mUrl = url;
        mStreamId = streamId;
        mStartOffset = (startOffset < 0) ? 0 : startOffset;
        mProgressStartOffset = progressStartOffset;
        mProgressInterval = progressInterval;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getStreamId() {
        return mStreamId;
    }

    public int getStartOffset() {
        return mStartOffset;
    }

    public int getProgressStartOffset() {
        return mProgressStartOffset;
    }

    public int getProgressInterval() {
        return mProgressInterval;
    }
}
