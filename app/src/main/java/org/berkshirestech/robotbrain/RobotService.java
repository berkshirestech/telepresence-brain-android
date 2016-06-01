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
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private UsbSerialPort motorPort;

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
        }else if(text.contains("go away")){
            runAwayAndCry();

        }else{
            say("I didn't understand you. did you say "+ text);
        }
    }

    public void dance(){
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

    public void kickstarter(){
        doCommands(
                cSay("exterminate. exterminate. exterminate."),
                cForward(4000),
                cWait(2000),
                cLeft(1000),
                cWait(1000),
                cSay("oh. I didn't see you there."),
                cWait(2000),
                cSay("I love humans. please support our kick starter"),
                cWait(3000),
                cRight(1000),
                cWait(500),
                cSay("exterminate. exterminate. exterminate."),
                cForward(8000)
        );
    }

    private RobotCommand cWait(int duration){
        return new RobotCommand(duration) {
            @Override
            void exec() {
                //do nothing
            }
        };
    }

    private RobotCommand cSay(final String text){
        return new RobotCommand(0) {
            @Override
            void exec() {
                say(text);
            }
        };
    }

    private RobotCommand cLeft(int duration){
        return new RobotCommand(duration) {
            @Override
            void exec() {
                goLeft();
            }
        };
    }

    private RobotCommand cRight(int duration){
        return new RobotCommand(duration) {
            @Override
            void exec() {
                goRight();
            }
        };
    }


    private RobotCommand cForward(int duration){
        return new RobotCommand(duration) {
            @Override
            void exec() {
                goForward();
            }
        };
    }

    private void doCommands(RobotCommand... commands){
        List<RobotCommand> commandList = new ArrayList<>(Arrays.asList(commands));
        commandList.remove(0).doCommand(commandList);
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

    public void goForward(){
        Log.i(TAG, "go forward");
        writeMotor("f");
    }

    public void goBack(){
        Log.i(TAG, "go back");
        writeMotor("b");
    }

    public void goLeft(){
        Log.i(TAG, "go left");
        writeMotor("l");
    }

    public void goRight(){
        Log.i(TAG, "go right");
        writeMotor("r");
    }

    public void stopLegs(){
        Log.i(TAG, "stopping legs");
        writeMotor("s");
    }

    public void stopHead(){
        Log.i(TAG, "stopping head");
        writeMotor("S");
    }

    public void goUp(){
        Log.i(TAG, "head up");
        writeMotor("U");
    }

    public void goHeadRight(){
        Log.i(TAG, "head right");
        writeMotor("R");
    }

    public void goHeadLeft(){
        Log.i(TAG, "head left");
        writeMotor("L");
    }

    public void goDown(){
        Log.i(TAG, "head down");
        writeMotor("D");
    }

    public void centerHead(){
        writeMotor("C");
    }

    public void writeMotor(String c){
        if(motorPort != null){
            try {
                motorPort.write(c.getBytes(), 1000);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
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
                    writeMotor(key);
                }else{
                    writeMotor(key);
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
                        if(code == 'm'){
                            //its the legs
                            Log.d("USB", "Found the motor port.");
                            motorPort = port;
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
        Log.i(TAG, "trying to say " + text);
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

    private abstract class RobotCommand {
        private int duration;

        public RobotCommand(int duration) {
            this.duration = duration;
        }

        public void doCommand(final List<RobotCommand> remainingCommands){
            this.exec();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLegs();
                    stopHead();
                    if(remainingCommands.size() > 0){
                        remainingCommands.remove(0).doCommand(remainingCommands);
                    }
                }
            }, duration);
        }

        abstract void exec();

    }
}
