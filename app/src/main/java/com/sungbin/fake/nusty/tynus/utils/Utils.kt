package com.sungbin.fake.nusty.tynus.utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import com.shashank.sony.fancytoastlib.FancyToast
import org.jsoup.Jsoup
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder


@Suppress("DEPRECATION")
@SuppressLint("MissingPermission", "HardwareIds")
object Utils {

    val sdcard = Environment.getExternalStorageDirectory().absolutePath!!
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.146 Whale/2.6.89.9 Safari/537.36"

    fun createFolder(name: String) {
        File("$sdcard/Tynus/$name/").mkdirs()
    }

    fun read(name: String, _null: String): String {
        try {
            val file = File("$sdcard/Tynus/$name/")
            if (!file.exists()) return _null
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis)
            val br = BufferedReader(isr)
            var str = br.readLine()

            while (true) {
                val inputLine = br.readLine() ?: break
                str += "\n" + inputLine
            }
            fis.close()
            isr.close()
            br.close()
            return str.toString()
        } catch (e: Exception) {
            Log.e("READ", e.toString())
        }

        return _null
    }

    fun save(name: String, content: String) {
        try {
            File("$sdcard/Tynus/").mkdirs()
            val file = File("$sdcard/Tynus/$name")
            val fos = FileOutputStream(file)
            fos.write(content.toByteArray())
            fos.close()
        } catch (e: Exception) {
            Log.e("SAVE", e.toString())
        }

    }

    fun delete(name: String) {
        File("$sdcard/Tynus/$name").delete()
    }

    fun readData(ctx: Context, name: String, _null: String): String? {
        val pref = ctx.getSharedPreferences("pref", MODE_PRIVATE)
        return pref.getString(name, _null)
    }

    fun saveData(ctx: Context, name: String, value: String) {
        val pref = ctx.getSharedPreferences("pref", MODE_PRIVATE)
        val editor = pref.edit()

        editor.putString(name, value)
        editor.apply()
    }

    fun clearData(ctx: Context) {
        val pref = ctx.getSharedPreferences("pref", MODE_PRIVATE)
        val editor = pref.edit()
        editor.clear()
        editor.apply()
    }

    fun copy(ctx: Context, text: String) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.primaryClip = clip
        toast(ctx, ctx.getString(com.sungbin.fake.nusty.tynus.R.string.copy_done), FancyToast.LENGTH_SHORT, FancyToast.SUCCESS)
    }

    fun error(ctx: Context, e: Exception, at: String) {
        val data = "Error: $e\nLineNumber: ${e.stackTrace[0].lineNumber}\nAt: $at"
        toast(ctx, data, FancyToast.LENGTH_SHORT, FancyToast.ERROR)
        copy(ctx, data)
        Log.e("Error", data)
    }

    @JvmStatic
    fun toast(ctx: Context, txt: String, length: Int, type: Int) {
        FancyToast.makeText(ctx, txt, length, type, false).show()
    }

    fun getHtml(adress: String): String? {
        return try {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val conn = Jsoup.connect(adress).userAgent(USER_AGENT).ignoreContentType(true)
            val doc = conn.get()
            doc.toString()
        } catch (e: IOException) {
            return e.message
        }

    }

    fun translate(targetLeng: String, string: String?): String? {
        return try {
            var lang = "en"
            val langs =
                "gl:갈리시아어::gu:구자라트어::el:그리스어::nl:네덜란드어::ne:네팔어::no:노르웨이어::da:덴마크어::de:독일어::lo:라오어::lv:라트비아어::la:라틴어::ru:러시아어::ro:루마니아어::lb:룩셈부르크어::lt:리투아니아어::mr:마라티어::mi:마오리어::mk:마케도니아어::mg:말라가시어::ml:말라얄람어::ms:말레이어::mt:몰타어::mn:몽골어::hmn:몽어::my:미얀마어::eu:바스크어::vi:베트남어::be:벨라루스어::bn:벵골어::bs:보스니아어::bg:불가리아어::sm:사모아어::sr:세르비아어::ceb:세부아노::st:세소토어::so:소말리아어::sn:쇼나어::su:순다어::sw:스와힐리어::sv:스웨덴어::gd:스코틀랜드게일어::es:스페인어::sk:슬로바키아어::sl:슬로베니아어::sd:신디어::si:신할라어::ar:아랍어::hy:아르메니아어::is:아이슬란드어::ht:아이티크리올어::ga:아일랜드어::az:아제르바이잔어::af:아프리칸스어::sq:알바니아어::am:암하라어::et:에스토니아어::eo:에스페란토어::en:영어::yo:요루바어::ur:우르두어::uz:우즈베크어::uk:우크라이나어::cy:웨일즈어::ig:이그보어::yi:이디시어::it:이탈리아어::id:인도네시아어::ja:일본어::jw:자바어::ka:조지아어::zu:줄루어::zh-CN:중국어::ny:체와어::cs:체코어::kk:카자흐어::ca:카탈로니아어::kn:칸나다어::co:코르시카어::xh:코사어::ku:쿠르드어::hr:크로아티아어::km:크메르어::ky:키르기스어::tl:타갈로그어::ta:타밀어::tg:타지크어::th:태국어::tr:터키어::te:텔루구어::ps:파슈토어::pa:펀자브어::fa:페르시아어::pt:포르투갈어::pl:폴란드어::fr:프랑스어::fy:프리지아어::fi:핀란드어::haw:하와이어::ha:하우사어::ko:한국어::hu:헝가리어::iw:히브리어::hi:힌디어"
            val langL = langs.split("::").toTypedArray()
            for (i in langL.indices) {
                val cache = langL[i].split(":").toTypedArray()
                if (cache[1] == targetLeng) {
                    lang = cache[0]
                }
            }
            val value: String = URLEncoder.encode(string, "UTF-8")
            val policy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val url =
                URL("http://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$lang&dt=t&q=$value&ie=UTF-8&oe=UTF-8")
            val con: URLConnection = url.openConnection()
            con.setRequestProperty("User-Agent", "Mozilla/5.0")
            val br =
                BufferedReader(InputStreamReader(con.getInputStream(), "UTF-8"))
            var inputLine = ""
            while (br.readLine().also { inputLine = it } != null) {
                return inputLine.split("\"").toTypedArray()[1]
            }
            br.close()
            inputLine
        } catch (e: java.lang.Exception) {
            e.toString()
        }
    }
}