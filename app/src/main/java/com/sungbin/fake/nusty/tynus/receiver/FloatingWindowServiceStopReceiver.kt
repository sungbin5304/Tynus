package com.sungbin.fake.nusty.tynus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sungbin.fake.nusty.tynus.service.FloatingWindowService
import com.sungbin.fake.nusty.tynus.utils.NotificationManager

class FloatingWindowServiceStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context!!.stopService(Intent(context, FloatingWindowService::class.java))
        NotificationManager.deleteNotification(context, 1)
    }
}