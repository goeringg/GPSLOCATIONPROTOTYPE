package com.example.prototype1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import java.io.IOException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    PendingIntent permissionIntent;
    UsbDevice device;
    FT_Device ftDev = null;
    D2xxManager ftD2xx;
    Context serial_context;
    UsbManager manager;
    UsbDeviceConnection connection;
    TextView textView;
    Toast toast;
    int iavailable = 0;
    byte[] readData;
    char[] readDataToText;
    public static final int readLength = 23;


    //map globals
    GoogleMap map;
    Marker petMarker;
    Marker phoneMarker;
    LocationListener locationListener;
    LocationManager mLocationManager;

    private static final String ACTION_USB_PERMISSION = "com.example.prototype1.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(ACTION_USB_PERMISSION.equals(action)){
                synchronized (this){
                    device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        if(device != null){
                            textView.setText("did it");
                            try{
                                ftD2xx = D2xxManager.getInstance(serial_context);
                                ftD2xx.createDeviceInfoList(serial_context);

                                ftDev = ftD2xx.openByIndex(serial_context, 0);

                                toast = Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_SHORT);
                                toast.show();

                                ftDev.setBaudRate(9600);

                                iavailable = ftDev.getQueueStatus();

                                if(iavailable > 0){
                                    if(iavailable > readLength){
                                        iavailable = readLength;
                                    }
                                }
                                readData = new byte[readLength];
                                readDataToText = new char[readLength];
                                ftDev.read(readData, iavailable);
                                for(int i = 0; i < iavailable; i++){
                                    readDataToText[i] = (char) readData[i];
                                }

                                //toast = Toast.makeText(getApplicationContext(), Arrays.toString(readDataToText), Toast.LENGTH_SHORT);
                                //toast.show();
                                String locationString = new String(readDataToText);
                                textView.setText(locationString);
                                //String locationString = Arrays.toString(readDataToText);
                                //locationString = locationString.substring(1, locationString.length()-1);
                                //textView.setText(locationString);

                                //Grayson's code to be written here!

                                String[] petLocationArray = locationString.split("/");

                                double petLat;
                                double petLong;
                                if(petLocationArray.length != 2){
                                    petLat = 33.72002356224238;
                                    petLong = -39.08091233839987;
                                }
                                else{
                                    petLat = Double.parseDouble(petLocationArray[0]);
                                    petLong = Double.parseDouble(petLocationArray[1]);
                                }
                                textView.setText(Double.toString(petLat) + "   " + Double.toString(petLong));

                                //map.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(petLat, petLong) , 14.0f));
                                petMarker.setPosition(new LatLng(petLat, petLong));

                            } catch (D2xxManager.D2xxException e) {
                                e.printStackTrace();
                                toast = Toast.makeText(getApplicationContext(), "cannot connect", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    }
                    else{
                        textView.setText("didn't work");
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, 1);

        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                LatLng phoneLocation = new LatLng(location.getLatitude(), location.getLongitude());
                phoneMarker.setPosition(new LatLng(10,10));
            
            }
        };

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

        serial_context = getApplicationContext();

        textView = findViewById(R.id.textView);
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
    }

    public void onClick(View v){
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            device = deviceIterator.next();
            textView.setText(device.getDeviceName());
        }
        if(device!=null){
            manager.requestPermission(device, permissionIntent);

            connection = manager.openDevice(device);

            //SerialReadAsyncTask serialReadAsyncTask = new SerialReadAsyncTask();
            //serialReadAsyncTask.execute("");
            //run thread task
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
            LatLng gunnison = new LatLng(10, 10);
        map.moveCamera( CameraUpdateFactory.newLatLngZoom(gunnison , 14.0f));

        petMarker = map.addMarker(new MarkerOptions().position(new LatLng(13,0)).title("pet")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        phoneMarker = map.addMarker(new MarkerOptions().position(gunnison).title("phone")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    public class SerialReadAsyncTask extends AsyncTask<String, String, String>{


        @Override
        protected String doInBackground(String... strings) {
            iavailable = ftDev.getQueueStatus();

            if(iavailable > 0){
                if(iavailable > readLength){
                    iavailable = readLength;
                }
            }
            readData = new byte[readLength];
            readDataToText = new char[readLength];
            ftDev.read(readData, iavailable);
            for(int i = 0; i < iavailable; i++){
                readDataToText[i] = (char) readData[i];
            }

            //toast = Toast.makeText(getApplicationContext(), Arrays.toString(readDataToText), Toast.LENGTH_SHORT);
            //toast.show();
            //textView.setText(Arrays.toString(readDataToText));
            publishProgress(Arrays.toString(readDataToText));
            return Arrays.toString(readDataToText);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String message = values[0];
            textView.setText(message);
        }
    }
}