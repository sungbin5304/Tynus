package com.sungbin.fake.nusty.tynus.utils

import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection


object FaceRecognition {
    private enum class Gender(val value: String) {
        female("여자"), male("남자")
    }

    private enum class Emotion(val value: String) {
        angry("화남"), disgust("혐오함"), fear("두려움"),
        laugh("웃음"), neutral("평범함"), sad("슬픔"),
        surprise("놀람"), smile("즐거움"), talking("수다떰")
    }

    private enum class Pose(val value: String) {
        part_face("부분 얼굴"), false_face("진짜 얼굴이 아님"),
        sunglasses("선글라스를 낌"), frontal_face("정면 얼굴"),
        left_face("왼쪽 얼굴"), right_face("오른쪽 얼굴"),
        rotate_face("회전된 얼굴")
    }

    fun getMyfaceInformation(imgFile: String, getCelebrityLikeMyFace: Boolean = false): String {
        try {
            var result = ""
            var faceCount = ""
            val thread = Thread {
                val clientId = "rGknnSNS3p_EfppfXh9z"
                val clientSecret = "VP6l2wF7Tg"
                val paramName = "image"
                val uploadFile = File(imgFile)
                val apiURL =
                    if (getCelebrityLikeMyFace) "https://openapi.naver.com/v1/vision/celebrity"
                    else "https://openapi.naver.com/v1/vision/face"
                val url = URL(apiURL)
                val con = url.openConnection() as HttpURLConnection
                con.useCaches = false
                con.doOutput = true
                con.doInput = true
                val boundary = "---" + System.currentTimeMillis() + "---"
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                con.setRequestProperty("X-Naver-Client-Id", clientId)
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret)
                val outputStream = con.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)
                val LINE_FEED = "\r\n"
                val fileName = uploadFile.name
                writer.append("--$boundary").append(LINE_FEED)
                writer.append("Content-Disposition: form-data; name=\"$paramName\"; filename=\"$fileName\"")
                    .append(LINE_FEED)
                writer.append(
                    "Content-Type: " + URLConnection.guessContentTypeFromName(
                        fileName
                    )
                ).append(LINE_FEED)
                writer.append(LINE_FEED)
                writer.flush()
                val inputStream = FileInputStream(uploadFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                inputStream.close()
                writer.append(LINE_FEED).flush()
                writer.append("--$boundary--").append(LINE_FEED)
                writer.close()
                val br: BufferedReader?
                val responseCode = con.responseCode
                br = if (responseCode == 200) {
                    BufferedReader(InputStreamReader(con.inputStream))
                } else {
                    BufferedReader(InputStreamReader(con.inputStream))
                }
                var inputLine: String?
                val response = StringBuffer()
                while (br.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                br.close()
                val jsonObject = JSONObject(response.toString())
                if (getCelebrityLikeMyFace){
                    val info = jsonObject.getJSONObject("info")
                    faceCount = info["faceCount"].toString()
                    if(faceCount == "0") {
                        result = "닮은 연애인이 없습니다."
                        return@Thread
                    }
                    val faces = jsonObject.getJSONArray("faces")
                    for(i in 0 until faces.length()){
                        val celebrity = faces.getJSONObject(i).getJSONObject("celebrity")
                        val name = celebrity["value"].toString()
                        val confidence = celebrity["confidence"].toString().toConfidence()
                        result += """
                            닮은 연예인 이름 : $name
                            정확도 : $confidence
                        """.trimIndent() + "\n\n"
                    }
                    result = result.replaceLast("\n\n", "")
                }
                else {
                    val info = jsonObject.getJSONObject("info")
                    faceCount = info["faceCount"].toString()
                    if(faceCount == "0") {
                        result = "감지된 얼굴이 없습니다."
                        return@Thread
                    }
                    val faces = jsonObject.getJSONArray("faces")
                    for(i in 0 until faces.length()){
                        try {
                            val landmark = faces.getJSONObject(i).getJSONObject("landmark")
                            val leftEye = landmark["leftEye"].toString()
                            val rightEye = landmark["rightEye"].toString()
                            val nose = landmark["nose"].toString()
                            val leftMouth = landmark["leftMouth"].toString()
                            val rightMouth = landmark["rightMouth"].toString()
                            result += """
                            왼쪽 눈의 위치 : $leftEye
                            오른쪽 눈의 위치 : $rightEye
                            코의 위치 : $nose
                            왼쪽 입 꼬리의 위치 : $leftMouth
                            오른쪽 입 꼬리의 위치 : $rightMouth
                        """.trimIndent() + "\n\n"
                        }
                        catch (ignored: Exception){
                            result += """
                            왼쪽 눈의 위치 : 알 수 없음
                            오른쪽 눈의 위치 : 알 수 없음
                            코의 위치 : 알 수 없음
                            왼쪽 입 꼬리의 위치 : 알 수 없음
                            오른쪽 입 꼬리의 위치 : 알 수 없음
                            """.trimIndent() + "\n\n"
                        }

                        try {
                            val gender = faces.getJSONObject(i).getJSONObject("gender")
                            val value = gender["value"].toString()
                            val confidence = gender["confidence"].toString().toConfidence()
                            result += """
                            성별 : ${Gender.valueOf(value).value}
                            성별 정확도: $confidence
                            """.trimIndent() + "\n\n"
                        }
                        catch (ignored: Exception){
                            result += """
                            성별 : 알 수 없음
                            성별 정확도: 알 수 없음
                            """.trimIndent() + "\n\n"
                        }

                        try{
                            val age = faces.getJSONObject(i).getJSONObject("age")
                            val value = age["value"].toString()
                            val confidence = age["confidence"].toString().toConfidence()
                            result += """
                                나이 : $value
                                나이 정확도 : $confidence
                            """.trimIndent() + "\n\n"
                        }
                        catch (ignored: Exception){
                            result += """
                                나이 : 알 수 없음
                                나이 정확도 : 알 수 없음
                            """.trimIndent() + "\n\n"
                        }

                        try{
                            val emotion = faces.getJSONObject(i).getJSONObject("emotion")
                            val value = emotion["value"].toString()
                            val confidence = emotion["confidence"].toString().toConfidence()
                            result += """
                                감정 : ${Emotion.valueOf(value).value}
                                감정 정확도 : $confidence
                            """.trimIndent() + "\n\n"
                        }
                        catch (ignored: Exception){
                            result += """
                                감정 : 알 수 없음
                                감정 정확도 : 알 수 없음
                            """.trimIndent() + "\n\n"
                        }

                        try{
                            val pose = faces.getJSONObject(i).getJSONObject("pose")
                            val value = pose["value"].toString()
                            val confidence = pose["confidence"].toString().toConfidence()
                            result += """
                                얼굴의 포즈 : ${Pose.valueOf(value).value}
                                얼굴의 포즈 정확도 : $confidence
                            """.trimIndent()
                        }
                        catch (ignored: Exception){
                            result += """
                                얼굴의 포즈 : 알 수 없음
                                얼굴의 포즈 정확도 : 알 수 없음
                            """.trimIndent()
                        }

                        val bar = "━━━━━━━━━━━━━━"
                        result += "\n$bar\n"

                    }
                }
            }
            thread.start()
            thread.join()
            val bar = "━━━━━━━━━━━━━━"
            result = result.replaceLast("\n$bar\n", "")
            result = "[감지된 얼굴 수 : $faceCount]\n$bar\n$result"
            return result
        } catch (e: Exception) {
            return e.toString()
        }
    }

    private fun String.toConfidence(): String{
        var value = this.substring(2,4)
        if(value[0].toString() == "0") value = value[1].toString()
        if(value == "0") value = "1"
        return "$value%"
    }

    private fun String.replaceLast(regex: String, replacement: String): String{
        val regexIndexOf = this.lastIndexOf(regex)
        return if(regexIndexOf == -1) this
        else {
            this.substring(0, regexIndexOf) + this.substring(regexIndexOf).replace(regex, replacement)
        }
    }
}