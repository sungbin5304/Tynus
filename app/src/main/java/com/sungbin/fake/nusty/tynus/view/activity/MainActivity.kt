package com.sungbin.fake.nusty.tynus.view.activity

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.text.Html
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.shashank.sony.fancytoastlib.FancyToast
import com.sungbin.fake.nusty.tynus.R
import com.sungbin.fake.nusty.tynus.adapter.ChatAdapter
import com.sungbin.fake.nusty.tynus.dto.ChatItem
import com.sungbin.fake.nusty.tynus.receiver.AlarmReceiver
import com.sungbin.fake.nusty.tynus.receiver.DeviceBootReceiver
import com.sungbin.fake.nusty.tynus.recorder.activity.RecodeActivity
import com.sungbin.fake.nusty.tynus.service.FloatingWindowService
import com.sungbin.fake.nusty.tynus.service.MusicService
import com.sungbin.fake.nusty.tynus.service.StepCountingService
import com.sungbin.fake.nusty.tynus.sinch.LoginActivity
import com.sungbin.fake.nusty.tynus.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION", "NAME_SHADOWING", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "StaticFieldLeak")
class MainActivity : AppCompatActivity() {

    private var i: Intent? = null
    private var recognizer: SpeechRecognizer? = null
    private var adapter: ChatAdapter? = null
    private var items: ArrayList<ChatItem>? = null
    private var chatData: List<String>? = null
    private var tts: TextToSpeech? = null
    private var randomInt: Long? = null
    private val allMp3Path: Array<String?>?
        get() {
            val resultPath: Array<String?>
            val selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?"
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
            val selectionArgsMp3 = arrayOf(mimeType)
            val c: Cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.DATA),
                selectionMimeType,
                selectionArgsMp3,
                null
            )
            if (c.count == 0) return null
            resultPath = arrayOfNulls(c.count)
            while (c.moveToNext()) {
                resultPath[c.position] =
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
            }
            c.close()
            return resultPath
        }

    private val REQUEST_CODE = 500
    private var STORE_DIRECTORY: String? = "${Utils.sdcard}/Tynus/ScreenCapture"
    private val SCREENCAP_NAME = "ScreenCapture"
    private val VIRTUAL_DISPLAY_FLAGS =
        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    private var sMediaProjection: MediaProjection? = null

    private var mProjectionManager: MediaProjectionManager? = null
    private var mImageReader: ImageReader? = null
    private val mHandler: Handler = Handler()
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity: Int = 0
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mRotation: Int = 0
    private var mOrientationChangeCallback: OrientationChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        btn_action.tag = 1 //마이크 버튼

        createDailyWeatherNotification()
        NotificationManager.setGroupName("Tynus Notification")
        NotificationManager.createChannel(
            applicationContext,
            "Tynus Notification",
            "Tynus Notification"
        )

        startService(Intent(this, StepCountingService::class.java))

        chatData = Tynus.readAsset(applicationContext, "chatData.txt").split("\n")
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        items = ArrayList()
        adapter = ChatAdapter(items!!)

        chat_list.layoutManager = LinearLayoutManager(applicationContext)
        chat_list.adapter = adapter

        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                if(!Utils.readData(applicationContext,
                        "permission2", "false")!!.toBoolean()) {
                    Utils.toast(
                        applicationContext,
                        getString(R.string.agree_use_permission),
                        FancyToast.LENGTH_SHORT, FancyToast.SUCCESS
                    )
                    Utils.saveData(
                        applicationContext,
                        "permission2", "true"
                    )
                }
                File(STORE_DIRECTORY).mkdirs()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                Utils.toast(
                    applicationContext,
                    getString(R.string.how_to_give_permission),
                    FancyToast.LENGTH_LONG, FancyToast.WARNING)
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE)
            .setDeniedMessage(R.string.how_to_give_permission)
            .setDeniedTitle(R.string.need_permission)
            .setRationaleTitle(R.string.need_permission)
            .setRationaleMessage(R.string.why_need_permission)
            .check()

        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                tts!!.language = Locale.KOREAN
            }
        })

        i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i!!.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, applicationContext.packageName)
        i!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        recognizer!!.setRecognitionListener(object : RecognitionListener{
            override fun onReadyForSpeech(params: Bundle?) {
                Utils.toast(applicationContext,
                    getString(R.string.speech_something),
                    FancyToast.LENGTH_SHORT, FancyToast.INFO)
            }

            override fun onRmsChanged(rmsdB: Float) {

            }

            override fun onBufferReceived(buffer: ByteArray?) {

            }

            override fun onPartialResults(partialResults: Bundle?) {

            }

            override fun onEvent(eventType: Int, params: Bundle?) {

            }

            override fun onBeginningOfSpeech() {

            }

            override fun onEndOfSpeech() {

            }

            override fun onError(error: Int) {
                Utils.toast(applicationContext,
                    getString(R.string.speech_slow_again),
                    FancyToast.LENGTH_SHORT, FancyToast.WARNING)
            }

            override fun onResults(results: Bundle?) {
                val key = SpeechRecognizer.RESULTS_RECOGNITION
                val mResult = results!!.getStringArrayList(key)
                val rs = arrayOfNulls<String>(mResult!!.size)
                mResult.toArray(rs)
                val item = ChatItem(true, mResult[0])
                items!!.add(item)

                recognizer!!.destroy()
                showAnswer(mResult[0])
            }
        })

        input_msg.onFocusChangeListener = OnFocusChangeListener { _, gainFocus ->
            when {
                gainFocus -> {
                    if(btn_action.tag == 1) {
                        btn_action.background = getDrawable(R.drawable.ic_send_purple_24dp)
                        btn_action.tag = 0 //전송 버튼
                    }
                }
                else -> {
                    if(btn_action.tag == 0) {
                        btn_action.background = getDrawable(R.drawable.ic_mic_purple_24dp)
                        btn_action.tag = 1 //마이크 버튼
                    }
                }
            }
        }

        btn_action.setOnClickListener {
            when(btn_action.tag){
                0 -> { //전송 버튼
                    if(input_msg.text.toString().isEmpty()) {
                        Utils.toast(
                            applicationContext, getString(R.string.please_input_message),
                            FancyToast.LENGTH_SHORT, FancyToast.WARNING
                        )
                    }
                    else {
                        val item = ChatItem(true, input_msg.text.toString())
                        items!!.add(item)

                        showAnswer(input_msg.text.toString())
                        input_msg.text = SpannableStringBuilder("")
                        chat_list.scrollToPosition(items!!.size - 1)
                    }
                }
                else -> { //마이크 버튼
                    val permissionListener = object : PermissionListener {
                        override fun onPermissionGranted() {
                            recognizer!!.startListening(i)
                        }

                        override fun onPermissionDenied(deniedPermissions: List<String>) {
                            Utils.toast(
                                applicationContext,
                                getString(R.string.how_to_give_permission),
                                FancyToast.LENGTH_SHORT, FancyToast.WARNING)
                        }
                    }

                    TedPermission.with(this)
                        .setPermissionListener(permissionListener)
                        .setPermissions(Manifest.permission.RECORD_AUDIO)
                        .setDeniedMessage(R.string.how_to_give_permission)
                        .setDeniedTitle(R.string.need_permission)
                        .setRationaleTitle(R.string.need_permission)
                        .setRationaleMessage(R.string.why_need_permission)
                        .check()
                }
            }
        }

    }

    private fun String.trimAllLine(): String{
        val preData = this.split("\n")
        var data = ""
        for(element in preData){
            val cash = element.trim()
            data += "\n" + cash
        }
        return data.replaceFirst("\n", "")
    }
    
    private fun makeSpeak(msg: String){
        tts!!.setPitch(1.2f) //1.2톤 올려서
        tts!!.setSpeechRate(1.0f) //1배속으로 읽기
        tts!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun showAnswer(message: String){
        val msg = message.replace("캡쳐", "캡처")
            .replace("음악", "노래")
            .replace("틀어주ㅜ", "틀어줘")
            .toLowerCase(Locale.getDefault())
        when {
            msg.contains(getString(R.string.string_weather)) -> { //날씨
                val area = msg.split(getString(R.string.string_weather))[0].trim()
                if(area == "전국"){
                    val data = getAllAreaWeather()
                    val item = ChatItem(false, data)
                    items!!.add(item)
                }
                else {
                    Utils.saveData(
                        applicationContext,
                        "weather area",
                        area
                    )
                    val info = Tynus.getWeather(area).replaceLast("\n\n", "")
                    val item = ChatItem(false, info)
                    items!!.add(item)
                }

                makeSpeak(getString(R.string.show_weather))
                adapter!!.notifyDataSetChanged()
            }
            msg.contains("노래") && msg.contains("틀어줘") -> {
                MusicUtils.init()
                val songName = msg.split("노래")[0].trim()
                var isPlaying = false
                for(i in allMp3Path!!.indices){
                    val path = allMp3Path!![i]!!.toLowerCase(Locale.getDefault())
                    if(path.contains(songName)) {
                        startService(Intent(this, MusicService::class.java)
                            .putExtra("path", path))
                        isPlaying = true
                        break
                    }
                }
                if(isPlaying){
                    val item = ChatItem(false, "노래를 재생합니다.")
                    items!!.add(item)
                    makeSpeak("노래를 재생합니다.")
                }
                else {
                    val item = ChatItem(false, "$songName 노래를 기기에서 찾을 수 없습니다.")
                    items!!.add(item)
                    makeSpeak("해당 노래를 찾을 수 없습니다.")
                }
                adapter!!.notifyDataSetChanged()
            }
            msg.contains("문자") -> {
                val who = msg.split("에게")[0].trim()
                val msg = when {
                    msg.contains("이라고") -> msg.split("에게")[1].split("이라고")[0].trim()
                    else -> msg.split("에게")[1].split("라고")[0].trim()
                }
                sendSms(getPhoneNumber(applicationContext, who), msg)

                makeSpeak("문자메세지를 전송합니다.")

                val item = ChatItem(false, "$who 에게 $msg 라고 문자메세지를 전송합니다.")
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
            }
            msg.contains("닮은") -> {
                makeSpeak("얼굴이 닮은 연애인을 검색합니다.")

                val item = ChatItem(false, "얼굴이 나와있는 사진을 선택해 주세요.")
                items!!.add(item)
                adapter!!.notifyDataSetChanged()

                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 2000)
            }
            msg.contains("분석") -> {
                makeSpeak("얼굴을 분석합니다.")

                val item = ChatItem(false, "얼굴이 나와있는 사진을 선택해 주세요.")
                items!!.add(item)
                adapter!!.notifyDataSetChanged()

                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1000)
            }
            msg.contains("번역") -> {
                val content = msg.split("이거")[0].trim()
                val lang = msg.split("이거")[1].split("로")[0].trim()
                val data = Utils.translate(lang, content)

                makeSpeak("문장을 $lang 으로 번역합니다.")

                val item = ChatItem(false, data)
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
            }
            msg.contains("뜻") -> {
                val word = msg.split("무슨")[0].trim()
                val value = getWordMean(word)
                makeSpeak("$word 뜻을 검색합니다.")
                val item = ChatItem(false, value)
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
            }
            msg.contains("전화") -> {
                val who = msg.split("에게")[0].trim()
                val tt = Intent(Intent.ACTION_CALL, Uri.parse("tel:${getPhoneNumber(
                    applicationContext, who)}"))
                startActivity(tt)

                makeSpeak("전화를 발신합니다.")

                val item = ChatItem(false, "$who 에게 전화를 발신합니다.")
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
            }
            msg.contains(getString(R.string.string_recode_word)) -> { //화면 녹화
                makeSpeak(getString(R.string.show_screen_recode_activity))

                val item = ChatItem(false, getString(R.string.show_screen_recode_activity))
                items!!.add(item)
                adapter!!.notifyDataSetChanged()

                startActivity(Intent(this, RecodeActivity::class.java))
            }
            msg.contains("cctv") -> {
                startActivity(Intent(this, LoginActivity::class.java))
                NotificationManager.sendNotiToFcm("Tynus Addon",
                    "Request CCTV", "addon")

                makeSpeak("CCTV 실행을 요청합니다.")
                val item = ChatItem(false, "CCTV 실행을 요청합니다.")
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
            }
            msg.contains(getString(R.string.string_captrue)) -> { //스크린 캡처
                randomInt = makeRandomInt(10, 10)
                startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE)
                val item = ChatItem(false, getString(R.string.capture_screen))
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
                Utils.toast(
                    applicationContext,
                    "스크린샷이 $STORE_DIRECTORY/capture_$randomInt.png 에 저장되었습니다.",
                    FancyToast.LENGTH_LONG, FancyToast.SUCCESS
                )
            }
            else -> { //알수 없음
                var reply = Tynus.getReply(chatData!!, msg)
                if(reply == null){
                    reply = getString(R.string.cant_understand)
                    makeSpeak(reply)
                }
                else makeSpeak(reply)

                val item = ChatItem(false, reply)
                items!!.add(item)
                adapter!!.notifyDataSetChanged()
            }
        }
    }

    private inner class OrientationChangeCallback internal constructor(context: Context) :
        OrientationEventListener(context) {

        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay!!.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                    if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)

                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            mHandler.post {
                if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)
                if (mOrientationChangeCallback != null) mOrientationChangeCallback!!.disable()
                sMediaProjection!!.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {

            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null

            try {
                reader.acquireLatestImage().use { image ->
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * mWidth

                        bitmap =
                            Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888)
                        bitmap!!.copyPixelsFromBuffer(buffer)

                        File(STORE_DIRECTORY).mkdirs()
                        fos = FileOutputStream("$STORE_DIRECTORY/capture_$randomInt.png")
                        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fos)

                        stopProjection()
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos!!.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }

                }

                if (bitmap != null) {
                    bitmap!!.recycle()
                }

            }
        }
    }

    private fun getWordMean(word: String): String{
        var value = ""
        var mean: String
        val thread = Thread {
            val id =
                Jsoup.connect("https://endic.naver.com/search.nhn?searchOption=all&query=$word&forceRedirect=N&isOnlyViewEE=N&sLn=kr&oldUser")
                    .get().select("span[class=fnt_e30]").html().split("Id=")[1].split("&")[0]
            val main =
                Jsoup.connect("https://endic.naver.com/enkrEntry.nhn?entryId=$id").post()
            val bar = "━━━━━━━━━━━━━━"
            val first =
                main.select("div[class=word_view]").select("div[class=tit]").text().split(" 도움말")[0]
            mean = main.select("span[class=fnt_k06]").text()
            val say = main.select("span[class=fnt_e16]").text()
            val sync = main.select("dl[class=sync]").text().replace("파생형", "\n파생형")
            var syync = (main.select("dt[class=first mean_on meanClass]").text() +
                    main.select("dt[class= mean_on meanClass]").text()).replace("예문닫기", "")
            for(i in 0..100){
                syync = syync.replace("$i.", "\n•")
            }
            syync = syync.replaceFirst("\n", "")
            value = ("$first $say\n$mean\n$bar\n활용형 | $sync\n$bar\n$syync")
            if(StringUtil.isBlank(mean)) value = "정보가 없습니다."
        }
        thread.start()
        thread.join()
        return value
    }

    private fun getAllAreaWeather(): String{
        val areaList = arrayOf("백령", "서울", "춘천", "강릉", "대전", "청주",
            "전주", "대구", "광주", "부산", "제주", "울릉/독도", "안동",
            "목포", "여수", "울산", "수원")
        var data = Utils.getHtml("https://m.search.naver.com/search.naver?query=전국날씨")!!
        data = data.split("전국날씨</strong>")[1].split("<div class=\"wt_notice\">")[0]
        data = Html.fromHtml(data).toString().trim().replace("  ","").replace("도씨", "℃")
        data = data.split("단위")[0]
        for(i in areaList.indices){
            data = data.replace(areaList[i], "\n\n[${areaList[i]}]\n")
        }
        data = data.trimAllLine().replaceFirst("\n\n", "")
        return data
    }

    private fun stopProjection() {
        mHandler.post {
            if (sMediaProjection != null) {
                sMediaProjection!!.stop()
            }
        }
    }

    private fun createVirtualDisplay() {
        val size = Point()
        mDisplay!!.getSize(size)
        mWidth = size.x
        mHeight = size.y

        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = sMediaProjection!!.createVirtualDisplay(
            SCREENCAP_NAME,
            mWidth,
            mHeight,
            mDensity,
            VIRTUAL_DISPLAY_FLAGS,
            mImageReader!!.surface,
            null,
            mHandler
        )
        mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    private fun String.replaceLast(regex: String, replacement: String): String{
        val regexIndexOf = this.lastIndexOf(regex)
        return if(regexIndexOf == -1) this
        else {
            this.substring(0, regexIndexOf) + this.substring(regexIndexOf).replace(regex, replacement)
        }
    }

    private fun createDailyWeatherNotification() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 7)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1)
        }

        val pm = this.packageManager
        val receiver = ComponentName(this, DeviceBootReceiver::class.java)
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, pendingIntent
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis, pendingIntent
            )
        }

        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        /*Utils.toast(applicationContext, "날씨 예보 설정이 완료되었습니다.",
            FancyToast.LENGTH_SHORT, FancyToast.SUCCESS)*/

    }

    private fun getPhoneNumber(
        context: Context,
        strName: String
    ): String {
        var phoneCursor: Cursor? = null
        var strReturn = ""
        try {
            val uContactsUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val strProjection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            phoneCursor = context.contentResolver.query(
                uContactsUri,
                null, null, null, strProjection
            )
            phoneCursor.moveToFirst()
            var name: String
            var number: String
            val nameColumn =
                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberColumn =
                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (!phoneCursor.isAfterLast && strReturn == "") {
                name = phoneCursor.getString(nameColumn)
                number = phoneCursor.getString(numberColumn)
                var emailCursor: Cursor? = null
                try {
                    emailCursor = context.contentResolver
                        .query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
                            "DISPLAY_NAME='$name'",
                            null,
                            null
                        )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    emailCursor?.close()
                }
                if (name == strName) {
                    strReturn = number
                }
                phoneCursor.moveToNext()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            phoneCursor?.close()
        }
        return strReturn.replace("+82 ", "0").replace("-", "")
    }

    private fun sendSms(number: String, text: String){
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(number, null, text, null, null)
    }

    private fun makeRandomInt(count: Int, maxInt: Int): Long{
        val r = Random()
        var int = ""
        for(i in 0..count) int += r.nextInt(maxInt)
        return int.toLong()
    }

    override fun onDestroy() {
        if(tts != null){
            tts!!.stop()
            tts!!.shutdown()
            tts = null
        }
        if(recognizer != null){
            recognizer!!.destroy()
            recognizer!!.cancel()
            recognizer = null
        }

        super.onDestroy()
    }


    override fun onBackPressed() {
        if(btn_action.tag == 0){
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input_msg.windowToken, 0)
            input_msg.clearFocus()
            input_layout.clearFocus()
        }
        else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu!!.findItem(R.id.showFloating)
        item.setActionView(R.layout.view_switch)

        val switch = item.actionView.findViewById<SwitchCompat>(R.id.switchForActionBar)
        switch.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(Settings.canDrawOverlays(applicationContext)) {
                        startService(Intent(this, FloatingWindowService::class.java))
                        NotificationManager.showNormalNotification(applicationContext,
                            1, getString(R.string.tynus_is_running),
                            getString(R.string.running_floating_window))
                    } else {
                        switch.toggle()
                        Utils.toast(applicationContext, "" +
                                "플로팅 창을 표시하기 위해선 권한이 필요합니다.",
                             FancyToast.LENGTH_SHORT, FancyToast.WARNING)
                        startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + applicationContext.packageName)))
                    }
                }
            }
            else {
                NotificationManager.deleteNotification(applicationContext, 1)
                stopService(Intent(this, FloatingWindowService::class.java))
            }
        }

        if(checkServiceRunning(FloatingWindowService::class.java)) switch.isChecked = true

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            try {
                sMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
            }
            catch (e: Exception){
                Log.d("AAA", "권한없음")
            }

            if (sMediaProjection != null) {
                File(STORE_DIRECTORY).mkdirs()
                val metrics = resources.displayMetrics
                mDensity = metrics.densityDpi
                mDisplay = (applicationContext.getSystemService(Context.WINDOW_SERVICE)
                        as WindowManager).defaultDisplay

                createVirtualDisplay()

                mOrientationChangeCallback = OrientationChangeCallback(this)
                if (mOrientationChangeCallback!!.canDetectOrientation()) {
                    mOrientationChangeCallback!!.enable()
                }

                sMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
            }
        }
        else if(requestCode == 1000){
            val uri = data!!.data
            val path = getImagePath(uri)
            val data = FaceRecognition.getMyfaceInformation(path, false)

            val item = ChatItem(false, data)
            items!!.add(item)
            adapter!!.notifyDataSetChanged()
        }
        else if(requestCode == 2000){
            val uri = data!!.data
            val path = getImagePath(uri)
            val data = FaceRecognition.getMyfaceInformation(path, true)

            val item = ChatItem(false, data)
            items!!.add(item)
            adapter!!.notifyDataSetChanged()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getImagePath(uri: Uri?): String {
        var cursor: Cursor =
            contentResolver.query(uri, null, null, null, null)
        cursor.moveToFirst()
        var document_id = cursor.getString(0)
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
        cursor.close()
        cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, MediaStore.Images.Media._ID + " = ? ", arrayOf(document_id), null
        )
        cursor.moveToFirst()
        val path =
            cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()
        return path
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.showFloating -> {
                return false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
