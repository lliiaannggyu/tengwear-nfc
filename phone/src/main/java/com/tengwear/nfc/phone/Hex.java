package com.tengwear.nfc.phone;

public final class Hex {
    private Hex() {}

    public static String bytesToHex(byte[] b) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) sb.append(String.format("%02X", v));
        return sb.toString();
    }
}
