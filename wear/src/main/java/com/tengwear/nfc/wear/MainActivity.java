package com.tengwear.nfc.wear;

import android.app.Activity;
import android.content.ComponentName;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 手表端主界面：启动 UDP 广播（让手机发现）+ TCP 服务（接收配置），
 * 收到配置后写入本地并通过 CardEmulation 动态注册 AID。
 */
public class MainActivity extends Activity {

    private TextView statusView;
    private TextView logView;
    private static final StringBuilder LOG = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (10 * d);
        root.setPadding(pad, pad, pad, pad);

        statusView = new TextView(this);
        statusView.setText("启动中…");
        statusView.setTextSize(16);
        root.addView(statusView);

        logView = new TextView(this);
        logView.setText("下滑查看日志");
        logView.setTextSize(12);
        root.addView(logView, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);

        // 1. 局域网 TCP 服务，接收手机端下发的模拟配置
        LanServer.start(this, 47813, new LanServer.Listener() {
            @Override
            public void onConfig(JSONObject cfg) {
                ConfigStore.save(MainActivity.this, cfg);
                registerAids(cfg);
                JSONArray rs = cfg.optJSONArray("responses");
                log("收到配置: AID=" + cfg.optString("aid") + " 规则=" + (rs == null ? 0 : rs.length()));
                runOnUiThread(() -> statusView.setText(
                        "模拟运行中\nAID: " + cfg.optString("aid", "(未设置)")
                                + "\n规则数: " + (rs == null ? 0 : rs.length())));
            }

            @Override
            public void onLog(String s) {
                log(s);
            }
        });

        // 2. UDP 广播自我宣告，供手机端发现
        Announcer.start(this, "47813");

        // 3. 恢复上次配置并注册
        JSONObject saved = ConfigStore.load(this);
        if (saved.length() > 0 && saved.has("aid")) {
            registerAids(saved);
            runOnUiThread(() -> statusView.setText("已恢复上次配置\nAID: " + saved.optString("aid")));
            log("已恢复上次配置");
        } else {
            runOnUiThread(() -> statusView.setText("运行中，等待手机发送配置…"));
        }
        log("TengWear NFC 手表端已启动");
    }

    private void registerAids(JSONObject cfg) {
        try {
            String aid = cfg.optString("aid", "").replace(" ", "").toUpperCase();
            if (aid.isEmpty()) return;
            android.nfc.NfcAdapter nfc = android.nfc.NfcAdapter.getDefaultAdapter(this);
            if (nfc == null) { log("NFC 不可用，无法注册 AID"); return; }
            CardEmulation ce = CardEmulation.getInstance(nfc);
            ComponentName cn = new ComponentName(this, EmulatorApduService.class);
            List<String> aids = new ArrayList<>();
            aids.add(aid);
            boolean ok = ce.registerAidsForService(cn, CardEmulation.CATEGORY_OTHER, aids);
            log("动态注册 AID " + aid + (ok ? " 成功" : " 失败"));
        } catch (Exception e) {
            log("注册 AID 异常: " + e);
        }
    }

    private void log(String s) {
        synchronized (LOG) {
            LOG.insert(0, s + "\n");
            if (LOG.length() > 4000) LOG.setLength(4000);
        }
        runOnUiThread(() -> logView.setText(LOG.toString()));
    }
}
