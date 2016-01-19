package com.willblaschko.android.alexa;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Random;

/**
 * Created by wblaschko on 8/13/15.
 */
public class Util {
    private static SharedPreferences mPreferences;

    /**
     * Show an authorization toast on the main thread to make sure the user sees it
     * @param context local context
     * @param message the message to show the user
     */
    public static void showAuthToast(final Context context, final String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast authToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                authToast.show();
            }
        });
    }


    /**
     * Get our default shared preferences
     * @param context local/application context
     * @return default shared preferences
     */
    public static SharedPreferences getPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return mPreferences;
    }


    private final static char[] MULTIPART_CHARS =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    .toCharArray();

    /**
     * Generate a randomized multipart boundary for our URL POSTs
     * @return the generated boundary
     */
    public static String generateMultipartBoundary() {
        StringBuilder buffer = new StringBuilder();
        Random rand = new Random();
        int count = rand.nextInt(11) + 30; // a random size from 30 to 40
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

}
