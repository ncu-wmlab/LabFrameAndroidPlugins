package com.xrlab.labframe_plugin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class Main {
    private static final String TAG = "LabFramePlugin_Main";

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
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
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
     * Open apk by package name
     * @param unityActivity
     * @param packageName
     */
    public static void OpenApk(Activity unityActivity, String packageName) {
        Log.d(TAG, "OpenApk "+packageName);
        Intent launchIntent = unityActivity.getPackageManager().getLaunchIntentForPackage(packageName);
        //launchIntent.putExtra("User_Info", user_info);
        unityActivity.startActivity(launchIntent);
    }

    /**
     * Show toast
     * @param unityActivity
     * @param msg
     */
    public static void MakeToast(Activity unityActivity, String msg) {
        Log.d(TAG, "MakeToast msg="+msg);
        Toast.makeText(unityActivity, msg, Toast.LENGTH_LONG).show();
    }

}
