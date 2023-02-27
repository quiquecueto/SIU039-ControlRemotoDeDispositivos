package com.example.siu039_controlremotodedispositivos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button connectButton;
    Button disconnectButton;
    Button forwardButton;
    Button backwardButton;
    Button turnLeftForwardButton;
    Button turnRightForwardButton;
    TextView statusLabel;
    BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    public static Boolean bluetoothActive = false;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    OutputStream outputStream;
    InputStream inputStream;
    BluetoothSocket btSocket;

    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Guardamos el nombre del dispositivo descubierto
            String remoteDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            //Guardamos el objeto Java del dispositivo descubierto, para poder conectar.
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //Leemos la intensidad de la radio con respecto a este dispositivo bluetooth
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
            //Guardamos el dispositivo encontrado en la lista
            deviceList.add(remoteDevice);
            //Mostramos el evento en el Log.
            Log.d("MyFirstApp", "Discovered "+ remoteDeviceName);
            Log.d("MyFirstApp", "RSSI "+ rssi + "dBm");
            if (remoteDeviceName != null && remoteDeviceName.equals("SUM_SCH3")) {
                Log.d("onReceive", "Discovered SUM_SCH3:connecting");
                connect(remoteDevice);
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = (Button) findViewById(R.id.connectButton);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        forwardButton = (Button) findViewById(R.id.forButton);
        backwardButton= (Button) findViewById(R.id.backButton);
        turnLeftForwardButton= (Button) findViewById(R.id.leftButton);
        turnRightForwardButton= (Button) findViewById(R.id.rightButton);
        statusLabel = (TextView) findViewById(R.id.textButton);


    }
    @SuppressLint("MissingPermission")
    public void onClickConnectButton(View view){
        if (bluetooth.isEnabled()){
            bluetoothActive = true;
            checkBTPermissions();
            startDiscovery();
            @SuppressLint({"MissingPermission", "HardwareIds"}) String address = bluetooth.getAddress();
            @SuppressLint("MissingPermission") String name = bluetooth.getName();
            //Mostramos la datos en pantalla (The information is shown in the screen)
            Toast.makeText(getApplicationContext(),"Bluetooth ENABLED:"+name+":"+address,
                    Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth NOT enabled",
                    Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) //Bluetooth permission request code
            if (resultCode == RESULT_OK) {
                bluetoothActive = true;
                Toast.makeText(getApplicationContext(), "User Enabled Bluetooth",
                        Toast.LENGTH_SHORT).show();
            } else {
                bluetoothActive = false;
                Toast.makeText(getApplicationContext(), "User Did not enable Bluetooth",
                        Toast.LENGTH_SHORT).show();
            }
    }
    @SuppressLint("MissingPermission")
    private void startDiscovery(){
        Toast.makeText(getApplicationContext(),"Bluetooth ENABLED:"+bluetoothActive,
                Toast.LENGTH_SHORT).show();
        if (bluetoothActive){
        //Borramos la lista de dispositivos anterior
            deviceList.clear();
            //Activamos un Intent Android que avise cuando se encuentre un dispositivo
            //NOTA: <<discoveryResult>> es una clase <<callback>> que describiremos en
            //el siguiente paso

            registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            //Ponemos el adaptador bluetooth en modo <<Discovery>>
            bluetooth.startDiscovery();
        }
    }

    public void checkBTPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new
                                String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
                    }
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }
    }
    @SuppressLint("MissingPermission")
    protected void connect(BluetoothDevice device) {
        try {
            btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            Log.d("connect", "Client connected");
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
        }catch (Exception e) {
            Log.e("ERROR: connect", ">>", e);
        }
    }

    public void forward(View view) {
        try {
            String tmpStr = "WHEELS+70+110";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void backward(View view) {
        try {
            String tmpStr = "WHEELS+110+70";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void left(View view) {
        try {
            String tmpStr = "WHEELS+110+90";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void right(View view) {
        try {
            String tmpStr = "WHEELS+90+110";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void stop(View view) {
        try {
            String tmpStr = "WHEELS+90+90";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

}