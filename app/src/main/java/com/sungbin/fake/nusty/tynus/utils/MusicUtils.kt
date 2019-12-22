package com.sungbin.fake.nusty.tynus.utils

import android.media.MediaPlayer
import java.lang.Exception

object MusicUtils {
    private val media = MediaPlayer()
    private var songPath: String? = null

    fun init(){
        try {
            media.reset()
        }
        catch (ignored: Exception){}
    }

    fun play(path: String){
        try {
            media.setDataSource(path)
            media.prepare()
            media.start()
            songPath = path
        }
        catch (ignored: Exception){}
    }

    fun isPlaying(): Boolean{
        return media.isPlaying
    }

    fun stop(){
        try {
            media.stop()
            media.reset()
            media.setDataSource(songPath)
            media.prepare()
        }
        catch (ignored: Exception){}
    }

    fun pause(stop: Boolean){
        try {
            if (stop) media.pause()
            else media.start()
        }
        catch (ignored: Exception){}
    }
}