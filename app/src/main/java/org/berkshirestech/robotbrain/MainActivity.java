package org.berkshirestech.robotbrain;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getSimpleName();

    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;

    public static boolean SERVICE_CONNECTED = false;

    private UsbSerialPort legPort;
    private UsbSerialPort headPort;
    private boolean legMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        newCheckUsb();

        FloatingActionButton forwardButton = (FloatingActionButton) findViewById(R.id.forward);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    legPort.write("f".getBytes(), 1000);
                } catch (IOException e) {
                    Log.e("USB", e.getMessage());
                }
            }
        });

        FloatingActionButton stopButton = (FloatingActionButton) findViewById(R.id.stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    legPort.write("s".getBytes(), 1000);
                } catch (IOException e) {
                    Log.e("USB", e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.i(TAG, "key event: " + event);
        if(event.getRepeatCount() == 0){
            if(keyCode == KeyEvent.KEYCODE_DPAD_UP){
                pressUp();
                return false;

            }else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                pressLeft();
                return false;

            }else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                pressRight();
                return false;

            }else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                pressDown();
                return false;

            }
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        Log.i(TAG, "key event: " + event);
        if(keyCode == KeyEvent.KEYCODE_1){
            legMode = true;
        }else if(keyCode == KeyEvent.KEYCODE_2){
            legMode = false;
        }else{
            if(legMode){
                stopLegs();
            }else{
                stopHead();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    public void pressUp(){
        if(legMode){
            goForward();
        }else{
            goUp();
        }
    }

    public void pressDown(){
        if(legMode){
            goBack();
        }else{
            goDown();
        }
    }

    public void pressRight(){
        if(legMode){
            goRight();
        }else{
            goHeadRight();
        }
    }

    public void pressLeft(){
        if(legMode){
            goLeft();
        }else{
            goHeadLeft();
        }
    }

    public void goForward(){
        Log.i(TAG, "go forward");
        writeLeg("f");
    }

    public void goBack(){
        Log.i(TAG, "go back");
        writeLeg("b");
    }

    public void goLeft(){
        Log.i(TAG, "go left");
        writeLeg("l");
    }

    public void goRight(){
        Log.i(TAG, "go right");
        writeLeg("r");
    }

    public void stopLegs(){
        Log.i(TAG, "stopping legs");
        writeLeg("s");
    }

    public void stopHead(){
        Log.i(TAG, "stopping head");
        writeHead("S");
    }

    public void goUp(){
        Log.i(TAG, "head up");
        writeHead("U");
    }

    public void goHeadRight(){
        Log.i(TAG, "head right");
        writeHead("R");
    }

    public void goHeadLeft(){
        Log.i(TAG, "head left");
        writeHead("L");
    }

    public void goDown(){
        Log.i(TAG, "head down");
        writeHead("D");
    }

    public void writeLeg(String c){
        if(legPort != null){
            try {
                legPort.write(c.getBytes(), 1000);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


    public void writeHead(String c){
        if(headPort != null){
            try {
                headPort.write(c.getBytes(), 1000);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public void checkUsb(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            //your code
            Log.d("USB", device.getDeviceName());

        }
    }


    public void newCheckUsb(){
        Log.d("USB", "Checking USB");
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.d("USB", "No connections");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Log.d("USB", "You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)");
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }

        for(UsbSerialPort port : driver.getPorts()){

            try {
                port.open(connection);
                port.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                byte buffer[] = new byte[8];

                port.write("h".getBytes(), 1000);

                int numBytesRead = port.read(buffer, 1000);
                if(numBytesRead > 0){
                    char code = (char) buffer[0];
                    if(code == 'l'){
                        //its the legs
                        Log.d("USB", "Found the leg port.");
                        legPort = port;
                    }else if(code == 'h'){
                        //its the legs
                        Log.d("USB", "Found the head port.");
                        headPort = port;
                    }
                }

            } catch (IOException e) {
                // Deal with error.
                throw new RuntimeException(e);
            }
        }
    }

//    private static final String ACTION_USB_PERMISSION =
//            "com.android.example.USB_PERMISSION";
//    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
//
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (ACTION_USB_PERMISSION.equals(action)) {
//                synchronized (this) {
//                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        if(device != null){
//                            //call method to set up device communication
//                        }
//                    }
//                    else {
//                        Log.d("USB", "permission denied for device " + device);
//                    }
//                }
//            }
//        }
//    };
}
