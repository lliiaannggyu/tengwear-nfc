package com.tengwear.nfc.wear;

import android.content.Context;
import android.os.Build;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/**
 * 每 3 秒向 255.255.255.255:47812 广播自己，
 * 格式: TENGWEAR_NFC|<设备名>|<ip>|<tcp端口>
 */
public final class Announcer {

    private Announcer() {}

    public static void start(Context ctx, String tcpPort) {
        Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                String msg = "TENGWEAR_NFC|" + Build.MODEL + "|" + localIp() + "|" + tcpPort;
                byte[] data = msg.getBytes("UTF-8");
                DatagramPacket p = new DatagramPacket(
                        data, data.length, InetAddress.getByName("255.255.255.255"), 47812);
                while (true) {
                    try {
                        socket.send(p);
                    } catch (Exception ignored) {}
                    Thread.sleep(3000);
                }
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.setName("tengwear-announcer");
        t.start();
    }

    static String localIp() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress a : Collections.list(nif.getInetAddresses())) {
                    if (!a.isLoopbackAddress() && a instanceof Inet4Address) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0.0";
    }
}
