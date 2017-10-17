package com.example.prabhu.healthis_challenge;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.example.prabhu.healthis_challenge.WavFile.WavFile;
import com.example.prabhu.healthis_challenge.WavFile.WavFileException;

import weka.classifiers.Classifier;
//import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;

public class RecordAudioActivity extends AppCompatActivity {

    private static final int REQUEST_ALL_PERMISSION = 200;

    private boolean permissionToAllAccepted = false;

    //Permissions - Record Audio and Write into external storage.
    private String [] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    public String audioFile, store, arffFile;
    //private MediaRecorder mRecorder = null;
    private Handler mHandler = new Handler();
    WavRecorder wavRecorder = null;


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_ALL_PERMISSION:
                permissionToAllAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if (!permissionToAllAccepted ) finish();
    }


    //Stop recording in a separate thread.
    private void recordAudioHandler() {
        TextView textView = (TextView) findViewById(R.id.textView);
        wavRecorder.stopRecording();
        textView.setText("Recorded");
    }

    //Prediction in a separate thread.
    private void predict() throws Exception {
        arffFile = store + "/test.arff";
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Predicting");
        try {
            data_preprocess(audioFile, arffFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WavFileException e) {
            e.printStackTrace();
        }

        Thread.currentThread().sleep(1000);

        //Load SVM from Assets
        LibSVM cls_from_file = (LibSVM) weka.core.SerializationHelper.read(this.getApplicationContext().getAssets().open("libsvm_speech.model"));

        //Convert into
        Instances toPredict = new Instances(new BufferedReader(new FileReader(arffFile)));
        if (toPredict.classIndex() == -1)
            toPredict.setClassIndex(toPredict.numAttributes() - 1);

        //Prediction
        double output = cls_from_file.classifyInstance(toPredict.firstInstance());

        System.out.println(output);

        mHandler.post(new Runnable() {
            public void run() {
                try {
                    playVideo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        TextView textView_class = (TextView) findViewById(R.id.textView_class);
        textView_class.setText("Classification Result:");
        if(output==0.0)
            textView.setText("Noise Activity");
        else
            textView.setText("Speech Activity");
    }

    //Play in media player of what was recorded.
    private void playVideo() throws IOException {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(audioFile);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);
        //Getting permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSION);


        boolean check = true;
        while(check){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ){
                check = false;
                store = getCacheDir().getAbsolutePath();
                audioFile = store + "/audiotemp.wav";

                //Getting the audio file in Wav format
                wavRecorder = new WavRecorder(audioFile);
                //Start recording
                wavRecorder.startRecording();
                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setText("Recording...");
            }
        }

        //Stop recording in 2 sec
        mHandler.postDelayed(new Runnable() {
                public void run() {
                    recordAudioHandler();
                }
        }, 2000);

        //Preprocess and predict.
        mHandler.postDelayed(new Runnable() {
            public void run() {
                try {
                    predict();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 3000);

    }

    private static final long MAX_FRAMES = 300;

    // Helper to convert WAV to WEKA format (ARFF)
    public void preprocess(String WavInput, int tag, BufferedWriter bw) throws IOException, WavFileException
    {
        File file = new File(WavInput);
        TextView textView = (TextView) findViewById(R.id.textView);

        if (!file.isFile()){
            textView.setText("Retry, audio not stored.");
        }

        WavFile wavFile = WavFile.openWavFile(file);
        //textView.setText("Number of channels:"+wavFile.getNumChannels());
        int numChannels = wavFile.getNumChannels();
        long numFrames = wavFile.getNumFrames();
        double[] buffer = new double[100 * numChannels];
        int framesRead;
        int count=0;
        String out = "";
        double avg = 0;
        List<Double> double_list = new ArrayList<Double>();
        do
        {
            framesRead = wavFile.readFrames(buffer, 100);

            for (int s=0 ; s<framesRead * numChannels ; s++)
            {
                avg = avg + buffer[s]*100;
            }

            double_list.add(avg/100);
            count++;
        }
        while (framesRead != 0);

        double max = Collections.max(double_list);

        out = Double.toString(double_list.get(0)/max);

        for (int i=1; i<double_list.size();i++)
        {
            out = out + "," + Double.toString(double_list.get(i)/max);
        }

        for (int i=count; i<MAX_FRAMES; i++){
            out = out + "," + "0.0";
        }

        out = out + "," + tag + "\n";
        bw.write(out);
        wavFile.close();

    }

    // Convert WAV to WEKA format (ARFF)
    public void data_preprocess(String WavInput, String evalarff) throws IOException, WavFileException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(evalarff));

        String out;
        out = "@RELATION audioClassification\n \n";

        for(int i=0; i<MAX_FRAMES; i++){
            out = out + "@ATTRIBUTE attr"+i+" NUMERIC\n";
        }

        out = out + "@ATTRIBUTE class {0,1} \n\n@DATA\n";

        bw.write(out);
        preprocess(WavInput, 0, bw);
        bw.close();
    }


}
