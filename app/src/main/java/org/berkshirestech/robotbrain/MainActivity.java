package org.berkshirestech.robotbrain;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private static String TAG = MainActivity.class.getSimpleName();

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;

    public static boolean SERVICE_CONNECTED = false;

    private UsbSerialPort legPort;
    private UsbSerialPort headPort;
    private boolean legMode = true;

    private Socket socket;


    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        connectUSB();

        tts = new TextToSpeech(this,this);


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
        }else if(keyCode == KeyEvent.KEYCODE_ENTER){
            promptSpeechInput();
        }else{
            if(legMode){
                stopLegs();
                stopHead();
            }else{
                stopHead();
            }
        }
        return super.onKeyUp(keyCode, event);
    }



    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Talk to me.");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            say("Sorry, I'm not listening right now.");
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    doCommand(result.get(0).trim().toLowerCase());
                }
                break;
            }

        }
    }

    private void doCommand(String text){
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public void connectSocketIO() throws URISyntaxException {
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
                Log.d(TAG,"disconnected");
            }

        });
        socket.connect();
    }


    public void connectUSB(){
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

    private void say(String text){
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
