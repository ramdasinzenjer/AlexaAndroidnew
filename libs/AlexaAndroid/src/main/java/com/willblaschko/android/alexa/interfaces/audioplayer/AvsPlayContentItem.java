package com.willblaschko.android.alexa.interfaces.audioplayer;

import android.net.Uri;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Directive to play a local content item, this is not generated from the Alexa servers, this is for local
 * use only.
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsPlayContentItem implements AvsItem {
    private Uri mUri;

    /**
     * Create a new local play item
     * @param uri the local URI
     */
    public AvsPlayContentItem(Uri uri){
        mUri = uri;
    }
    public Uri getUri(){
        return mUri;
    }
}
