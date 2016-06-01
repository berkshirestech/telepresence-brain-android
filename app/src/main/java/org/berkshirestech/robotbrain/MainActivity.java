package org.berkshirestech.robotbrain;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{
    private static String TAG = MainActivity.class.getSimpleName();

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private boolean legMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "key event: " + event);
        if(event.getRepeatCount() == 0){
            if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                if (legMode){
                    mService.goBack();
                }else{
                    mService.goUp();
                }
                return true;

            }else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                if(legMode){
                    mService.goLeft();
                }else{
                    mService.goHeadLeft();
                }
                return true;

            }else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                if(legMode){
                    mService.goRight();
                }else{
                    mService.goHeadRight();
                }
                return true;

            }else if(keyCode == KeyEvent.KEYCODE_DPAD_UP){
                if(legMode){
                    mService.goForward();
                }else{
                    mService.goDown();
                }
                return true;

            }
        }
        return false;
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        Log.i(TAG, "key event: " + event);
        if(keyCode == KeyEvent.KEYCODE_ENTER){
            promptSpeechInput();
        }else if(keyCode == KeyEvent.KEYCODE_1) {
            mService.say("Leg mode on!");
            legMode = true;
        }else if(keyCode == KeyEvent.KEYCODE_2){
            mService.say("Head mode on!");
            legMode = false;
        }else if(keyCode == KeyEvent.KEYCODE_3 || keyCode == KeyEvent.KEYCODE_M){
            mService.say("Connecting to motors");
            mService.connectUSB();
        }else if(keyCode == KeyEvent.KEYCODE_4){
            mService.say("Connecting to internet");
            try {
                mService.connectSocketIO();
                openWebPage();
            } catch (URISyntaxException e) {
                Log.e(TAG, e.getMessage());
            }
        }else if(keyCode == KeyEvent.KEYCODE_5){
            mService.centerHead();
        }else if(keyCode == KeyEvent.KEYCODE_6){
            mService.kickstarter();
        }else if(keyCode == KeyEvent.KEYCODE_E){
            mService.say("Excuse me");
        }else if(keyCode == KeyEvent.KEYCODE_D){
            mService.dance();
        }else if(keyCode == KeyEvent.KEYCODE_H){
            mService.say("Hello");
        }else{
            if(legMode){
                mService.stopLegs();
                mService.stopHead();
            }else{
                mService.stopHead();
            }
        }
        return super.onKeyUp(keyCode, event);
    }


    private void openWebPage(){
        Uri webpage = Uri.parse("https://warm-eyrie-7840.herokuapp.com/?host=true");
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
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
            if(mBound){
                mService.say("Sorry, I'm not listening right now.");
            }
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(!mBound) return;

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mService.doCommand(result.get(0).trim().toLowerCase());
                }
                break;
            }

        }
    }




    private RobotService mService;
    boolean mBound = false;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, RobotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RobotService.RobotBinder binder = (RobotService.RobotBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

}
