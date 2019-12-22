package com.sungbin.fake.nusty.tynus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sungbin.fake.nusty.tynus.service.MusicService
import com.sungbin.fake.nusty.tynus.utils.MusicUtils
import com.sungbin.fake.nusty.tynus.utils.NotificationManager

class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.getStringExtra("action")){
            "stop" -> MusicUtils.stop()
            "pause" -> MusicUtils.pause(true)
            "start" -> MusicUtils.pause(false)
            "cancel" -> {
                MusicUtils.init()
                context!!.stopService(Intent(context, MusicService::class.java))
            }
        }
    }
}