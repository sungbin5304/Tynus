@file:Suppress("DEPRECATION")

package com.sungbin.fake.nusty.tynus.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationManagerCompat
import com.sungbin.fake.nusty.tynus.R
import com.sungbin.fake.nusty.tynus.view.activity.MainActivity
import com.sungbin.fake.nusty.tynus.receiver.FloatingWindowServiceStopReceiver
import com.sungbin.fake.nusty.tynus.receiver.MediaControlReceiver
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object NotificationManager {

    private var GROUP_NAME = "undefined"

    private val smallIcon: Int
        get() = R.drawable.icon

    fun setGroupName(name: String) {
        GROUP_NAME = name
    }

    fun createChannel(context: Context, name: String, description: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val group1 = NotificationChannelGroup(GROUP_NAME, GROUP_NAME)
            getManager(context).createNotificationChannelGroup(group1)

            val channelMessage =
                NotificationChannel(Channel.NAME, name, android.app.NotificationManager.IMPORTANCE_DEFAULT)
            channelMessage.description = description
            channelMessage.group = GROUP_NAME
            channelMessage.lightColor = R.color.colorAccent
            channelMessage.enableVibration(true)
            channelMessage.vibrationPattern = longArrayOf(0, 0)
            getManager(context).createNotificationChannel(channelMessage)
        }
    }

    private fun stopAction(context: Context): Notification.Action {
        val mStopAction: Notification.Action?
        val intent = Intent(context, FloatingWindowServiceStopReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mStopAction =
            Notification.Action(R.drawable.ic_layers_clear_white_24dp,
                context.getString(R.string.stop_floatiing_window), pendingIntent)
        return mStopAction
    }

    private fun getManager(context: Context): android.app.NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    }

    fun showMediaStyleNotification(context: Context, id: Int, title: String, content: String, bitmap: Bitmap?) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val mMusicAction1: Notification.Action?
        val intent1 = Intent(context, MediaControlReceiver::class.java)
        intent1.putExtra("action", "start")
        val pendingIntent1 = PendingIntent.getBroadcast(
            context, 1,
            intent1, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mMusicAction1 =
            Notification.Action(R.drawable.ic_play_arrow_gray_24dp,
                "play", pendingIntent1)

        val mMusicAction2: Notification.Action?
        val intent2 = Intent(context, MediaControlReceiver::class.java)
        intent2.putExtra("action", "stop")
        val pendingIntent2 = PendingIntent.getBroadcast(
            context, 2,
            intent2, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mMusicAction2 =
            Notification.Action(R.drawable.ic_stop_gray_24dp,
                "stop", pendingIntent2)

        val mMusicAction3: Notification.Action?
        val intent3 = Intent(context, MediaControlReceiver::class.java)
        intent3.putExtra("action", "pause")
        val pendingIntent3 = PendingIntent.getBroadcast(
            context, 3,
            intent3, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mMusicAction3 =
            Notification.Action(R.drawable.ic_pause_gray_24dp,
                "pause", pendingIntent3)

        val mMusicAction4: Notification.Action?
        val intent4 = Intent(context, MediaControlReceiver::class.java)
        intent4.putExtra("action", "cancel")
        val pendingIntent4 = PendingIntent.getBroadcast(
            context, 4,
            intent4, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mMusicAction4 =
            Notification.Action(R.drawable.ic_close_gray_24dp,
                "cancel", pendingIntent4)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val builder = Notification.Builder(context, Channel.NAME)
                .setContentTitle(content)
                .setContentText(title)
                .setSmallIcon(smallIcon)
                .setShowWhen(true)
                //.setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(mMusicAction2)
                .addAction(mMusicAction1)
                .addAction(mMusicAction3)
                .addAction(mMusicAction4)
                .setStyle(Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2))
            if(bitmap != null) builder.setLargeIcon(bitmap)
            getManager(context).notify(id, builder.build())
        } else {
            val builder = Notification.Builder(context)
                .setContentTitle(content)
                .setContentText(title)
                .setSmallIcon(smallIcon)
                .setShowWhen(true)
                //.setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(mMusicAction2)
                .addAction(mMusicAction1)
                .addAction(mMusicAction3)
                .addAction(mMusicAction4)
                .setStyle(Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2))
            if(bitmap != null) builder.setLargeIcon(bitmap)
            getManager(context).notify(id, builder.build())
        }
    }

    fun showNormalNotification(context: Context, id: Int, title: String, content: String, addAction: Boolean = true, isOnGoing: Boolean = true) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val builder = Notification.Builder(context, Channel.NAME)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOngoing(isOnGoing)
                .setContentIntent(pendingIntent)
                if(addAction) builder.addAction(stopAction(context))
            getManager(context).notify(id, builder.build())
        } else {
            val builder = Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setAutoCancel(true)
                .setOngoing(isOnGoing)
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
            if(addAction) builder.addAction(stopAction(context))
            getManager(context).notify(id, builder.build())
        }
    }

    fun showInboxStyleNotification(context: Context, id: Int, title: String, content: String, boxText: List<String>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val builder = Notification.Builder(context, Channel.NAME)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setAutoCancel(true)
            val inboxStyle = Notification.InboxStyle()
            inboxStyle.setBigContentTitle(title)
            inboxStyle.setSummaryText(content)

            for (str in boxText) {
                inboxStyle.addLine(str)
            }

            builder.style = inboxStyle

            getManager(context).notify(id, builder.build())
        } else {
            val builder = Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setAutoCancel(true)
            val inboxStyle = Notification.InboxStyle()
            inboxStyle.setBigContentTitle(title)
            inboxStyle.setSummaryText(content)

            for (str in boxText) {
                inboxStyle.addLine(str)
            }

            builder.style = inboxStyle

            getManager(context).notify(id, builder.build())
        }
    }

    fun deleteNotification(context: Context, id: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(id)
        }
        catch (e: java.lang.Exception){ }
    }

    annotation class Channel {
        companion object {
            const val NAME = "CHANNEL"
        }
    }

    private const val FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send"
    private const val SERVER_KEY =
        "AAAAbRF5XUs:APA91bHNTccVLqY_t99e4L1UhWADuDRBAMWDqYZzzPEmznkfmVmNVYalHkgjxkxdAaG99k56vtgkqmCtO7r--hzJUEqe2WfVe21xpVekAYuavEsk8IX1ql4quqcS-dC9c20fNKfw0NnL"

    fun sendNotiToFcm(title: String, message: String, to: String) {
        Thread(Runnable {
            try {
                val root = JSONObject()
                val notification = JSONObject()
                notification.put("body", message)
                notification.put("title", title)
                root.put("data", notification)
                root.put("to", "/topics/$to")

                val url = URL(FCM_MESSAGE_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.addRequestProperty("Authorization", "key=$SERVER_KEY")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Content-type", "application/json")
                val os = conn.outputStream
                os.write(root.toString().toByteArray(charset("utf-8")))
                os.flush()
                conn.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }
}
