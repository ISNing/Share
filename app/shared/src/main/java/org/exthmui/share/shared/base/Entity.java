package org.exthmui.share.shared.base;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;
import static androidx.core.content.FileProvider.getUriForFile;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.exthmui.share.shared.FileUtils;
import org.exthmui.share.shared.R;
import org.exthmui.share.shared.base.exceptions.EmptyPathException;
import org.exthmui.share.shared.base.exceptions.FailedResolvingUriException;
import org.exthmui.share.shared.base.exceptions.FileNotExistsException;
import org.exthmui.share.shared.base.exceptions.UnknownUriSchemeException;

public class Entity implements Parcelable {

    public static final String TAG = "Entity";

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
    private final Uri uri; // Content uri only
    @Nullable
    private final String fileName;
    @Nullable
    private String filePath = null;
    private int fileType;
    private final long fileSize;
    private String MD5;
    private final boolean initialized;

    private static final Creator<Entity> CREATOR = new Creator<>() {
        @Override
        public Entity createFromParcel(Parcel source) {
            return new Entity(source);
        }

        @Override
        public Entity[] newArray(int size) {
            return new Entity[size];
        }
    };

    /**
     * Instantiate an Entity
     *
     * @param context Context
     * @param uri     Content uri or File uri required
     * @throws FailedResolvingUriException Failed resolving uri
     */
    public Entity(Context context, @NonNull Uri uri) throws FailedResolvingUriException {
        switch (uri.getScheme()) {
            case SCHEME_FILE:
                try {
                    uri = generateContentUri(context, uri);
                } catch (FailedResolvingUriException e) {
                    initialized = false;
                    throw e;
                }
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

    /**
     * Instantiate an Entity
     *
     * @param context Context
     * @param uri     Content uri or File uri required
     * @param type    File type
     * @throws FailedResolvingUriException Failed resolving uri
     */
    public Entity(Context context, @NonNull Uri uri, int type) throws FailedResolvingUriException {
        this(context, uri);
        this.fileType = type;
    }

    private Entity(@NonNull Uri uri, @Nullable String fileName, @Nullable String filePath,
        int fileType, long fileSize, String MD5) {
        this.uri = uri;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.MD5 = MD5;
        this.initialized = true;
    }

    private Entity(@NonNull Parcel source) {
        this.uri = source.readParcelable(Uri.class.getClassLoader());
        this.fileName = source.readString();
        this.filePath = source.readString();
        this.fileType = source.readInt();
        this.fileSize = source.readLong();
        this.MD5 = source.readString();
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            this.initialized = source.readBoolean();
        } else {
            this.initialized = source.readInt() != 0;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(uri, 0);
        parcel.writeString(fileName);
        parcel.writeString(filePath);
        parcel.writeInt(fileType);
        parcel.writeLong(fileSize);
        parcel.writeString(MD5);
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            parcel.writeBoolean(initialized);
        } else {
            parcel.writeInt(initialized ? 1 : 0);
        }
    }

    private Uri generateContentUri(Context context, Uri uri)
        throws EmptyPathException, FileNotExistsException {
        final String path = uri.getPath();
        if (path.isEmpty()) {
            Log.e(TAG, "Failed resolving Uri: Empty path. Uri:" + uri);
            throw new EmptyPathException();
        }

        final File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "Failed resolving Uri: File not exists. Uri:" + uri);
            throw new FileNotExistsException();
        }

        return getUriForFile(context, context.getString(R.string.content_uri_authority), file);
    }

    @Nullable
    public InputStream getInputStream(@NonNull Context context) throws FileNotFoundException {
        InputStream inputStream;
        inputStream = context.getContentResolver().openInputStream(uri);
        return inputStream;
    }

    /**
     * Calculate MD5
     *
     * @param context {@link Context} object
     */
    public void calculateMD5(@NonNull Context context) throws IOException {
        InputStream inputStream = getInputStream(context);
        if (inputStream == null) {
            Log.e(TAG, String.format("Failed to calculate MD5 for %s due to got null InoutStream", fileName));
            return;
        }
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
}
