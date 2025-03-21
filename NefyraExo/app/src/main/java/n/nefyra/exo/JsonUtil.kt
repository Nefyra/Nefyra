package com.nefyra.exo

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileInputStream

object JsonUtil {
    fun readUrlFromPrivateStorage(context: Context): VideoConfig? {
        var fis: FileInputStream? = null
        return try {
            val file = File(context.getExternalFilesDir(null), "video_config.json")
            val mapper = ObjectMapper()
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

            if (!file.exists()) {
                Log.e("JSON", "File not found in external private storage")
                return null
            }

            fis = FileInputStream(file)
            mapper.readValue(fis, VideoConfig::class.java)
        } catch (e: Exception) {
            Log.e("JSON", "解析失败", e)
            null
        } finally {
            try {
                fis?.close()
            } catch (e: Exception) {
                Log.e("JSON", "关闭流失败", e)
            }
        }
    }
}