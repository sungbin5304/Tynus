package com.sungbin.fake.nusty.tynus.recorder.utils;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.text.format.DateUtils;
import com.sungbin.fake.nusty.tynus.R;

import static android.os.Build.VERSION_CODES.O;
import static com.sungbin.fake.nusty.tynus.recorder.activity.RecodeActivity.ACTION_STOP;


public class Notifications extends ContextWrapper {
    private static final int id = 0x1fff;

    private long mLastFiredTime = 0;
    private NotificationManager mManager;
    private Notification.Action mStopAction;
    private Notification.Builder mBuilder;
    private Context ctx;

    public Notifications(Context context) {
        super(context);
        ctx = context;
        if (Build.VERSION.SDK_INT >= O) {
            createNotificationChannel();
        }
    }

    public void recording(long timeMs) {
        if (SystemClock.elapsedRealtime() - mLastFiredTime < 1000) {
            return;
        }
        Notification notification = getBuilder()
                .setContentText("길이: " + DateUtils.formatElapsedTime(timeMs / 1000))
                .build();
        getNotificationManager().notify(id, notification);
        mLastFiredTime = SystemClock.elapsedRealtime();
    }

    private Notification.Builder getBuilder() {
        if (mBuilder == null) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(ctx.getString(R.string.string_recoding))
                    .setOngoing(true)
                    .setLocalOnly(true)
                    .setOnlyAlertOnce(true)
                    .addAction(stopAction())
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_cast_white_24dp);
            if (Build.VERSION.SDK_INT >= O) {
                builder.setChannelId(ctx.getString(R.string.string_recode))
                        .setUsesChronometer(true);
            }
            mBuilder = builder;
        }
        return mBuilder;
    }

    @TargetApi(O)
    private void createNotificationChannel() {
        NotificationChannel channel =
                new NotificationChannel(ctx.getString(R.string.string_recode),
                        ctx.getString(R.string.notification_screen_recode),
                        NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        getNotificationManager().createNotificationChannel(channel);
    }

    private Notification.Action stopAction() {
        if (mStopAction == null) {
            Intent intent = new Intent(ACTION_STOP).setPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1,
                    intent, PendingIntent.FLAG_ONE_SHOT);
            mStopAction = new Notification.Action(android.R.drawable.ic_media_pause, ctx.getString(R.string.string_stop), pendingIntent);
        }
        return mStopAction;
    }

    public void clear() {
        mLastFiredTime = 0;
        mBuilder = null;
        mStopAction = null;
        getNotificationManager().cancelAll();
    }

    NotificationManager getNotificationManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }
}
