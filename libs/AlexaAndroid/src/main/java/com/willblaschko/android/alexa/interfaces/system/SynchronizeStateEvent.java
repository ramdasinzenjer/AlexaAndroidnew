package com.willblaschko.android.alexa.interfaces.system;

import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.SendEvent;

import org.jetbrains.annotations.NotNull;

/**
 * Synchronize state {@link com.willblaschko.android.alexa.data.Event} to open a synchonize the state with the server
 * and get pending {@link com.willblaschko.android.alexa.data.Directive}
 *
 * {@link com.willblaschko.android.alexa.data.Event}
 *
 * @author will on 5/21/2016.
 */
public class SynchronizeStateEvent extends SendEvent {
    @NotNull
    @Override
    protected String getEvent() {
        return Event.getSynchronizeStateEvent();
    }
}
