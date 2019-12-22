@file:Suppress("DEPRECATION")

package com.sungbin.fake.nusty.tynus.sinch

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.sinch.android.rtc.SinchError
import com.sungbin.fake.nusty.tynus.R


class LoginActivity : BaseActivity(), SinchService.StartFailedListener {

    private var mLoginButton: Button? = null
    private var mLoginName: EditText? = null
    private var mSpinner: ProgressDialog? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE), 100)
        }

        mLoginName = findViewById(R.id.loginName)
        mLoginName!!.setText("CCTV")
        mLoginButton = findViewById(R.id.loginButton)
        mLoginButton!!.isEnabled = false
        mLoginButton!!.setOnClickListener { loginClicked() }
    }

    override fun onServiceConnected() {
        mLoginButton!!.isEnabled = true
        sinchServiceInterface!!.setStartListener(this)
        mLoginButton!!.performClick()
    }

    override fun onPause() {
        if (mSpinner != null) {
            mSpinner!!.dismiss()
        }
        super.onPause()
    }

    override fun onStartFailed(error: SinchError) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        if (mSpinner != null) {
            mSpinner!!.dismiss()
        }
    }

    override fun onStarted() {
        openPlaceCallActivity()
    }

    private fun loginClicked() {
        val userName = mLoginName!!.text.toString()

        if (userName.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show()
            return
        }

        if (!sinchServiceInterface!!.isStarted) {
            sinchServiceInterface!!.startClient(userName)
            showSpinner()
        } else {
            openPlaceCallActivity()
        }
    }

    private fun openPlaceCallActivity() {
        val mainActivity = Intent(this, PlaceCallActivity::class.java)
        startActivity(mainActivity)
    }

    private fun showSpinner() {
        mSpinner = ProgressDialog(this)
        mSpinner!!.setTitle("접속중...")
        mSpinner!!.setMessage("CCTV에 연결중 입니다...")
        mSpinner!!.show()
    }
}
