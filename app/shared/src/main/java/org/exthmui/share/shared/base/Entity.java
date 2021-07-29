package org.exthmui.share.shared.base;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.work.Data;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.FileUtils;
import org.exthmui.share.shared.R;
import org.exthmui.share.shared.base.exceptions.EmptyPathException;
import org.exthmui.share.shared.base.exceptions.FailedResolvingUriException;
import org.exthmui.share.shared.base.exceptions.FileNotExistsException;
import org.exthmui.share.shared.base.exceptions.UnknownUriSchemeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Entity {

    private static final String TAG = "Entity";

    public static final String FILE_URI = "FILE_URI";
    public static final String FILE_URIS = "FILE_URIS";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_NAMES = "FILE_NAMES";
    public static final String FILE_PATH = "FILE_PATH";
    public static final String FILE_PATHS = "FILE_PATHS";
    public static final String FILE_TYPE = "FILE_TYPE";
    public static final String FILE_TYPES = "FILE_TYPES";
    public static final String FILE_SIZE = "FILE_SIZE";
    public static final String FILE_SIZES = "FILE_SIZES";

    @NonNull
    private final Uri uri;
    private final boolean initialized;
    @Nullable
    private final String fileName;
    @Nullable
    private String filePath = null;
    private final int fileType;
    private final long fileSize;
    private String MD5;

    public Entity(Context context, @NonNull Uri uri) throws FailedResolvingUriException {
        switch (uri.getScheme()) {
            case SCHEME_FILE:
                final String path = uri.getPath();
                if (path.isEmpty()) {
                    Log.e(TAG, "Failed resolving Uri: Empty path. Uri:" + uri);
                    this.initialized = false;
                    throw new EmptyPathException();
                }

                final File file = new File(path);
                if (!file.exists()) {
                    Log.e(TAG, "Failed resolving Uri: File not exists. Uri:" + uri);
                    this.initialized = false;
                    throw new FileNotExistsException();
                }
                uri = FileProvider.getUriForFile(context, context.getString(R.string.content_uri_authority), file);
                break;
            case SCHEME_CONTENT:
                break;
            default:
                this.initialized = false;
                throw new UnknownUriSchemeException();
        }

        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(
                    uri, null, null, null, null);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed resolving Uri: " + uri, e);
            this.initialized = false;
            throw new FailedResolvingUriException(e);
        }

        if (cursor == null) {
            Log.w(TAG, "Failed resolving Uri: Got a null cursor, ignoring. Uri: " + uri);
            this.initialized = true;
            throw new FailedResolvingUriException();
        }

        cursor.moveToFirst();

        fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        final int sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE);
        fileSize = cursor.isNull(sizeIndex) ? -1 : cursor.getLong(sizeIndex);
        cursor.close();
        this.uri = uri;
        this.fileType = FileUtils.getFileType(fileName).getNumVal();
        this.initialized = true;
    }

    public Entity(Context context, Uri uri, int type) throws FailedResolvingUriException {
        this.fileType = type;

        switch (uri.getScheme()) {
            case SCHEME_FILE:
                final String path = uri.getPath();
                if (path.isEmpty()) {
                    Log.e(TAG, "Failed resolving Uri: Empty path. Uri:" + uri);
                    this.initialized = false;
                    throw new EmptyPathException();
                }

                final File file = new File(path);
                if (!file.exists()) {
                    Log.e(TAG, "Failed resolving Uri: File not exists. Uri:" + uri);
                    this.initialized = false;
                    throw new FileNotExistsException();
                }
                uri = FileProvider.getUriForFile(context, context.getString(R.string.content_uri_authority), file);
                break;
            case SCHEME_CONTENT:
                break;
            default:
                this.initialized = false;
                throw new UnknownUriSchemeException();
        }

        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(
                    uri, null, null, null, null);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed resolving Uri: " + uri, e);
            this.initialized = false;
            throw new FailedResolvingUriException(e);
        }

        if (cursor == null) {
            Log.w(TAG, "Failed resolving Uri: Got a null cursor, ignoring. Uri: " + uri);
            this.initialized = true;
            throw new FailedResolvingUriException();
        }

        cursor.moveToFirst();

        fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        final int sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE);
        fileSize = cursor.isNull(sizeIndex) ? -1 : cursor.getLong(sizeIndex);
        cursor.close();
        this.uri = uri;
        this.initialized = true;
    }

    private Entity(@NonNull Uri uri, @Nullable String fileName, @Nullable String filePath, int fileType, long fileSize) {
        this.uri = uri;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.initialized = true;
    }

    public @NonNull
    InputStream getInputStream(@NonNull Context context) throws FileNotFoundException {
        InputStream inputStream = null;
        FileNotFoundException exception = null;
        if (filePath != null) {
            File file = new File(filePath);
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                exception = e;
            }
        }
        if (inputStream == null) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                exception = e;
            }
        }
        if (inputStream == null) {
            if (exception != null) throw exception;
            else throw new FileNotFoundException();
        }
        return inputStream;
    }

    /**
     * Calculate MD5
     *
     * @param context {@link Context} object
     */
    public void calculateMD5(@NonNull Context context) throws IOException {
        InputStream inputStream = getInputStream(context);
        this.MD5 = FileUtils.getMD5(inputStream);
    }

    @NonNull
    public Uri getUri() {
        return uri;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    @Nullable
    public String getFilePath() {
        return filePath;
    }

    public int getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMD5() {
        return MD5;
    }

    public void setMD5(String MD5) {
        this.MD5 = MD5;
    }

    public static Entity fromData(Data data) {
        return new Entity(Uri.parse(data.getString(FILE_URI)),
                data.getString(FILE_NAME), data.getString(FILE_PATH),
                data.getInt(FILE_TYPE, Constants.FileTypes.UNKNOWN.getNumVal()),
                data.getLong(FILE_SIZE, -1));
    }

    public Data toData() {
        return new Data.Builder()
                .putString(FILE_URI, uri.toString())
                .putString(FILE_NAME, fileName)
                .putString(FILE_PATH, filePath)
                .putInt(FILE_TYPE, fileType)
                .putLong(FILE_SIZE, fileSize)
                .build();
    }
}
