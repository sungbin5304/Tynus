package com.sungbin.fake.nusty.tynus.sinch

import android.annotation.SuppressLint
import com.sinch.android.rtc.PushPair
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallState
import com.sinch.android.rtc.video.VideoCallListener

import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.sungbin.fake.nusty.tynus.R
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class CallScreenActivity : BaseActivity() {

    private var mAudioPlayer: AudioPlayer? = null
    private var mTimer: Timer? = null
    private var mDurationTask: UpdateCallDurationTask? = null

    private var mCallId: String? = null
    private var mCallStart: Long = 0
    private var mAddedListener = false
    private var mVideoViewsAdded = false

    private var mCallDuration: TextView? = null
    private var mCallState: TextView? = null
    private var mCallerName: TextView? = null

    private inner class UpdateCallDurationTask : TimerTask() {

        override fun run() {
            this@CallScreenActivity.runOnUiThread { updateCallDuration() }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putLong(CALL_START_TIME, mCallStart)
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mCallStart = savedInstanceState.getLong(CALL_START_TIME)
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.callscreen)

        mAudioPlayer = AudioPlayer(this)
        mCallDuration = findViewById(R.id.callDuration)
        mCallerName = findViewById(R.id.remoteUser)
        mCallState = findViewById(R.id.callState)
        val endCallButton = findViewById<Button>(R.id.hangupButton)

        endCallButton.setOnClickListener { endCall() }

        mCallId = intent.getStringExtra(SinchService.CALL_ID)
        if (savedInstanceState == null) {
            mCallStart = System.currentTimeMillis()
        }
    }

    public override fun onServiceConnected() {
        val call = sinchServiceInterface!!.getCall(mCallId!!)
        if (!mAddedListener) {
            call.addCallListener(SinchCallListener())
            mAddedListener = true
        }

        updateUI()
    }

    private fun updateUI() {
        if (sinchServiceInterface == null) {
            return
        }

        val call = sinchServiceInterface!!.getCall(mCallId!!)
        mCallerName!!.text = call.remoteUserId
        mCallState!!.text = call.state.toString()
        if (call.state == CallState.ESTABLISHED) {
            addVideoViews()
        }
    }

    public override fun onStop() {
        super.onStop()
        mDurationTask!!.cancel()
        mTimer!!.cancel()
        removeVideoViews()
    }

    public override fun onStart() {
        super.onStart()
        mTimer = Timer()
        mDurationTask = UpdateCallDurationTask()
        mTimer!!.schedule(mDurationTask, 0, 500)
        updateUI()
    }

    override fun onBackPressed() {
    }

    private fun endCall() {
        mAudioPlayer!!.stopProgressTone()
        val call = sinchServiceInterface!!.getCall(mCallId!!)
        call.hangup()
        finish()
    }

    private fun formatTimespan(timespan: Long): String {
        val totalSeconds = timespan / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun updateCallDuration() {
        if (mCallStart > 0) {
            mCallDuration!!.text = formatTimespan(System.currentTimeMillis() - mCallStart)
        }
    }

    private fun addVideoViews() {
        if (mVideoViewsAdded || sinchServiceInterface == null) {
            return
        }

        val vc = sinchServiceInterface!!.videoController
        if (vc != null) {
            val localView = findViewById<RelativeLayout>(R.id.localVideo)
            localView.addView(vc.localView)

            localView.setOnClickListener {
                vc.toggleCaptureDevicePosition()
            }

            val view = findViewById<LinearLayout>(R.id.remoteVideo)
            view.addView(vc.remoteView)
            mVideoViewsAdded = true
        }
    }

    private fun removeVideoViews() {
        if (sinchServiceInterface == null) {
            return
        }

        val vc = sinchServiceInterface!!.videoController
        if (vc != null) {
            val view = findViewById<LinearLayout>(R.id.remoteVideo)
            view.removeView(vc.remoteView)

            val localView = findViewById<RelativeLayout>(R.id.localVideo)
            localView.removeView(vc.localView)
            mVideoViewsAdded = false
        }
    }

    private inner class SinchCallListener : VideoCallListener {
        override fun onVideoTrackPaused(p0: Call?) {
        }

        override fun onVideoTrackResumed(p0: Call?) {
        }

        override fun onCallEnded(call: Call) {
            val cause = call.details.endCause
            mAudioPlayer!!.stopProgressTone()
            volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
            val endMsg = "Call ended: " + call.details.toString()
            Toast.makeText(this@CallScreenActivity, endMsg, Toast.LENGTH_LONG).show()

            endCall()
        }

        override fun onCallEstablished(call: Call) {
            mAudioPlayer!!.stopProgressTone()
            mCallState!!.text = "CCTV 연결됨"
            volumeControlStream = AudioManager.STREAM_VOICE_CALL
            val audioController = sinchServiceInterface!!.audioController
            audioController!!.enableSpeaker()
            mCallStart = System.currentTimeMillis()
        }

        override fun onCallProgressing(call: Call) {
            mAudioPlayer!!.playProgressTone()
        }

        override fun onShouldSendPushNotification(call: Call, pushPairs: List<PushPair>) {
        }

        override fun onVideoTrackAdded(call: Call) {
            addVideoViews()
        }
    }

    companion object {

        internal val TAG = CallScreenActivity::class.java.simpleName
        internal const val CALL_START_TIME = "callStartTime"
        internal const val ADDED_LISTENER = "addedListener"
    }
}
