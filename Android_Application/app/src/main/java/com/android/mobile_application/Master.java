package com.android.mobile_application;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class Master extends AppCompatActivity implements Serializable {

    private static final int REQUEST_ENABLE_BT = 100;
    BluetoothAdapter bluetoothAdapter;
    String Btname;
    ArrayAdapter<String> adapter;
    static ArrayAdapter<String> chosenadapter;
    BluetoothDevice bdDevice;
    ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
    ListView devicediscoverylist, chosendeviceslist;
    static TextView status;
    BluetoothDevice device;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static String phoneId;
    static List<String> accepteddevices;
    static Context c1;

    static HashMap<String, BluetoothSocket> map;

    private final BroadcastReceiver bluetoothDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
            {

                chosenadapter.add(device.getName()+"->Connected");
                phoneId=device.getName();
                chosenadapter.notifyDataSetChanged();
                Gateway gateway=new Gateway(map.get(device.getName()),handler);
            }else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Discover Finished", Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                chosenadapter.remove(device.getName() +"->Connected");
                status.setText("Disconnected from "+ device.getName());
                chosenadapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        ToggleButton bluetoothswitch = findViewById(R.id.Bluetoothswitch);
        Button bluetoothdiscovery = findViewById(R.id.bluetoothdiscovery);
        Button matrixmultiplication = findViewById(R.id.matrixmultiplication);
        arrayListBluetoothDevices = new ArrayList<BluetoothDevice>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
        devicediscoverylist = findViewById(R.id.devicediscoverylist);
        devicediscoverylist.setAdapter(adapter);
        chosendeviceslist = findViewById(R.id.chosendeviceslist);
        chosenadapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
        chosendeviceslist.setAdapter(chosenadapter);
        status = findViewById(R.id.status);
        map=new HashMap<String, BluetoothSocket>();
        accepteddevices=new ArrayList<>();
        c1=getApplicationContext();

        IntentFilter filter=new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        getApplicationContext().registerReceiver(this.bluetoothDeviceReceiver,filter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothdiscovery.setEnabled(false);

        // Bluetooth
        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter.isEnabled()){
            bluetoothswitch.setChecked(true);
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
            startActivity(discoverableIntent);
            bluetoothdiscovery.setEnabled(true);
        }

        bluetoothswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(),"The device doesn't support bluetooth",Toast.LENGTH_SHORT).show();
                    bluetoothswitch.setEnabled(false);
                    Intent intent =new Intent();
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    Toast.makeText(getApplicationContext(), "Going Back", Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                } else if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Btname=bluetoothAdapter.getName();
                    bluetoothAdapter.setName("MASTER");
                    bluetoothdiscovery.setEnabled(true);
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
                    startActivity(discoverableIntent);
                }else if(bluetoothAdapter.isEnabled()){
                    bluetoothAdapter.disable();
                    bluetoothdiscovery.setEnabled(false);
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    chosenadapter.clear();
                    status.setText("STATUS");
                    //logs.setText("LOGS");
                    chosenadapter.notifyDataSetChanged();
                }
            }

        });


        bluetoothdiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clear();
                arrayListBluetoothDevices.clear();
                getPairedDevices();
                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                Master.this.registerReceiver(myReceiver, intentFilter);
                bluetoothAdapter.startDiscovery();
            }
        });

        devicediscoverylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bdDevice = arrayListBluetoothDevices.get(position);
                String str=adapter.getItem(position);
                String[] values=str.split("->");
                if(values[2].equals("NotPaired")){
                    bluetoothAdapter.cancelDiscovery();
                    Boolean isBonded = false;
                    try {
                        isBonded = createBond(bdDevice);
                        if(isBonded){
                            adapter.remove(adapter.getItem(position));
                            adapter.insert(bdDevice.getName()+"->"+bdDevice.getAddress()+"->Paired",0);
                            adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(),"Connected to "+ bdDevice+" status: "+isBonded,Toast.LENGTH_SHORT).show();
                }else if(values[2].equals("Paired")){
                    bluetoothAdapter.cancelDiscovery();
                    device=arrayListBluetoothDevices.get(position);

                    try {
                        map.put(device.getName(),device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("4d040779-adb7-434f-bd74-7d1885bb822d")));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        map.get(device.getName()).connect();
                        Message message = Message.obtain();
                        message.what=STATE_CONNECTED;
                        handler.sendMessage(message);

                    } catch (IOException e) {
                    }


                }
            }
        });

        chosendeviceslist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    String[] str=chosenadapter.getItem(position).split("->");
                Gateway gateway=new Gateway(map.get(str[0]),handler);
                gateway.start();
                JSONObject jsonObjectnew = new JSONObject();
                try {
                    jsonObjectnew.put("ping","master ping");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String jsonString= jsonObjectnew.toString();
                gateway.write(jsonString.getBytes());
            }
        });
        matrixmultiplication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for(int i=0;i<chosenadapter.getCount();i++){
                    String[] str=chosenadapter.getItem(i).split("->");
                    accepteddevices.add(str[0]);
                }
                Intent intent;
                intent = new Intent(getApplicationContext(), Classification_Activity.class);
                startActivity(intent);
            }
        });
    }

    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        arrayListBluetoothDevices.clear();
        if(pairedDevice.size()>0)
        {
            for(BluetoothDevice device : pairedDevice)
            {
                adapter.add(device.getName()+"->"+device.getAddress()+"->Paired");
                arrayListBluetoothDevices.add(device);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(arrayListBluetoothDevices.size()<1)
                {
                    adapter.add(device.getName()+"->"+device.getAddress()+"->NotPaired");
                    arrayListBluetoothDevices.add(device);
                    adapter.notifyDataSetChanged();
                }
                else
                {
                    boolean flag = true;    // flag to indicate that particular device is already in the arlist or not
                    for(int i = 0; i<arrayListBluetoothDevices.size();i++)
                    {
                        if(device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress()))
                        {
                            flag = false;
                        }
                    }
                    if(flag)
                    {
                        adapter.add(device.getName()+"->"+device.getAddress() +"->NotPaired");
                        arrayListBluetoothDevices.add(device);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    public boolean createBond(BluetoothDevice btDevice) throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth ENABLED", Toast.LENGTH_LONG).show();
            }else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth NOT ENABLED", Toast.LENGTH_LONG).show();
                ToggleButton btn=findViewById(R.id.Bluetoothswitch);
                Button btn1=findViewById(R.id.bluetoothdiscovery);
                btn1.setEnabled(false);
                btn.setChecked(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }

    static Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if(msg.what==STATE_CONNECTED){
                status.setText("CONNECTED TO "+phoneId);
            }else if(msg.what==STATE_CONNECTION_FAILED){
                status.setText("CONNECTION FAILED");
            }
            return true;
        }
    });

}

