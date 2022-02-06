package org.exthmui.share;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.exthmui.share.databinding.ActivityMainBinding;
import org.exthmui.share.databinding.SettingsActivityBinding;
import org.exthmui.share.shared.base.AudioFile;
import org.exthmui.share.controller.FileManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}