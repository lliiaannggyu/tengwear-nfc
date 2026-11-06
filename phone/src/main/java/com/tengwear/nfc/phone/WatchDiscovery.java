package com.tengwear.nfc.phone;

import android.content.Context;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * 监听手表端的 UDP 广播（端口 47812），
 * 广播格式: TENGWEAR_NFC|<设备名>|<ip>|<tcp端口>
 */
public final class WatchDiscovery {

    public interface Listener {
        void onDevice(String name, String addr);
        void onLog(String s);
    }

    private WatchDiscovery() {}

    public static void start(Context ctx, Listener listener) {
        Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(47812)) {
                socket.setBroadcast(true);
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String msg = new String(p.getData(), 0, p.getLength()).trim();
                    if (!msg.startsWith("TENGWEAR_NFC|")) continue;
                    String[] parts = msg.split("\\|");
                    if (parts.length < 4) continue;
                    listener.onDevice(parts[1], parts[2] + ":" + parts[3]);
                }
            } catch (Exception e) {
                listener.onLog("设备发现错误: " + e);
            }
        });
        t.setDaemon(true);
        t.setName("tengwear-discovery");
        t.start();
    }
}
