package com.xrlab.labframe_plugin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

public class Main {
    /**
     * Check storage permission
     * @param unityActivity
     * @param packageName Unity: Application.identifier
     * @return
     */
    public static boolean RequestStoragePermission(Activity unityActivity, String packageName) {
        PackageManager packageManager = unityActivity.getPackageManager();

        // >= Android 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(Environment.isExternalStorageManager()) {
                return true;
            }
            // request MANAGE_EXTERNAL_STORAGE
            Intent reqPermIntent = new Intent(
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:"+packageName));
            unityActivity.startActivity(reqPermIntent);
            MakeToast(unityActivity, "Please grant the permission and restart the app!");
            return false;
        }
        else { // Android <= 10
            String[] perms = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            unityActivity.requestPermissions(perms, 0);
            return true;
        }
    }

    /**
     * Show toast
     * @param unityActivity
     * @param msg
     */
    public static void MakeToast(Activity unityActivity, String msg) {
        Toast.makeText(unityActivity, msg, Toast.LENGTH_LONG).show();
    }
}
