package org.exthmui.share;

import android.content.ClipData;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.exthmui.share.shared.FileUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.exceptions.FailedResolvingUriException;

import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShareBottomSheetFragment shareFragment = new ShareBottomSheetFragment();
        shareFragment.setEntities(getEntities());
        shareFragment.show(getSupportFragmentManager(), shareFragment.getTag());
    }

    private List<Entity> getEntities() {
        final List<Entity> entities = new ArrayList<>();
        final String mimeType = getIntent().getType();
        final ClipData clipData = getIntent().getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Entity entity = null;
                try {
                    entity = new Entity(this, clipData.getItemAt(i).getUri());
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
            Entity entity = null;
            try {
                entity = new Entity(this, getIntent().getData(), FileUtils.getFileTypeByMime(mimeType).getNumVal());
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
}