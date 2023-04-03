package com.example.siu039_controlremotodedispositivos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Variables globales usadas en la clase MainActivity
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
    FrameLayout cameraPreviewFrameLayout;
    Camera mCamera;
    CameraPreview mCameraPreview;
    static File mediaStorageDir;
    static File mediaFile;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    public static Handler handlerNetworkExecutorResult;
    NetworkExecutor networkExecutor;

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

    android.hardware.Camera.PictureCallback mPicture = new
            android.hardware.Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
                    byte[] resized = resizeImage(data);
                    File pictureFile = getOutputMediaFile();
                    if (pictureFile == null) {
                        return;
                    }
                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(resized);
                        fos.close();
                    } catch (Exception e) {
                        Log.e("onPictureTaken", "ERROR:" + e);
                    }
                }
            };



    @SuppressLint({"MissingInflatedId", "HandlerLeak"})
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
        cameraPreviewFrameLayout = (FrameLayout) findViewById(R.id.cameraView);
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        cameraPreviewFrameLayout = (FrameLayout) findViewById(R.id.cameraView);
        cameraPreviewFrameLayout.addView(mCameraPreview);
        handlerNetworkExecutorResult = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d("handlerNetworkExecute", (String) msg.obj);
                if (msg != null) {
                    if (msg.obj.equals("FORWARD")) {
                        forward();
                    } else if (msg.obj.equals("BACKWARD")) {
                        backward();
                    } else if (msg.obj.equals("LEFT")) {
                        left();
                    } else if (msg.obj.equals("RIGHT")) {
                        right();
                    } else if (msg.obj.equals("STOP")) {
                        stop();
                    } else if (msg.obj.equals("CAMERA")) {
                        captureCamera();
                    }
                }
            }
        };
        networkExecutor = new NetworkExecutor();
        networkExecutor.start();
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
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new
                                String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
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

    public void forward() {
        try {
            String tmpStr = "WHEELS+70+110";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void backward() {
        try {
            String tmpStr = "WHEELS+110+70";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void left() {
        try {
            String tmpStr = "WHEELS+110+90";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void right() {
        try {
            String tmpStr = "WHEELS+90+110";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void stop() {
        try {
            String tmpStr = "WHEELS+90+90";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    private android.hardware.Camera getCameraInstance() {
        android.hardware.Camera camera = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return getCameraInstance();
        } else {
            // Permisos concedidos, puedes usar la cámara
            try {
                camera = android.hardware.Camera.open(0);
            } catch (Exception e) {
                // cannot get camera or does not exist
                Log.d("getCameraInstance", "ERROR" + e);
            }
        }
        return camera;
    }

    private static File getOutputMediaFile() {
        if (mediaStorageDir == null){
            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SIU039-CameraController");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("SIU039-CameraController", "failed to create directory");
                    return null;
                }
            }
        }
        if (mediaFile==null) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG.jpg");
        }
        return mediaFile;
    }

    public void captureCamera(){
        if (mCamera!=null) {
            mCamera.takePicture(null, null, mPicture);
        }
    }

    byte[] resizeImage(byte[] input) {
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 80, 107,
                true);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        return blob.toByteArray();
    }
    public class NetworkExecutor extends Thread {

        private static final int HTTP_SERVER_PORT = 8082;
        final public int CODE_OK = 200;
        final public int CODE_BADREQUEST = 400;
        final public int CODE_FORBIDDEN = 403;
        final public int CODE_NOTFOUND = 404;
        final public int CODE_INTERNALSERVERERROR = 500;
        final public int CODE_NOTIMPLEMENTED = 501;
        String fileStr = readResourceTextFile();

        public void run() {
            Socket scliente = null;
            ServerSocket unSocket = null;
            while (true) {
                try {
                    unSocket = new ServerSocket(HTTP_SERVER_PORT); //Creamos el puerto
                    scliente = unSocket.accept(); //Aceptando conexiones del navegador Web
                    System.setProperty("line.separator", "\r\n");
                    //Creamos los objetos para leer y escribir en el socket
                    BufferedReader in = new BufferedReader(new InputStreamReader(scliente.getInputStream()));
                    PrintStream out = new PrintStream(new BufferedOutputStream(scliente.getOutputStream()));
                    //Leemos el comando que ha sido enviado por el servidor web
                    //Ejemplo de comando: GET /index.html HTTP\1.0
                    String cadena = in.readLine();

                    StringTokenizer st = new StringTokenizer(cadena);
                    String commandString = st.nextToken().toUpperCase();
                    if (commandString.equals("GET")) {
                        String urlObjectString = st.nextToken();
                        Log.v("urlObjectString", urlObjectString);
                        if (urlObjectString.toUpperCase().startsWith("/INDEX.HTML") ||
                                urlObjectString.toUpperCase().equals("/INDEX.HTM") ||
                                urlObjectString.equals("/")) {
                            String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                            out.print(headerStr);
                            out.println(fileStr);
                            out.flush();
                        }
                        if (urlObjectString.toUpperCase().startsWith("/FORWARD")) {
                            String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                            out.print(headerStr);
                            out.println(fileStr);
                            out.flush();
                        }
                        if (urlObjectString.toUpperCase().startsWith("/CAMERA.JPG") ||
                                urlObjectString.toUpperCase().startsWith("/CAMERA.")) {
                            File cameraFile = getOutputMediaFile();
                            FileInputStream fis = null;
                            boolean exist = true;
                            try {
                                fis = new FileInputStream(cameraFile);
                            } catch (FileNotFoundException e) {
                                exist = false;
                            }
                            if (exist) {
                                String headerStr = getHTTP_Header(CODE_OK, "image/jpeg", (int) cameraFile.length());
                                out.print(headerStr);
                                byte[] buffer = new byte[4096];
                                int n;
                                while ((n = fis.read(buffer)) > 0) { // enviar archivo
                                    out.write(buffer, 0, n);
                                }
                                out.flush();
                                out.close();
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private String getHTTP_Header(int headerStatusCode, String headerContentType, int
                headerFileLength) {
            String result = getHTTP_HeaderStatus(headerStatusCode) +
                    "\r\n" +
                    getHTTP_HeaderContentLength(headerFileLength)+
                    getHTTP_HeaderContentType(headerContentType)+
                    "\r\n";
            return result;
        }

        private String getHTTP_HeaderStatus(int headerStatusCode){
            String result = "";
            switch (headerStatusCode) {
                case CODE_OK:
                    result = "200 OK"; break;
                case CODE_BADREQUEST:
                    result = "400 Bad Request"; break;
                case CODE_FORBIDDEN:
                    result = "403 Forbidden"; break;
                case CODE_NOTFOUND:
                    result = "404 Not Found"; break;
                case CODE_INTERNALSERVERERROR:
                    result = "500 Internal Server Error"; break;
                case CODE_NOTIMPLEMENTED:
                    result = "501 Not Implemented"; break;
            }
            return ("HTTP/1.0 "+result);
        }
        private String getHTTP_HeaderContentLength(int headerFileLength){
            return "Content-Length: " + headerFileLength + "\r\n";
        }
        private String getHTTP_HeaderContentType(String headerContentType){
            return "Content-Type: "+headerContentType+"\r\n";
        }
        public String readResourceTextFile() {
            String fileStr = "";
            InputStream is = getResources().openRawResource(R.raw.index);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String readLine = null;
            try {
                while ((readLine = br.readLine()) != null) {
                    fileStr = fileStr + readLine + "\r\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileStr;
        }
    }

}