package com.soundplay.martin.oreillytuner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity {

    private PdUiDispatcher pdUiDispatcher;
    private static  final String TAG="OReillyTuner";
    private PdService pdService = null;
    private ServiceConnection pdConnection;
    private TextView pitchLabelTextView;
    private TextView userMessageView;
    private PitchView pitchView;
    private final int RECORD_AUDIO_PERMISSION = 1;
    private final int READ_PHONE_STATE_PERMISSION=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGUI();

        if(ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.RECORD_AUDIO},RECORD_AUDIO_PERMISSION);
        }

        if(ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_PERMISSION);
        }

        pdConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                pdService = ((PdService.PdBinder)service).getService();
                try{
                    initPd();
                    loadPatch();
                }catch(IOException e){
                    Log.d("TAG", e.toString());
                    finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(new Intent(this, PdService.class),pdConnection,BIND_AUTO_CREATE);
        initSystemServices();
    }

    @Override
    public void onRequestPermissionsResult(int RequestCode, String perissions[],int[] grantResults){
        switch(RequestCode){
            case RECORD_AUDIO_PERMISSION:
                if(grantResults.length>0 && grantResults[0]!= PackageManager.PERMISSION_GRANTED){

                }
                break;
            case READ_PHONE_STATE_PERMISSION:
                if(grantResults.length>0 && grantResults[0]!= PackageManager.PERMISSION_GRANTED){

                }
                break;
        }
    }

    private String[] noteStrings = new String[] {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    private int midiFromPitch(float frequency ) {
        double noteNum = 12 * (Math.log( frequency / 440 )/Math.log(2) );
        return (int) (Math.round( noteNum ) + 69);
    }

    private String noteFromPitch(float frequency ) {
        double noteNum = 12 * (Math.log( frequency / 440 )/Math.log(2) );
        return noteStrings[(int) (Math.round( noteNum ) + 69) % 12];
    }

    private double frequencyFromNoteNumber(int note ) {
        return 440 * Math.pow(2,(double) (note-69)/ (double) 12);
    }


    private double centsOffFromPitch(double frequency, int note ) {
        return Math.floor( Math.log( frequency / frequencyFromNoteNumber( note ))/Math.log(2) );
        //return frequency - frequencyFromNoteNumber(note);
    }


    private void initGUI(){

        setContentView(R.layout.activity_main);
        pitchView = (PitchView) findViewById(R.id.pitchGraph);
        Button eButton = (Button)findViewById(R.id.e_button);

        eButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerNote(52);
                pitchView.setCenterPitch(52);
            }
        });
        Button aButton = (Button)findViewById(R.id.a_button);
        aButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerNote(57);
                pitchView.setCenterPitch(57);
            }
        });
        Button dButton = (Button)findViewById(R.id.d_button);
        dButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerNote(62);
                pitchView.setCenterPitch(62);
            }
        });
        Button gButton = (Button)findViewById(R.id.g_button);
        gButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerNote(67);
                pitchView.setCenterPitch(67);
            }
        });
        Button bButton = (Button)findViewById(R.id.b_button);
        bButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerNote(71);
                pitchView.setCenterPitch(71);
            }
        });
        Button e2Button = (Button)findViewById(R.id.e2_button);
        e2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerNote(76);
                pitchView.setCenterPitch(76);
            }
        });
        pitchLabelTextView = (TextView) findViewById(R.id.pitchLabel);
        userMessageView = (TextView) findViewById(R.id.userMessage);

    }


    private void initPd() throws IOException{
        int sampleRate = AudioParameters.suggestSampleRate();
        pdService.initAudio(sampleRate, 1, 1, 10.0f);
        //pdService.initAudio(sampleRate);
        start();
        pdService.startAudio();

        pdUiDispatcher = new PdUiDispatcher();

        pdUiDispatcher.addListener("pitch", new PdListener() {
            @Override
            public void receiveBang(String source) {
                Log.d(TAG, "Incoming Bang from: "+source);
                pitchLabelTextView.setText(String.format("Bang: %s", source));
            }

            @Override
            public void receiveFloat(String source, float x) {
                if (x > 80.0 && x < 1050.0){
                    //Log.d(TAG, "Incoming Pitch: "+x);
                    String noteLiteral = noteFromPitch(x);
                    float targetPitch = (float) frequencyFromNoteNumber(midiFromPitch(x));
                    float diff = targetPitch - x;
                    pitchLabelTextView.setText(String.format("Note: %s", noteLiteral));

                    if (diff < 0.3 && diff > -0.3) userMessageView.setText(String.format("You are tuned to %s", noteLiteral));
                    else if (diff < 1 && diff > -1){
                        if (diff > 0) userMessageView.setText(String.format("Almost tuned, a little up to %s", noteLiteral));
                        else userMessageView.setText(String.format("Almost tuned, a little down to %s", noteLiteral));
                    }
                    else if (diff > 1) userMessageView.setText(String.format("Too flat! tune up a bit. Tuning %s", noteLiteral));
                    else if (diff < -1) userMessageView.setText(String.format("Too sharp! tune down a bit. Tuning %s", noteLiteral));

                    Log.d(TAG, "Note: "+ noteFromPitch(x)+ ", Current Pitch: " + x + "Note num: " + midiFromPitch(x) + "Note's Pitch: " + frequencyFromNoteNumber(midiFromPitch(x)));
                    //pitchView.setCurrentPitch(x);
                    pitchView.setCurrentPitch(x);
                    pitchView.setCenterPitch(targetPitch);
                    pitchView.invalidate();
                }
            }

            @Override
            public void receiveSymbol(String source, String symbol) {

            }

            @Override
            public void receiveList(String source, Object... args) {

            }

            @Override
            public void receiveMessage(String source, String symbol, Object... args) {

            }
        });
        PdBase.setReceiver(pdUiDispatcher);
    }

    private void loadPatch() throws IOException{
        File dir = getFilesDir();
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.tuner),dir, true);
        File patchFile = new File(dir, "tuner.pd");
        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    private void triggerNote(float n){
        PdBase.sendFloat("midinote", n);
        PdBase.sendBang("trigger");
        pitchView.setCenterPitch(n);
    }

    private void start(){
        if(!pdService.isRunning()){
            Intent intent = new Intent(MainActivity.this,MainActivity.class);
            pdService.startAudio(intent, R.drawable.icon, "OReilly Guitar Tuner", "Return to the tuner");
        }
    }

    private void initSystemServices(){
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber){
                if(pdService==null){
                    return;
                }
                if(state == TelephonyManager.CALL_STATE_IDLE){
                    start();
                }else{
                    pdService.stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    protected  void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(pdConnection);
    }
}
