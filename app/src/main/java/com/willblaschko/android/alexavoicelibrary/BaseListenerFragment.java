package com.willblaschko.android.alexavoicelibrary;

import android.support.v4.app.Fragment;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Created by will on 5/30/2016.
 */

public abstract class BaseListenerFragment extends Fragment {


    protected AsyncCallback<AvsResponse, Exception> getRequestCallback(){
        if(getActivity() != null && getActivity() instanceof AvsListenerInterface){
            return ((AvsListenerInterface) getActivity()).getRequestCallback();
        }
        return null;
    }

    public abstract void startListening();

    public interface AvsListenerInterface{
        public AsyncCallback<AvsResponse, Exception> getRequestCallback();
    }
}
