package com.tengwear.nfc.wear;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

/**
 * HCE 卡模拟服务：所有 APDU 交给 ConfigStore 按规则应答。
 * 注意：HCE 是 ISO-DEP 协议，模拟的是 APDU 层数据，
 * 无法模拟 MIFARE Classic 私有协议的扇区认证。
 */
public class EmulatorApduService extends HostApduService {

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (commandApdu == null || commandApdu.length == 0) {
            return Hex.hexToBytes("6D00");
        }
        return ConfigStore.respond(this, commandApdu);
    }

    @Override
    public void onDeactivated(int reason) {
        // 0 = DEACTIVATION_LINK_LOSS, 1 = DEACTIVATION_DESELECTED
    }
}
