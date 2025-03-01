package com.nefyra.exo;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nefyra.exo.VideoConfig;

public class JsonUtil {
  public static VideoConfig readUrlFromPrivateStorage(Context context) {
    FileInputStream fis = null;
    try {
      File file = new File(context.getFilesDir(), "video_config.json");

      if (!file.exists()) {
        Log.e("JSON", "File not found in private storage");
        return null;
      }

      fis = new FileInputStream(file);
      return new ObjectMapper().readValue(fis, VideoConfig.class);
    } catch (Exception e) {
      Log.e("JSON", "解析失败", e);
      return null;
    } finally {
      try {
        if (fis != null) fis.close();
      } catch (Exception e) {
        Log.e("JSON", "关闭流失败", e);
      }
    }
  }
}
