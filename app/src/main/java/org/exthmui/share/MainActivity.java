package org.exthmui.share;

import android.app.PendingIntent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.exthmui.share.databinding.ActivityMainBinding;
import org.exthmui.share.shared.ReceiverUtils;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ReceiverUtils.buildDialogPendingIntent(MainActivity.this, "sfs", "dfs", "sfddsaf", "fsfdef.file", 219201, 100).send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}