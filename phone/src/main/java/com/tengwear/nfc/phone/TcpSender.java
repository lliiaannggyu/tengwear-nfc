package com.tengwear.nfc.phone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 把 JSON 配置发送到手表端 TCP 服务（长度前缀协议）。
 * 协议: [4字节大端长度][UTF-8 JSON]
 */
public final class TcpSender {

    private TcpSender() {}

    public static boolean send(String hostPort, String json) {
        String[] hp = hostPort.split(":");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hp[0], Integer.parseInt(hp[1])), 3000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte[] data = json.getBytes("UTF-8");
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            return in.readByte() == 1; // 手表端 ack
        } catch (Exception e) {
            return false;
        }
    }
}
