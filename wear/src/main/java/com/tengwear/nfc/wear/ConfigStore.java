package com.tengwear.nfc.wear;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 模拟配置存储与 APDU 匹配引擎。
 * 配置格式:
 * {
 *   "aid": "F001020304",
 *   "selectResponse": "6F2180012B50...9000",
 *   "defaultResponse": "6D00",
 *   "responses": [ {"req": "80CA9F7F00", "res": "...9000"} ]
 * }
 */
public final class ConfigStore {

    private ConfigStore() {}

    private static File file(Context c) {
        return new File(c.getFilesDir(), "config.json");
    }

    public static synchronized JSONObject load(Context c) {
        try (FileInputStream fis = new FileInputStream(file(c))) {
            byte[] buf = new byte[fis.available()];
            int n = fis.read(buf);
            return new JSONObject(new String(buf, 0, Math.max(0, n), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static synchronized void save(Context c, JSONObject cfg) {
        try (FileOutputStream fos = new FileOutputStream(file(c))) {
            fos.write(cfg.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    /** APDU 匹配：SELECT(00A40400) 用 selectResponse；其余按 responses 精确匹配；兜底 defaultResponse */
    public static byte[] respond(Context c, byte[] apdu) {
        JSONObject cfg = load(c);
        String h = Hex.bytesToHex(apdu);
        try {
            if (h.startsWith("00A40400") && cfg.has("selectResponse")) {
                return Hex.hexToBytes(cfg.getString("selectResponse"));
            }
            JSONArray rs = cfg.optJSONArray("responses");
            if (rs != null) {
                for (int i = 0; i < rs.length(); i++) {
                    JSONObject r = rs.getJSONObject(i);
                    if (r.optString("req", "").equalsIgnoreCase(h)) {
                        return Hex.hexToBytes(r.optString("res", "6D00"));
                    }
                }
            }
        } catch (Exception ignored) {}
        return Hex.hexToBytes(cfg.optString("defaultResponse", "6D00"));
    }
}
