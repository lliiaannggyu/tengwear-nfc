package com.tengwear.nfc.phone;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * 手机读卡端：ReaderMode 读 NFC 标签/卡元数据，经局域网发给手表端。
 * 只读取自己拥有的卡片的公开信息（UID、技术类型、NDEF 文本）。
 */
public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {

    private TextView logView;
    private ListView deviceList;
    private TextView statusView;
    private ArrayAdapter<String> deviceAdapter;
    private final HashMap<String, String> devices = new HashMap<>(); // name -> ip:port
    private String selectedAddr = null;
    private NfcAdapter nfc;
    private boolean reading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * d);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("TengWear NFC · 手机读卡端");
        title.setTextSize(20);
        root.addView(title);

        statusView = new TextView(this);
        statusView.setText("发现手表中…（手表端需与手机同一 Wi-Fi）");
        root.addView(statusView);

        deviceList = new ListView(this);
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceList.setAdapter(deviceAdapter);
        deviceList.setOnItemClickListener((p, v, pos, id) -> {
            String item = deviceAdapter.getItem(pos);
            if (item == null) return;
            if (item.startsWith("▶ ")) {
                selectedAddr = null;
                refreshList();
            } else {
                selectedAddr = devices.get(item);
                refreshList();
            }
        });
        root.addView(deviceList, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button toggle = new Button(this);
        toggle.setText("开始读卡 / 停止读卡");
        toggle.setOnClickListener(v -> setReading(!reading));
        root.addView(toggle);

        logView = new TextView(this);
        logView.setText("日志：");
        root.addView(logView, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);

        nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc == null) {
            statusView.setText("本机不支持 NFC");
            toggle.setEnabled(false);
        }

        WatchDiscovery.start(this, new WatchDiscovery.Listener() {
            @Override
            public void onDevice(String name, String addr) {
                boolean isNew = !devices.containsValue(addr);
                devices.values().removeIf(v -> v.equals(addr));
                devices.put(name, addr);
                runOnUiThread(() -> {
                    if (isNew) appendLog("发现手表: " + name + " -> " + addr);
                    refreshList();
                });
            }

            @Override
            public void onLog(String s) {
                runOnUiThread(() -> appendLog(s));
            }
        });
    }

    private void refreshList() {
        deviceAdapter.clear();
        for (String name : devices.keySet()) {
            String addr = devices.get(name);
            deviceAdapter.add(selectedAddr != null && selectedAddr.equals(addr) ? "▶ " + name + "  " + addr : name + "  " + addr);
        }
        deviceAdapter.notifyDataSetChanged();
        statusView.setText(selectedAddr == null
                ? "点击选择目标手表（▶ = 已选中）"
                : "目标: " + selectedAddr);
    }

    private void setReading(boolean on) {
        reading = on;
        if (nfc == null) return;
        if (on) {
            Bundle extras = new Bundle();
            extras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500);
            int flags = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V;
            nfc.enableReaderMode(this, this, flags, extras);
            appendLog("读卡模式已开启，请碰卡");
        } else {
            nfc.disableReaderMode(this);
            appendLog("读卡模式已关闭");
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            JSONObject o = new JSONObject();
            o.put("kind", "tag");
            o.put("uid", Hex.bytesToHex(tag.getId()));

            JSONArray techs = new JSONArray();
            for (String t : tag.getTechList()) techs.put(t);
            o.put("techs", techs);

            // NDEF 内容（公开文本/URI）
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                NdefMessage msg = ndef.getCachedNdefMessage();
                if (msg != null) {
                    JSONArray records = new JSONArray();
                    for (NdefRecord r : msg.getRecords()) {
                        JSONObject ro = new JSONObject();
                        String uri = r.toUri() != null ? r.toUri().toString() : null;
                        if (uri != null) {
                            ro.put("type", "uri");
                            ro.put("data", uri);
                        } else {
                            ro.put("type", "text");
                            ro.put("data", new String(r.getPayload(), "UTF-8"));
                        }
                        records.put(ro);
                    }
                    o.put("ndef", records);
                }
                ndef.close();
            }

            appendLog("读卡成功 UID=" + o.getString("uid") + " techs=" + techs.length());

            if (selectedAddr != null) {
                boolean ok = TcpSender.send(selectedAddr, o.toString());
                appendLog(ok ? "已发送到手表 " + selectedAddr : "发送失败 " + selectedAddr);
                Toast.makeText(this, ok ? "已发送到手表" : "发送失败", Toast.LENGTH_SHORT).show();
            } else {
                appendLog("未选择手表，数据未发送");
            }
        } catch (Exception e) {
            appendLog("读卡异常: " + e);
        }
    }

    private void appendLog(String s) {
        logView.setText(logView.getText() + "\n" + s);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (reading && nfc != null) nfc.disableReaderMode(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reading && nfc != null) {
            Bundle extras = new Bundle();
            int flags = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V;
            nfc.enableReaderMode(this, this, flags, extras);
        }
    }
}
