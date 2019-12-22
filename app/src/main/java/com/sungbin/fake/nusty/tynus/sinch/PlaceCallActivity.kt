package com.sungbin.fake.nusty.tynus.sinch

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.sungbin.fake.nusty.tynus.R

class PlaceCallActivity : BaseActivity() {

    private var mCallButton: Button? = null
    private var mCallName: EditText? = null


    private val buttonClickListener = OnClickListener { v ->
        when (v.id) {
            R.id.callButton -> callButtonClicked()
            R.id.stopButton -> stopButtonClicked()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        mCallName = findViewById(R.id.callName)
        mCallName!!.setText("CCTV")
        mCallButton = findViewById(R.id.callButton)
        mCallButton!!.isEnabled = false
        mCallButton!!.setOnClickListener(buttonClickListener)

        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnClickListener(buttonClickListener)
    }

    override fun onServiceConnected() {
        val userName = findViewById<TextView>(R.id.loggedInName)
        userName.text = sinchServiceInterface!!.userName
        mCallButton!!.isEnabled = true

        Handler().postDelayed({
            mCallButton!!.performClick()
        }, 3000)
    }

    public override fun onDestroy() {
        if (sinchServiceInterface != null) {
            sinchServiceInterface!!.stopClient()
        }
        super.onDestroy()
    }

    private fun stopButtonClicked() {
        if (sinchServiceInterface != null) {
            sinchServiceInterface!!.stopClient()
        }
        finish()
    }

    private fun callButtonClicked() {
        val userName = mCallName!!.text.toString()
        if (userName.isEmpty()) {
            Toast.makeText(this, "Please enter a user to call", Toast.LENGTH_LONG).show()
            return
        }

        val call = sinchServiceInterface!!.callUserVideo(userName)
        val callId = call.callId

        val callScreen = Intent(this, CallScreenActivity::class.java)
        callScreen.putExtra(SinchService.CALL_ID, callId)
        startActivity(callScreen)
    }
}
