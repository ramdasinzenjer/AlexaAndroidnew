package com.willblaschko.android.alexavoicelibrary.recommendation;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by willb_000 on 12/29/2015.
 */
public class RecommendationBuilder {

    String mTitle;
    String mDescription;
    String mImage;
    String mLargeImage;
    String mBackground;
    PendingIntent mIntent;
    int mPriority = NotificationCompat.PRIORITY_DEFAULT;

    public RecommendationBuilder(){

    }

    public RecommendationBuilder setTitle(String title) {
        mTitle = title;
        return this;
    }

    public RecommendationBuilder setDescription(String description) {
        mDescription = description;
        return this;
    }

    public RecommendationBuilder setImage(String uri) {
        mImage = uri;
        return this;
    }

    public RecommendationBuilder setLargeImage(String uri) {
        mLargeImage = uri;
        return this;
    }


    public RecommendationBuilder setBackground(String uri) {
        mBackground = uri;
        return this;
    }
    public RecommendationBuilder setIntent(PendingIntent intent){
        mIntent = intent;
        return this;
    }

    public RecommendationBuilder setPriority(int priority){
        mPriority = priority;
        return this;
    }

    public Notification build(Context context) throws IOException {

        Notification notification = new NotificationCompat.BigPictureStyle(
                new NotificationCompat.Builder(context)
                        .setContentTitle(mTitle)
                        .setContentText(mDescription)
                        .setPriority(mPriority)
                        .setLocalOnly(true)
                        .setOngoing(true)
                        .setCategory(Notification.CATEGORY_RECOMMENDATION)
                        .setLargeIcon(getBitmapFromURL(mLargeImage))
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .setContentIntent(mIntent))
                .build();

        return notification;
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }
}
