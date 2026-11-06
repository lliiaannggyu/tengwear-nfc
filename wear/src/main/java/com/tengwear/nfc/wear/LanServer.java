package com.tengwear.nfc.wear;

import android.content.Context;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 局域网 TCP 服务（端口 47813）。
 * 协议: [4字节大端长度][UTF-8 JSON] -> 回复 1 字节 0x01 表示成功。
 */
public final class LanServer {

    public interface Listener {
        void onConfig(JSONObject cfg);
        void onLog(String s);
    }

    private LanServer() {}

    public static void start(Context ctx, int port, Listener listener) {
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                listener.onLog("LAN 服务已启动 port=" + port);
                while (true) {
                    try (Socket s = server.accept()) {
                        s.setSoTimeout(8000);
                        DataInputStream in = new DataInputStream(s.getInputStream());
                        int len = in.readInt();
                        if (len <= 0 || len > 512 * 1024) continue;
                        byte[] buf = new byte[len];
                        in.readFully(buf);
                        JSONObject cfg = new JSONObject(new String(buf, "UTF-8"));
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeByte(1);
                        out.flush();
                        listener.onConfig(cfg);
                    } catch (Exception e) {
                        listener.onLog("单次连接错误: " + e);
                    }
                }
            } catch (Exception e) {
                listener.onLog("LAN 服务错误: " + e);
            }
        });
        t.setDaemon(true);
        t.setName("tengwear-lan");
        t.start();
    }
}
