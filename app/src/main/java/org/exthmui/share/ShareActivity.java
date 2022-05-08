package org.exthmui.share;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.exceptions.FailedResolvingUriException;

import java.util.ArrayList;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            ShareBottomSheetFragment shareFragment = new ShareBottomSheetFragment();
            shareFragment.show(getSupportFragmentManager(), shareFragment.getTag());
            shareFragment.setEntities(getEntities());
        }
        grantUriPermissions();
    }

    @NonNull
    private ArrayList<Entity> getEntities() {
        final ArrayList<Entity> entities = new ArrayList<>();
        // From ClipData
        final ClipData clipData = getIntent().getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Entity entity = null;
                try {
                    entity = new Entity(getApplicationContext(), clipData.getItemAt(i).getUri());
                } catch (FailedResolvingUriException e) {
                    e.printStackTrace();
                }
                if (entity != null) {
                    if (entity.isInitialized()) {
                        entities.add(entity);
                    }
                }
            }
        } else {
            // From Intent Data
            Entity entity = null;
            try {
                entity = new Entity(getApplicationContext(), getIntent().getData());
            } catch (FailedResolvingUriException e) {
                e.printStackTrace();
            }
            if (entity != null) {
                if (entity.isInitialized()) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }

    private void grantUriPermissions() {
        final ClipData clipData = getIntent().getClipData();
        if (clipData != null) {
            grantUriPermissions(clipData);
        } else {
            grantUriPermissions(getIntent().getData());
        }
    }

    private void grantUriPermissions(Uri data) {
        Intent intent = new Intent(this, DiscoverService.class)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setData(data);
        startService(intent);
    }

    private void grantUriPermissions(ClipData data) {
        Intent intent = new Intent(this, DiscoverService.class)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(data);
        startService(intent);
    }
}