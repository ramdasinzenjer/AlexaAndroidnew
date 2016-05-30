package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * {@link com.willblaschko.android.alexa.data.Directive} to prompt the user for a speech input
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsExpectSpeechItem implements AvsItem {
    long timeoutInMiliseconds;

    public AvsExpectSpeechItem(){
        this(2000);
    }

    public AvsExpectSpeechItem(long timeoutInMiliseconds){
        this.timeoutInMiliseconds = timeoutInMiliseconds;
    }

    public long getTimeoutInMiliseconds() {
        return timeoutInMiliseconds;
    }
}
