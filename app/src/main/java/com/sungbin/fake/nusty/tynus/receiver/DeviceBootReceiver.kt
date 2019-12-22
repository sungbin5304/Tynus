package com.sungbin.fake.nusty.tynus.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.shashank.sony.fancytoastlib.FancyToast
import com.sungbin.fake.nusty.tynus.service.StepCountingService
import com.sungbin.fake.nusty.tynus.utils.Utils
import java.util.*

@Suppress("DEPRECATION")
class DeviceBootReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Objects.equals(intent!!.action, "android.intent.action.BOOT_COMPLETED")) {
            Utils.toast(context!!,
                "날씨 알림과 만보기가 활성화 되었습니다.",
                FancyToast.LENGTH_LONG, FancyToast.SUCCESS)

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1)
            }

            val pm = context.packageManager
            val receiver = ComponentName(context, DeviceBootReceiver::class.java)
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, pendingIntent
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis, pendingIntent
                )
            }

            pm.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            context.startService(Intent(context, StepCountingService::class.java))
        }
    }
}