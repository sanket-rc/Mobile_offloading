package com.android.mobile_application;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;


@SuppressWarnings("ALL")
public class Slave extends AppCompatActivity{
    ToggleButton bluetoothswitch;
    TextView quadrant;
    ImageButton listeningbutton;
    BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket serverSocket;
    TextView slavestatus;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    Gateway gateway,gatewaybattery,gatewaystatus,gatewaycalculate;
    String bluetooth_name;
    String response="0";
    ImageView capturedImage;
    AlertDialog alertDialog;
    int monitorflag=0;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }

    /**
     * @param encodedString
     * @return bitmap (from given string)
     */
    public Bitmap StringToBitMap(String encodedString){
        try {
            byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            return decodedByte;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    public Bitmap StringToBitMap(byte[] encodedString){
        try {

            Bitmap decodedByte = BitmapFactory.decodeByteArray(encodedString, 0, encodedString.length);
            return decodedByte;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slave);
        bluetoothswitch=findViewById(R.id.bluetoothswitch);
        listeningbutton=findViewById(R.id.listeningbutton);
        slavestatus=findViewById(R.id.statusslave);
        quadrant=findViewById(R.id.quadrant);
        listeningbutton.setEnabled(false);


        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter.isEnabled()){
            bluetoothswitch.setChecked(true);
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
            startActivity(discoverableIntent);
            listeningbutton.setEnabled(true);
        }

        bluetoothswitch.setOnClickListener(new View.OnClickListener() {
            private static final int REQUEST_ENABLE_BT = 100;

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
                    bluetoothAdapter.setName("SLAVE");
                    listeningbutton.setEnabled(true);
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
                    startActivity(discoverableIntent);
                }else if(bluetoothAdapter.isEnabled()){
                    bluetoothAdapter.disable();
                    slavestatus.setText("STATUS");
                    listeningbutton.setEnabled(false);
                }
            }

        });

        listeningbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("MYAPP", UUID.fromString("4d040779-adb7-434f-bd74-7d1885bb822d"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread t=new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothSocket socket=null;
                        while (socket==null)
                        {

                            try {

                                Message message=Message.obtain();
                                message.what=STATE_CONNECTING;
                                handler.sendMessage(message);
                                socket=serverSocket.accept();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Message message=Message.obtain();
                                message.what=STATE_CONNECTION_FAILED;
                                handler.sendMessage(message);


                            }

                            if(socket!=null)
                            {
                                Message message=Message.obtain();
                                message.what=STATE_CONNECTED;
                                handler.sendMessage(message);


                                ///FOR MESSAGE
                                gateway=new Gateway(socket,handler);
                                gateway.start();

                                //for caluclate
                                gatewaycalculate= new Gateway(socket,handler);
                                gatewaycalculate.start();

                                gatewaystatus=new Gateway(socket,handler);
                                gatewaystatus.start();

                                break;

                            }

                        }

                    }
                });
                t.start();

            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Do you wish to proceed?for the computaion");
        alertDialogBuilder.setPositiveButton("yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Toast.makeText(getApplicationContext(), "Start Battery Monitoring Power Tutor Application", Toast.LENGTH_LONG).show();
                        response="1";
                    }
                });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                response="0";
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();



    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){

                case STATE_LISTENING :
                    slavestatus.setText("LISTENING");
                    break;
                case  STATE_CONNECTING:
                    slavestatus.setText("CONNECTING");
                    break;
                case STATE_CONNECTED:
                    slavestatus.setText("CONNECTED");
                    break;
                case STATE_CONNECTION_FAILED:
                    slavestatus.setText("CONN FAILED");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte [])msg.obj;

                    String tempMsg = new String(readBuff,0,msg.arg1);

                    JsonObject jsonObject = new JsonParser().parse(tempMsg).getAsJsonObject();
                    bluetooth_name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");

                    if(jsonObject.has("classify_image"))
                    {
                        Toast.makeText(getApplicationContext(), jsonObject.get("quadrant").toString(), Toast.LENGTH_SHORT).show();
                        String doubleValues = jsonObject.get("mat_img").toString();
                        quadrant.setText("Quadrant: " + jsonObject.get("quadrant").toString() + "  Confidance values : " + doubleValues);
                    }
                    break;
            }
            return true;
        }
    });


}

