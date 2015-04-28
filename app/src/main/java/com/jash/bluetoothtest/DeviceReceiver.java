package com.jash.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by jash on 15-4-22.
 */
public class DeviceReceiver extends BroadcastReceiver {
    private Handler handler;

    public DeviceReceiver(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DeviceReceiver", intent.toString());
        BluetoothDevice extra = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Log.d("DeviceReceiver", extra.getName() + "");
        Log.d("DeviceReceiver", extra.getAddress());
        Message message = handler.obtainMessage(0);
        message.setData(intent.getExtras());
        message.sendToTarget();

    }
}
