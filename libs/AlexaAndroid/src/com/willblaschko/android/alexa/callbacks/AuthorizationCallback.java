package com.willblaschko.android.alexa.callbacks;

/**
 * Created by wblaschko on 8/13/15.
 */
public interface AuthorizationCallback {
    void onCancel();
    void onSuccess();
    void onError(Exception error);
}
