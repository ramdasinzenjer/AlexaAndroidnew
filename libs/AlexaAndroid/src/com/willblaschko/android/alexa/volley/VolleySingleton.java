package com.willblaschko.android.alexa.volley;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by wblaschko on 2/28/15.
 */
public class VolleySingleton {
    private RequestQueue mRequestQueue;
    private static VolleySingleton mInstance;

    public final static String DEFAULT_REQUEST_TAG = "POC";

    Context mContext;

    private VolleySingleton(Context context) {
        mContext = context.getApplicationContext();
        mRequestQueue = getRequestQueue();

    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        return mRequestQueue;
    }


    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void cancelRequests(String tag){
        getRequestQueue().cancelAll(tag);
    }

}
