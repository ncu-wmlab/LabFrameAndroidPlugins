package com.xrlab.labframe_plugin;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

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
     * @param packageName Unity: Application.identifier
     * @return
     */
    public static boolean RequestStoragePermission(String packageName) {
        PackageManager packageManager = UnityPlayer.currentActivity.getPackageManager();

        // >= Android 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(Environment.isExternalStorageManager()) {
                return true;
            }
            // request MANAGE_EXTERNAL_STORAGE
            Intent reqPermIntent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:"+packageName));
            UnityPlayer.currentActivity.startActivity(reqPermIntent);
            MakeToast("Please grant the permission and restart the app!");
            return false;
        }
        else { // Android <= 10
            String[] perms = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            UnityPlayer.currentActivity.requestPermissions(perms, 0);
            return true;
        }
    }

    /**
     * Open apk by package name
     * @param packageName
     */
    public static void OpenApk(String packageName) {
        Log.d(TAG, "OpenApk "+packageName);
        Intent launchIntent = UnityPlayer.currentActivity.getPackageManager().getLaunchIntentForPackage(packageName);
        //launchIntent.putExtra("User_Info", user_info);
        UnityPlayer.currentActivity.startActivity(launchIntent);
    }

    /**
     * Show toast
     * @param msg
     */
    public static void MakeToast(String msg) {
        Log.d(TAG, "MakeToast msg="+msg);
        Toast.makeText(UnityPlayer.currentActivity, msg, Toast.LENGTH_LONG).show();
    }

}
