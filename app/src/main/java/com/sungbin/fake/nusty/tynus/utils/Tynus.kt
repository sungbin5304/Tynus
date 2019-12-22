package com.sungbin.fake.nusty.tynus.utils

import android.content.Context
import android.content.res.AssetManager
import com.sungbin.fake.nusty.tynus.module.ChatModule
import org.jsoup.Jsoup
import java.io.InputStream


object Tynus {
    fun readAsset(ctx: Context, name: String): String {
        val assetManager: AssetManager = ctx.assets
        val inputStream: InputStream = assetManager.open(name)
        return inputStream.bufferedReader().use { it.readText() }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getReply(data: List<String>, input: String): String? {
        /*val cm = ChatModule()
        cm.setData(data)
        cm.inputChat(input)
        val result = cm.result ?: return null
        val r = floor(Math.random() * result.size).toInt()
        return result[r]*/
        return ChatModule().getReplyFromPingPong(input)
    }

    fun getWeather(pos: String): String {
        var result = ""
        val thread = Thread {
            val main =
                Jsoup.connect("https://m.search.naver.com/search.naver?query=$pos%20날씨").get()
            val data = main.select("div.week_weather")[4]
            val data1 = data.select("div.wt_cast").select("li")
            val data2 = data.select("div.wt_temp")
            var m = 0
            val days = arrayOf("오늘", "내일", "모래", "글피")
            for (n in 0 until data2.size) run {
                result += if (n < 4) {
                    "[${days[n]}]\n"
                } else "[${n}일 뒤]\n"
                var cache = data2[n].text()
                cache = cache.replace("도", "℃,").replace("도", "℃").replace(" 기온 ", "")
                result += "기온 : $cache\n"
                cache = data1[m].text()
                if (n == 0) cache = cache.replace("오전", "")
                val cc = cache.split("퍼센트")
                result += "오전 : " + cc[1].trim() + " (강수확률" + cc[0] + "%)\n"
                cache = data1[m + 1].text()
                if (n == 0) cache = cache.replace("오후", "")
                val dd = cache.split("퍼센트")
                result += "오후 : " + dd[1].trim() + " (강수확률" + dd[0] + "%)\n\n"
                m += 2
            }
        }
        thread.start()
        thread.join()
        return result
    }
}