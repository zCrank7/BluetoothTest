package com.jash.bluetoothtest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements OnClickListener {
    private static final String uuid = "4e3a500b-1ba9-4c3f-a5fe-76cb46608b5f";
    private BluetoothAdapter bluetoothAdapter;
    private RecyclerView recycler;
    private DeviceAdapter adapter;
    private DeviceReceiver receiver;
    private Map<BluetoothDevice, BluetoothSocket> socketMap = new HashMap<>();
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    BluetoothDevice device = msg.getData().getParcelable(BluetoothDevice.EXTRA_DEVICE);
                    adapter.add(device);
                    break;
                case 1:
                    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private BluetoothServerSocket server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recycler = ((RecyclerView) findViewById(R.id.recycler));
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this, new ArrayList<BluetoothDevice>());
        recycler.setAdapter(adapter);
        //获得蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "本设备没有蓝牙模块", Toast.LENGTH_LONG).show();
            finish();
            
        }
        //检测蓝牙是否开启
        if (!bluetoothAdapter.isEnabled()){
            //调用方法开启
//            bluetoothAdapter.enable();
            //通过意图开启
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
        } else {
            //getBondedDevices 获得已配对的设备列表
            adapter.addAll(bluetoothAdapter.getBondedDevices());
            discovery();
        }

    }

    private void discovery(){
        //开始扫描
        bluetoothAdapter.startDiscovery();
        receiver = new DeviceReceiver(handler);
        //找到蓝牙设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        try {
            server = bluetoothAdapter.listenUsingRfcommWithServiceRecord("", UUID.fromString(uuid));
            new Thread(){
                @Override
                public void run() {
                    try {
                        BluetoothSocket socket;
                        while ((socket = server.accept()) != null){
                            BluetoothDevice device = socket.getRemoteDevice();
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                            Message message = handler.obtainMessage(0);
                            message.setData(bundle);
                            message.sendToTarget();
                            socketMap.put(device, socket);
                            new ReadThread(socket, handler).start();
//                            Log.d("BluetoothSocket", device.getName() + "");
//                            DataInputStream stream = new DataInputStream(socket.getInputStream());
//                            Log.d("BluetoothSocket", stream.readUTF());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recycler != null) {
            unregisterReceiver(receiver);
        }
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Set<Map.Entry<BluetoothDevice, BluetoothSocket>> entries = socketMap.entrySet();
        for (Map.Entry<BluetoothDevice, BluetoothSocket> entry:entries){
            try {
                entry.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            Toast.makeText(this, "开启成功", Toast.LENGTH_SHORT).show();
            adapter.addAll(bluetoothAdapter.getBondedDevices());
            discovery();
        } else {
            Toast.makeText(this, "开启失败", Toast.LENGTH_SHORT).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public void onClick(View v) {
        int position = recycler.getChildPosition(v);
        final BluetoothDevice item = adapter.getItem(position);
//        ParcelUuid[] uuids = item.getUuids();
//        if (uuids != null) {
//            for (ParcelUuid uuid:uuids){
//                Log.d("ParcelUuid", uuid.toString());
//            }
//        } else {
//            Log.d("ParcelUuid", "null");
//        }
        new Thread(){
            @Override
            public void run() {
                try {
                    BluetoothSocket socket = socketMap.get(item);
                    if (socket == null) {
                        socket = item.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
                        //发起一个连接
                        socket.connect();
                        new ReadThread(socket, handler).start();
                        socketMap.put(item, socket);
                    }
                    DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                    stream.writeUTF("发送测试");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
