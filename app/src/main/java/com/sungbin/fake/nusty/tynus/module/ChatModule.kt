package com.sungbin.fake.nusty.tynus.module

import android.util.Log
import com.sungbin.fake.nusty.tynus.utils.Utils
import java.net.URLEncoder

class ChatModule {

    private var data: List<String>? = null
    private var input: String? = null

    val result: Array<String>?
        get() {
            val chats = ArrayList<String>()
            val input = this.input!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var max = 1
            for (n in 0 until data!!.size - 1) {
                val count = getCount(data!![n], input)
                if (count > max) {
                    chats.clear()
                    max = count
                }
                if (count == max) chats.add(data!![n + 1])
            }
            return if (chats.size == 0) null else chats.toArray(arrayOfNulls<String>(chats.size))
        }

    fun setData(data: List<String>) {
        this.data = data
    }

    fun inputChat(input: String) {
        this.input = input
    }

    private fun getCount(data: String, input: Array<String>): Int {
        var count = 0
        for (n in input.indices) {
            if (data.contains(input[n])) count++
        }
        return count
    }

    fun getReplyFromPingPong(text: String): String{
        val link = "https://builder.pingpong.us/api/builder/pingpong/chat/demo?query=${URLEncoder.encode(text, "UTF-8")}"
        val data = Utils.getHtml(link)!!.split("reply\":\"")[1].split("\",\"type")[0]
        return if(data.contains("\\")) data.split("\\")[0]
        else data
    }

}
