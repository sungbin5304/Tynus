package com.sungbin.fake.nusty.tynus.sinch

import com.sinch.android.rtc.PushPair
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.video.VideoCallListener

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.TextView
import com.sungbin.fake.nusty.tynus.R

class IncomingCallScreenActivity : BaseActivity() {
    private var mCallId: String? = null
    private var mAudioPlayer: AudioPlayer? = null

    private val mClickListener = OnClickListener { v ->
        when (v.id) {
            R.id.answerButton -> answerClicked()
            R.id.declineButton -> declineClicked()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.incoming)

        val answer = findViewById<Button>(R.id.answerButton)
        answer.setOnClickListener(mClickListener)
        val decline = findViewById<Button>(R.id.declineButton)
        decline.setOnClickListener(mClickListener)

        mAudioPlayer = AudioPlayer(this)
        mAudioPlayer!!.playRingtone()
        mCallId = intent.getStringExtra(SinchService.CALL_ID)
    }

    override fun onServiceConnected() {
        val call = sinchServiceInterface!!.getCall(mCallId!!)
        call.addCallListener(SinchCallListener())
        val remoteUser = findViewById<TextView>(R.id.remoteUser)
        remoteUser.text = call.remoteUserId

    }

    private fun answerClicked() {
        mAudioPlayer!!.stopRingtone()
        val call = sinchServiceInterface!!.getCall(mCallId!!)
        call.answer()
        val intent = Intent(this, CallScreenActivity::class.java)
        intent.putExtra(SinchService.CALL_ID, mCallId)
        startActivity(intent)
    }

    private fun declineClicked() {
        mAudioPlayer!!.stopRingtone()
        val call = sinchServiceInterface!!.getCall(mCallId!!)
        call.hangup()
        finish()
    }

    private inner class SinchCallListener : VideoCallListener {
        override fun onVideoTrackResumed(p0: Call?) {
        }

        override fun onVideoTrackPaused(p0: Call?) {
        }

        override fun onCallEnded(call: Call) {
            val cause = call.details.endCause
            mAudioPlayer!!.stopRingtone()
            finish()
        }

        override fun onCallEstablished(call: Call) {
        }

        override fun onCallProgressing(call: Call) {
        }

        override fun onShouldSendPushNotification(call: Call, pushPairs: List<PushPair>) {
        }

        override fun onVideoTrackAdded(call: Call) {
        }
    }
}
