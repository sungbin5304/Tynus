package com.sungbin.fake.nusty.tynus.sinch

import com.sinch.android.rtc.AudioController
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.Sinch
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchClientListener
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.video.VideoController
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallClientListener

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.sungbin.fake.nusty.tynus.sinch.Sinch.APP_KEY
import com.sungbin.fake.nusty.tynus.sinch.Sinch.APP_SECRET
import com.sungbin.fake.nusty.tynus.sinch.Sinch.ENVIRONMENT

class SinchService : Service() {

    private val mSinchServiceInterface = SinchServiceInterface()
    private var mSinchClient: SinchClient? = null
    private var mUserId: String? = null

    private var mListener: StartFailedListener? = null

    private val isStarted: Boolean
        get() = mSinchClient != null && mSinchClient!!.isStarted

    override fun onDestroy() {
        if (mSinchClient != null && mSinchClient!!.isStarted) {
            mSinchClient!!.terminate()
        }
        super.onDestroy()
    }

    private fun start(userName: String) {
        if (mSinchClient == null) {
            mUserId = userName
            mSinchClient = Sinch.getSinchClientBuilder().context(applicationContext).userId(userName)
                    .applicationKey(APP_KEY)
                    .applicationSecret(APP_SECRET)
                    .environmentHost(ENVIRONMENT).build()

            mSinchClient!!.setSupportCalling(true)
            mSinchClient!!.startListeningOnActiveConnection()

            mSinchClient!!.addSinchClientListener(MySinchClientListener())
            mSinchClient!!.callClient.addCallClientListener(SinchCallClientListener())
            mSinchClient!!.start()
        }
    }

    private fun stop() {
        if (mSinchClient != null) {
            mSinchClient!!.terminate()
            mSinchClient = null
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mSinchServiceInterface
    }

    inner class SinchServiceInterface : Binder() {

        val userName: String?
            get() = mUserId

        val isStarted: Boolean
            get() = this@SinchService.isStarted

        val videoController: VideoController?
            get() = if (!isStarted) {
                null
            } else mSinchClient!!.videoController

        val audioController: AudioController?
            get() = if (!isStarted) {
                null
            } else mSinchClient!!.audioController

        fun callUserVideo(userId: String): Call {
            return mSinchClient!!.callClient.callUserVideo(userId)
        }

        fun startClient(userName: String) {
            start(userName)
        }

        fun stopClient() {
            stop()
        }

        fun setStartListener(listener: StartFailedListener) {
            mListener = listener
        }

        fun getCall(callId: String): Call {
            return mSinchClient!!.callClient.getCall(callId)
        }
    }

    interface StartFailedListener {

        fun onStartFailed(error: SinchError)

        fun onStarted()
    }

    private inner class MySinchClientListener : SinchClientListener {

        override fun onClientFailed(client: SinchClient, error: SinchError) {
            if (mListener != null) {
                mListener!!.onStartFailed(error)
            }
            mSinchClient!!.terminate()
            mSinchClient = null
        }

        override fun onClientStarted(client: SinchClient) {
            if (mListener != null) {
                mListener!!.onStarted()
            }
        }

        override fun onClientStopped(client: SinchClient) {
        }

        override fun onLogMessage(level: Int, area: String, message: String) {
            when (level) {
                Log.DEBUG -> Log.d(area, message)
                Log.ERROR -> Log.e(area, message)
                Log.INFO -> Log.i(area, message)
                Log.VERBOSE -> Log.v(area, message)
                Log.WARN -> Log.w(area, message)
            }
        }

        override fun onRegistrationCredentialsRequired(client: SinchClient,
                                                       clientRegistration: ClientRegistration) {
        }
    }

    private inner class SinchCallClientListener : CallClientListener {

        override fun onIncomingCall(callClient: CallClient, call: Call) {
            val intent = Intent(this@SinchService, IncomingCallScreenActivity::class.java)
            intent.putExtra(CALL_ID, call.callId)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this@SinchService.startActivity(intent)
        }
    }

    companion object {
        const val CALL_ID = "CALL_ID"
        internal val TAG = SinchService::class.java!!.getSimpleName()
    }

}
