package com.tengwear.nfc.wear;

public final class Hex {
    private Hex() {}

    public static String bytesToHex(byte[] b) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) sb.append(String.format("%02X", v));
        return sb.toString();
    }

    public static byte[] hexToBytes(String h) {
        if (h == null) return new byte[0];
        h = h.replaceAll("[^0-9A-Fa-f]", "");
        int len = h.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
