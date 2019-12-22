package com.sungbin.fake.nusty.tynus.sinch

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity(), ServiceConnection {

    protected var sinchServiceInterface: SinchService.SinchServiceInterface? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationContext.bindService(Intent(this, SinchService::class.java), this,
                BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        if (SinchService::class.java.name == componentName.className) {
            sinchServiceInterface = iBinder as SinchService.SinchServiceInterface
            onServiceConnected()
        }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        if (SinchService::class.java.name == componentName.className) {
            sinchServiceInterface = null
            onServiceDisconnected()
        }
    }

    protected open fun onServiceConnected() {
    }

    private fun onServiceDisconnected() {
    }

}
