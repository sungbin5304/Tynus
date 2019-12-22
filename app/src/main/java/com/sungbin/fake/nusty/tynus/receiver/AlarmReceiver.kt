package com.sungbin.fake.nusty.tynus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sungbin.fake.nusty.tynus.utils.NotificationManager
import com.sungbin.fake.nusty.tynus.utils.Tynus
import com.sungbin.fake.nusty.tynus.utils.Utils
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val weatherData = Tynus.getWeather(Utils.readData(
            context!!, "weather area",
            "서울")!!)
            .split("]\n")[1]
            .split("\n[")[0]
            .split("\n")
        val notice = when {
            weatherData.contains("비") -> "오늘은 비가 내릴 예정이니 우산을 챙겨주세요!"
            weatherData.contains("눈") -> "오늘은 눈이 내릴 예정이니 따듯하게 입고가세요!"
            else -> "오늘은 비나 눈 소식이 없어요 :)"
        }
        NotificationManager.showInboxStyleNotification(
            context,
            0,
            "오늘의 ${Utils.readData(
                context, "weather area",
                "서울")!!} 날씨",
            notice,
            weatherData
        )

        val nextNotifyTime = Calendar.getInstance()
        nextNotifyTime.add(Calendar.DATE, 1)

        val preStep = Utils.readData(context,
            "step", "0")!!.toInt()
        NotificationManager.showNormalNotification(context,
            555,
            "어제는 $preStep 걸음 걸으셨어요!",
            "오늘의 목표! ${preStep+100} 걸음",
            false,
            false
        )
        Utils.saveData(context,
            "gaolStep", "${preStep+100}")
        Utils.saveData(context,
            "step", "0")
    }

}