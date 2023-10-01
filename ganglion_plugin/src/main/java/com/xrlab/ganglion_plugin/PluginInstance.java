package com.xrlab.ganglion_plugin;

import static android.content.Context.BIND_AUTO_CREATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import java.util.List;
import java.util.Objects;
import java.util.Timer;

//import static com.unity3d.player.UnityPlayer.UnitySendMessage;

@SuppressLint("MissingPermission")
public class PluginInstance {
    private static final String TAG = "LabFrame_GanglionPlugin_PluginInstance";
    private static Activity unityActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mIsDeviceGanglion;
    private boolean mScanning;
    BluetoothLeScanner scanner;
    private BluetoothService mBluetoothLeService;
    private static int mCommandIdx = 0;

    public boolean mConnected = false;
    public boolean mConnecting = false;

    public boolean mUseEeg = false;
    public boolean mUseImpedance = false;

    public  String mPreferredDeviceName = "";
    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothGattCharacteristic mNotifyOnRead;
    private BluetoothGattCharacteristic mGanglionSend;

    //Ganglion Service/Characteristics UUIDs (SIMBLEE Chip Defaults)
    public final static String UUID_GANGLION_SERVICE = "0000fe84-0000-1000-8000-00805f9b34fb";
    public final static String UUID_GANGLION_RECEIVE = "2d30c082-f39f-4ce6-923f-3484ea480596";
    public final static String UUID_GANGLION_SEND = "2d30c083-f39f-4ce6-923f-3484ea480596";
    public final static String UUID_GANGLION_DISCONNECT = "2d30c084-f39f-4ce6-923f-3484ea480596";

    public static void receiveUnityActivity(Activity act) {
        unityActivity = act;
    }

    public final void SetPreferredGanglionName(String s) // called by unity
    {
        mPreferredDeviceName = s;
    }
    public final void StreamImpedance() // called by unity
    {
        // send
        mUseEeg = true;
        //char cmd = (char) mImpedanceCommands[mImpedanceCommandIdx];
        char cmd = (char) 'z';
        Log.i(TAG, "Sending Command : " + cmd);
        mGanglionSend.setValue(new byte[]{(byte) cmd});
        mBluetoothLeService.writeCharacteristic((mGanglionSend));
        // mImpedanceCommandIdx = (mImpedanceCommandIdx + 1) % mImpedanceCommands.length; //update for next run to toggle off
    }
    public final void StopStreamImpedance() // called by unity
    {
        // send
        mUseEeg = false;
        //char cmd = (char) mImpedanceCommands[mImpedanceCommandIdx];
        char cmd = (char) 'Z';
        Log.i(TAG, "Sending Command : " + cmd);
        mGanglionSend.setValue(new byte[]{(byte) cmd});
        mBluetoothLeService.writeCharacteristic((mGanglionSend));
        // mImpedanceCommandIdx = (mImpedanceCommandIdx + 1) % mImpedanceCommands.length; //update for next run to toggle off
    }
    public final void StreamData() // call by Unity, start get data from ganglion
    {
        // send
        mUseImpedance = true;
        char cmd = (char) 'b';
        Log.i(TAG, "Sending Command : " + cmd);
        mGanglionSend.setValue(new byte[]{(byte) cmd});
        mBluetoothLeService.writeCharacteristic((mGanglionSend));
        // mCommandIdx = (mCommandIdx + 1) % mCommands.length; //update for next run to toggle off
    }
    public final void StopStreamData() // call by Unity, start get data from ganglion
    {
        // send
        mUseImpedance = false;
        char cmd = (char) 's';
        Log.i(TAG, "Sending Command : " + cmd);
        mGanglionSend.setValue(new byte[]{(byte) cmd});
        mBluetoothLeService.writeCharacteristic((mGanglionSend));
        // mCommandIdx = (mCommandIdx + 1) % mCommands.length; //update for next run to toggle off
    }
    public final void Disconnect() // call by Unity
    {
        mBluetoothLeService.disconnect();
    }


    public final void Init()
    {
        if(mScanning || mConnecting)
        {
            Log.i(TAG,"Is already Scanning/Connecting");
            return;
        }

        unityActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(unityActivity, "error_bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();

        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        scanner.startScan(scanCallback);
        Log.i(TAG, "Start Scanning");
        mScanning = true;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothDevice bluetoothDevice = result.getDevice();
            unityActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothDevice == null || bluetoothDevice.getName() == null)
                        return;

                    String deviceName = bluetoothDevice.getName();
                    String deviceAddr = bluetoothDevice.getAddress();
                    mIsDeviceGanglion = bluetoothDevice.getName().toUpperCase().contains("GANGLION");

                    // Check is (preferred) Ganglion
                    if (!deviceName.contains("Ganglion")) {
                        Log.i(TAG, "BT Scan device = " + deviceName + deviceAddr);
                        return;
                    }
                    if(!mPreferredDeviceName.isEmpty() && !deviceName.contains(mPreferredDeviceName)) {
                        Log.i(TAG, "BT Found unpreferred Ganglion device = " + deviceName + deviceAddr);
                        return;
                    }

                    Log.i(TAG, "BT Found Ganglion device = " + deviceName + deviceAddr);
                    scanner.stopScan(scanCallback);
                    mScanning = false;
                    mConnecting = true;

                    mDeviceName = deviceName;
                    mDeviceAddress = deviceAddr;

                    unityActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

                    Intent gattServiceIntent = new Intent(unityActivity, BluetoothService.class);
                    unityActivity.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                    Log.i(TAG, "Created Service to Handle all further BLE Interactions");
                }
            });
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "onScanFailed， errorCode = " + errorCode);
            mScanning = false;
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "componentName: " + componentName);
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.i(TAG, "Trying to connect to GATTServer on: " + mDeviceName + " Address: " + mDeviceAddress);
            mBluetoothLeService.connect(mDeviceAddress);
            mCommandIdx = 0;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "Disconnecting from");
            mBluetoothLeService = null;
        }
    };

    private boolean setCharacteristicNotification(BluetoothGattCharacteristic currentNotify, BluetoothGattCharacteristic newNotify, String toastMsg) {
        if (currentNotify == null) {//none registered previously
            mBluetoothLeService.setCharacteristicNotification(newNotify, true);
        } else {//something was registered previously
            if (!currentNotify.getUuid().equals(newNotify.getUuid())) {//we are subscribed to another characteristic?
                mBluetoothLeService.setCharacteristicNotification(currentNotify, false);//unsubscribe
                mBluetoothLeService.setCharacteristicNotification(newNotify, true); //subscribe to Receive
            } else {
                //no change required
                return false;
            }
        }
        Toast.makeText(unityActivity, "Notify: " + toastMsg, Toast.LENGTH_SHORT).show();
        return true;//indicates reassignment needed for mNotifyOnRead
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "GattServer Connected");
                mConnected = true;
                mConnecting = false;
                if(mUseImpedance || mUseEeg)
                    new Handler(Looper.getMainLooper()).postDelayed(()->{
                        if(mUseEeg) {
                            Log.i(TAG, "Resume EEG");
                            StreamData();
                        }
                        if(mUseImpedance) {
                            Log.i(TAG, "Resume Impedance");
                            StreamImpedance();
                        }
                    }, 2000);

            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.v(TAG, "GattServer Disconnected");
                mConnected = false;

            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.v(TAG, "GattServer Services Discovered");
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                String dataType = intent.getStringExtra(BluetoothService.DATA_TYPE);
                if (Objects.equals(dataType, "RAW")) {
                    int[] samples = intent.getIntArrayExtra(BluetoothService.SAMPLE_ID);
                    int[] intentData1 = intent.getIntArrayExtra(BluetoothService.FULL_DATA_1);
                } else if (Objects.equals(dataType, "19BIT")) {
                    int[] samples = intent.getIntArrayExtra(BluetoothService.SAMPLE_ID);
                    int[] intentData1 = intent.getIntArrayExtra(BluetoothService.FULL_DATA_1);
                    int[] intentData2 = intent.getIntArrayExtra(BluetoothService.FULL_DATA_2);
                } else {
                    //handle this
                }

            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        for (BluetoothGattService gattService : gattServices) {
            Log.i(TAG, "Service Iterator: " + gattService.getUuid());

            if (mIsDeviceGanglion) {////we only want the SIMBLEE SERVICE, rest, we junk...
                if (!UUID_GANGLION_SERVICE.equals(gattService.getUuid().toString())) continue;
            }

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();

                //if this is the read attribute for Cyton/Ganglion, register for notify service
                if (SampleGattAttributes.UUID_GANGLION_RECEIVE.equals(uuid)) {//the RECEIVE characteristic
                    Log.i(TAG, "Registering notify for: " + uuid);
                    //we set it to notify, if it isn't already on notify
                    if (mNotifyOnRead == null) {
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        mNotifyOnRead = gattCharacteristic;
                    } else {
                        Log.v(TAG, "De-registering Notification for: " + mNotifyOnRead.getUuid().toString() + " first");
                        mBluetoothLeService.setCharacteristicNotification(mNotifyOnRead, false);
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        mNotifyOnRead = gattCharacteristic;
                    }
                }

                if (UUID_GANGLION_SEND.equals(uuid)) {//the RECEIVE characteristic
                    Log.i(TAG, "GANGLION SEND: " + uuid);
                    mGanglionSend = gattCharacteristic;
                }
            }
        }
    }

    private void updateConnectionState(final int resourceId) {
        unityActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

}
