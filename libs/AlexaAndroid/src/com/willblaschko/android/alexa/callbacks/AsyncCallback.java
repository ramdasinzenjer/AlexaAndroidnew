package com.willblaschko.android.alexa.callbacks;

/**
 * Created by wblaschko on 12/4/14.
 */
public interface AsyncCallback<D, E>{
    void start();
    void success(D result);
    void failure(E error);
    void complete();
}
