package com.willblaschko.android.alexavoicelibrary.recommendation;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.TaskStackBuilder;

import com.willblaschko.android.alexavoicelibrary.VoiceLaunchActivity;

import java.io.IOException;

/**
 * Created by will on 3/6/2016.
 */
public class NotificationHelper {
    public static void createNotification(Context context){
        if(!isDirectToTV(context)){ return; }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

            NotificationManager mNotificationManager  = (NotificationManager)
                context.getSystemService(Activity.NOTIFICATION_SERVICE);

        RecommendationBuilder builder = new RecommendationBuilder();

        try {
            Notification notification = builder.setPriority(Notification.PRIORITY_MAX)
                    .setTitle("Alexa")
                    .setDescription("Voice Command")
                    .setIntent(buildPendingIntent(context))
                        .build(context);

                mNotificationManager.notify(1, notification);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isDirectToTV(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                    || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK));
        }
        return false;
    }


    private static PendingIntent buildPendingIntent(Context context) {
        Intent launchIntent = new Intent(context, VoiceLaunchActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(launchIntent);
        PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        return intent;
    }
}
