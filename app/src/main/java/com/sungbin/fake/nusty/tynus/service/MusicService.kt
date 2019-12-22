package com.sungbin.fake.nusty.tynus.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.sungbin.fake.nusty.tynus.R
import com.sungbin.fake.nusty.tynus.receiver.MediaControlReceiver
import com.sungbin.fake.nusty.tynus.utils.MusicUtils
import com.sungbin.fake.nusty.tynus.utils.NotificationManager
import com.sungbin.fake.nusty.tynus.view.activity.MainActivity

@Suppress("DEPRECATION")
class MusicService : Service() {
    var media: MediaPlayer? = null
    override fun onStartCommand(dataIntent: Intent, flags: Int, startId: Int): Int {
        try{
            val context = applicationContext
            val path = dataIntent.getStringExtra("path")
            val title = getMp3Data(path, MediaMetadataRetriever.METADATA_KEY_TITLE)!!
            val content = getMp3Data(path, MediaMetadataRetriever.METADATA_KEY_ARTIST)!!
            val bitmap = getAlbemCoverFromMp3(path)

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
                Notification.Action(
                    R.drawable.ic_play_arrow_gray_24dp,
                    "play", pendingIntent1)

            val mMusicAction2: Notification.Action?
            val intent2 = Intent(context, MediaControlReceiver::class.java)
            intent2.putExtra("action", "stop")
            val pendingIntent2 = PendingIntent.getBroadcast(
                context, 2,
                intent2, PendingIntent.FLAG_UPDATE_CURRENT
            )
            mMusicAction2 =
                Notification.Action(
                    R.drawable.ic_stop_gray_24dp,
                    "stop", pendingIntent2)

            val mMusicAction3: Notification.Action?
            val intent3 = Intent(context, MediaControlReceiver::class.java)
            intent3.putExtra("action", "pause")
            val pendingIntent3 = PendingIntent.getBroadcast(
                context, 3,
                intent3, PendingIntent.FLAG_UPDATE_CURRENT
            )
            mMusicAction3 =
                Notification.Action(
                    R.drawable.ic_pause_gray_24dp,
                    "pause", pendingIntent3)

            val mMusicAction4: Notification.Action?
            val intent4 = Intent(context, MediaControlReceiver::class.java)
            intent4.putExtra("action", "cancel")
            val pendingIntent4 = PendingIntent.getBroadcast(
                context, 4,
                intent4, PendingIntent.FLAG_UPDATE_CURRENT
            )
            mMusicAction4 =
                Notification.Action(
                    R.drawable.ic_close_gray_24dp,
                    "cancel", pendingIntent4)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val builder = Notification.Builder(context, NotificationManager.Channel.NAME)
                    .setContentTitle(content)
                    .setContentText(title)
                    .setSmallIcon(R.drawable.icon)
                    .setShowWhen(true)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .addAction(mMusicAction2)
                    .addAction(mMusicAction1)
                    .addAction(mMusicAction3)
                    .addAction(mMusicAction4)
                    .setStyle(Notification.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                if(bitmap != null) builder.setLargeIcon(bitmap)
                startForeground(1, builder.build())
            } else {
                val builder = Notification.Builder(context)
                    .setContentTitle(content)
                    .setContentText(title)
                    .setSmallIcon(R.drawable.icon)
                    .setShowWhen(true)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .addAction(mMusicAction2)
                    .addAction(mMusicAction1)
                    .addAction(mMusicAction3)
                    .addAction(mMusicAction4)
                    .setStyle(Notification.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                if(bitmap != null) builder.setLargeIcon(bitmap)
                startForeground(1, builder.build())
            }

            MusicUtils.play(path)
        } catch (e: Exception) {
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicUtils.init()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun getAlbemCoverFromMp3(path: String): Bitmap?{
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)

        val data = mmr.embeddedPicture
        return if(data != null) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } else null
    }

    private fun getMp3Data(path: String, data: Int): String?{
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)

        return mmr.extractMetadata(data)
    }
}