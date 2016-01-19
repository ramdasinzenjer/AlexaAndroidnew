/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.willblaschko.android.alexa.avs;

@SuppressWarnings("serial")
public class AvsException extends Exception {

    public AvsException() {
    }

    public AvsException(String message) {
        super(message);
    }

    public AvsException(Throwable cause) {
        super(cause);
    }

    public AvsException(String message, Throwable cause) {
        super(message, cause);
    }



}
