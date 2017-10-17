package com.example.prabhu.healthis_challenge;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //After the record button is pressed, go to record activity.
    public void changeActivityToRecord(View view) {
        Intent intent = new Intent(this, RecordAudioActivity.class);
        startActivity(intent);
    }
}
