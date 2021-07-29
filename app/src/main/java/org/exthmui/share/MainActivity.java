package org.exthmui.share;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.exthmui.share.beans.AudioFile;
import org.exthmui.share.controller.FileManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStart = findViewById(R.id.button);
        FileManager fileManager = new FileManager(getApplicationContext());

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<AudioFile> audios = fileManager.getAudios();
            }
        });
    }
}