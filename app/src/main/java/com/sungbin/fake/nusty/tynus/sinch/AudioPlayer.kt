@file:Suppress("DEPRECATION")

package com.sungbin.fake.nusty.tynus.sinch

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.sungbin.fake.nusty.tynus.R

import java.io.IOException

class AudioPlayer(context: Context) {

    private val mContext: Context = context.applicationContext
    private var mPlayer: MediaPlayer? = null
    private var mProgressTone: AudioTrack? = null

    fun playRingtone() {
        val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                mPlayer = MediaPlayer()
                mPlayer!!.setAudioStreamType(AudioManager.STREAM_RING)

                try {
                    mPlayer!!.setDataSource(mContext,
                            Uri.parse("android.resource://" + mContext.packageName + "/" + R.raw.phone_loud1))
                    mPlayer!!.prepare()
                } catch (e: IOException) {
                    mPlayer = null
                    return
                }

                mPlayer!!.isLooping = true
                mPlayer!!.start()
            }
        }
    }

    fun stopRingtone() {
        if (mPlayer != null) {
            mPlayer!!.stop()
            mPlayer!!.release()
            mPlayer = null
        }
    }

    fun playProgressTone() {
        stopProgressTone()
        try {
            mProgressTone = createProgressTone(mContext)
            mProgressTone!!.play()
        } catch (e: Exception) {
        }

    }

    fun stopProgressTone() {
        if (mProgressTone != null) {
            mProgressTone!!.stop()
            mProgressTone!!.release()
            mProgressTone = null
        }
    }

    companion object {
        internal val LOG_TAG = AudioPlayer::class.java.simpleName
        private const val SAMPLE_RATE = 16000

        @Throws(IOException::class)
        private fun createProgressTone(context: Context): AudioTrack {
            val fd = context.resources.openRawResourceFd(R.raw.progress_tone)
            val length = fd.length.toInt()

            val audioTrack = AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, length, AudioTrack.MODE_STATIC)

            val data = ByteArray(length)
            readFileToBytes(fd, data)

            audioTrack.write(data, 0, data.size)
            audioTrack.setLoopPoints(0, data.size / 2, 30)

            return audioTrack
        }

        @Throws(IOException::class)
        private fun readFileToBytes(fd: AssetFileDescriptor, data: ByteArray) {
            val inputStream = fd.createInputStream()

            var bytesRead = 0
            while (bytesRead < data.size) {
                val res = inputStream.read(data, bytesRead, data.size - bytesRead)
                if (res == -1) {
                    break
                }
                bytesRead += res
            }
        }
    }
}
