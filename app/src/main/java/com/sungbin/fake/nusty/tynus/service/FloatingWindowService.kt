package com.sungbin.fake.nusty.tynus.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sungbin.fake.nusty.tynus.R
import com.sungbin.fake.nusty.tynus.utils.NotificationManager
import com.sungbin.fake.nusty.tynus.utils.Utils
import kotlin.math.ceil

@Suppress("DEPRECATION")
class FloatingWindowService : Service() {
    private var mManager: WindowManager? = null
    private var layout: LinearLayout? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        try {
            val mParams = WindowManager.LayoutParams(
                dip2px(90), dip2px(120),
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }else{
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            layout = LinearLayout(this)
            layout!!.orientation = LinearLayout.VERTICAL
            val scroll = ScrollView(this)
            val title = TextView(this)
            title.text = getString(R.string.app_name)
            title.setTextColor(resources.getColor(R.color.colorAccent))
            title.textSize = 17f
            title.gravity = Gravity.CENTER
            val pad = dip2px(3)
            title.setPadding(pad, pad, pad, pad)
            title.setOnClickListener {
                try {
                    if (layout!!.childCount == 1) { //피는거
                        layout!!.addView(scroll)
                        mParams.width = dip2px(90)
                        mParams.height = dip2px(120)
                        mManager!!.updateViewLayout(layout, mParams)
                    } else { //접는거
                        layout!!.removeView(scroll)
                        mParams.width = -2
                        mParams.height = -2
                        mManager!!.updateViewLayout(layout, mParams)
                    }
                } catch (e: Exception) {
                    Utils.error(this, e, "onCreate - 2")
                }
            }
            val longClick = booleanArrayOf(false) //드래그 부분
            title.setOnTouchListener { _, ev ->
                if (longClick[0]) {
                    when (ev.action) {
                        MotionEvent.ACTION_UP -> longClick[0] = false
                        MotionEvent.ACTION_MOVE -> {
                            mParams.x = ev.rawX.toInt() - dip2px(45)
                            mParams.y = ev.rawY.toInt() - dip2px(50)
                            mParams.gravity = Gravity.START or Gravity.TOP
                            mManager!!.updateViewLayout(layout, mParams)
                        }
                    }
                } else if (ev.action == MotionEvent.ACTION_DOWN) {
                    Handler().postDelayed({ if (!longClick[0]) longClick[0] = true }, 100)
                }
                false
            }
            layout!!.addView(title)
            val layout2 = LinearLayout(this)
            layout2.orientation = LinearLayout.VERTICAL
            val menus = arrayOf("명령어", "길찾기", "앱 실행", "브라우저", "설정", "닫기")
            val txts = arrayOfNulls<TextView>(menus.size)
            for (n in menus.indices) {
                txts[n] = TextView(this)
                txts[n]!!.text = menus[n]
                txts[n]!!.setTextColor(Color.WHITE)
                txts[n]!!.textSize = 14f
                txts[n]!!.id = n
                txts[n]!!.gravity = Gravity.CENTER
                txts[n]!!.setOnClickListener { v ->
                    try {
                        when (v.id) {
                            0 //명령어
                            -> {
                            }
                            1 //길찾기
                            -> {
                            }
                            2 //앱 실행
                            -> {
                            }
                            3 //브라우저
                            -> {
                            }
                            4 //설정
                            -> {
                            }
                            5 //닫기
                            ->{
                                NotificationManager.deleteNotification(applicationContext, 1)
                                stopService(Intent(this, FloatingWindowService::class.java))
                            }
                        }
                    } catch (e: Exception) {
                        Utils.error(this, e, "onCreate")
                    }
                }
                layout2.addView(txts[n])
            }
            scroll.addView(layout2)
            layout!!.addView(scroll)
            layout!!.setBackgroundColor(Color.argb(80, 10, 10, 10))

            mManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            mManager!!.addView(layout, mParams)
        } catch (e: Exception) {
            Utils.error(this, e, "onCreate - 3")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mManager!!.removeView(layout)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun dip2px(dips: Int): Int {
        return ceil((dips * resources.displayMetrics.density).toDouble()).toInt()
    }

}
