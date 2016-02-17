package org.berkshirestech.robotbrain;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by russellleggett on 1/26/16.
 */
public class RobotService extends Service  implements TextToSpeech.OnInitListener{
    private static String TAG = RobotService.class.getSimpleName();
    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;

    public static boolean SERVICE_CONNECTED = false;

    private UsbSerialPort legPort;
    private UsbSerialPort headPort;
    private boolean legMode = true;

    private Socket socket;
    private TextToSpeech tts;







    // Binder given to clients
    private final IBinder mBinder = new RobotBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class RobotBinder extends Binder {
        RobotService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RobotService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        connectUSB();

        tts = new TextToSpeech(this,this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Returns true if it handled the event
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean handleKeyDown(int keyCode, KeyEvent event) {
//        Log.i(TAG, "key event: " + event);
        if(event.getRepeatCount() == 0){
            if(keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP){
                pressUp();
                return true;

            }else if(keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT){
                pressLeft();
                return true;

            }else if(keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT){
                pressRight();
                return true;

            }else if(keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN){
                pressDown();
                return true;

            }
        }
        return false;
    }


    public boolean handleKeyUp(int keyCode, KeyEvent event) {
//        Log.i(TAG, "key event: " + event);
        if(keyCode == KeyEvent.KEYCODE_1){
            say("Leg mode on!");
            legMode = true;
        }else if(keyCode == KeyEvent.KEYCODE_2){
            say("Head mode on!");
            legMode = false;
        }else if(keyCode == KeyEvent.KEYCODE_3){
            say("Connecting to motors");
            connectUSB();
        }else if(keyCode == KeyEvent.KEYCODE_4){
            say("Connecting to internet");
            try {
                connectSocketIO();
            } catch (URISyntaxException e) {
                Log.e(TAG, e.getMessage());
            }
        }else if(keyCode == KeyEvent.KEYCODE_5){
            centerHead();
        }else{
            if(legMode){
                stopLegs();
                stopHead();
            }else{
                stopHead();
            }
        }
        return false;
    }


    public void doCommand(String text){
        if(text.contains("dance")){
            dance();
        }else if(text.contains("center head")){
            centerHead();
            say("yes human");
        }else if(text.contains("make a sandwich")){
            say("I'm afraid I can't do that");
        }else if(text.contains("tell me a joke")){
            say("0110111001101111");
        }else if(text.contains("hello robot")){
            say("die humans");
        }else if(text.contains("peanut butter")){
            say("jelly time");
        }else if(text.contains("tv")){
            say("you mean remote control");
        }else if(text.contains("i love you")){
            say("what is love");
        }else if(text.contains("i hate you")){
            runAwayAndCry();

        }else{
            say("I didn't understand you. did you say "+ text);
        }
    }

    private void dance(){
        say("commencing dance");
        goLeft();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopLegs();
                say("that's enough, humans");
            }
        }, 5000);
    }


    private void runAwayAndCry(){
        say("fine I'm leaving");
        goLeft();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopLegs();
                say("boo hoo hoo");
            }
        }, 3000);
    }

    public void pressUp(){
        if (legMode){
            goBack();
        }else{
            goUp();
        }
    }

    public void pressDown(){
        if(legMode){
            goForward();
        }else{
            goDown();
        }
    }

    public void pressRight(){
        if(legMode){
            goRight();
            goHeadLeft();
        }else{
            goHeadRight();
        }
    }

    public void pressLeft(){
        if(legMode){
            goLeft();
            goHeadRight();
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

    public void centerHead(){
        writeHead("C");
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
    private void connectSocketIO() throws URISyntaxException {
        socket = IO.socket("http://warm-eyrie-7840.herokuapp.com/");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG,"connected");
                socket.emit("imarobot");
            }

        }).on("keydown", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG,"keydown");
                String key = args[0].toString();
                if(key.equals(key.toUpperCase())){
                    //its an uppercase letter, therefore its for the head
                    writeHead(key);
                }else{
                    writeLeg(key);
                }
            }

        }).on("stop", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG,"stop");
                stopLegs();
                stopHead();
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "disconnected");
            }

        });
        socket.connect();
    }


    private void connectUSB(){
        Log.d("USB", "Checking USB");
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.d("USB", "No connections");
            return;
        }

        // Open a connection to the first available driver.
        for(UsbSerialDriver driver : availableDrivers){

            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                Log.d("USB", "You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)");
                // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
                return;
            }

            for(UsbSerialPort port : driver.getPorts()){
                Log.d(TAG, "found a port: " + port.getPortNumber());
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
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                say("Text to speech is working");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    public void say(String text){
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
