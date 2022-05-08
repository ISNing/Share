package org.exthmui.share.shared.base.mediastore;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;

public class Utils {
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static Bitmap getThumbnail(@NonNull ContentResolver contentResolver, @NonNull Uri mediaUri, @NonNull Size size) throws IOException {
        Bitmap bitmap;
        bitmap = contentResolver.loadThumbnail(mediaUri, size, null);
        return bitmap;
    }

    @SuppressWarnings("deprecated")
    @TargetApi(Build.VERSION_CODES.P)
    public static Bitmap getAudioAlbumArt(@NonNull ContentResolver contentResolver, Uri albumUri) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        Cursor cursor = contentResolver.query(albumUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Album album = new Album(cursor);
            bitmap = BitmapFactory.decodeFile(album.getAlbumArt(), options);
            cursor.close();
        }
        return bitmap;
    }

    @SuppressWarnings("deprecated")
    @TargetApi(Build.VERSION_CODES.P)
    public static Bitmap getImageThumbnail(@NonNull ContentResolver contentResolver, long id, int kind) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        bitmap = MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, kind, options);
        return bitmap;
    }

    @SuppressWarnings("deprecated")
    @TargetApi(Build.VERSION_CODES.P)
    public static Bitmap getVideoThumbnail(@NonNull ContentResolver contentResolver, long id, int kind) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        bitmap = MediaStore.Video.Thumbnails.getThumbnail(contentResolver, id, kind, options);
        return bitmap;
    }
}
