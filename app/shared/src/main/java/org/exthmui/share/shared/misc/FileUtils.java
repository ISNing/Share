package org.exthmui.share.shared.misc;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@SuppressWarnings("SpellCheckingInspection")
public abstract class FileUtils {
    // construct a with an approximation of the capacity
    private static final HashMap<String, Constants.FileType> MIME_IDS = new HashMap<>(1 + (int) (114 / 0.75));

    private static void put(String mimeType, Constants.FileType type) {
        if (MIME_IDS.put(mimeType, type) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    private static void putKeys(Constants.FileType fileType, @NonNull String... mimeTypes) {
        for (String type : mimeTypes) {
            put(type, fileType);
        }
    }

    static {
        putKeys(Constants.FileType.APK,
                "application/vnd.android.package-archive"
        );
        putKeys(Constants.FileType.AUDIO,
                "application/ogg",
                "application/x-flac"
        );
        putKeys(Constants.FileType.CERTIFICATE,
                "application/pgp-keys",
                "application/pgp-signature",
                "application/x-pkcs12",
                "application/x-pkcs7-certreqresp",
                "application/x-pkcs7-crl",
                "application/x-x509-ca-cert",
                "application/x-x509-user-cert",
                "application/x-pkcs7-certificates",
                "application/x-pkcs7-mime",
                "application/x-pkcs7-signature"
        );
        putKeys(Constants.FileType.CODE,
                "application/rdf+xml",
                "application/rss+xml",
                "application/x-object",
                "application/xhtml+xml",
                "text/css",
                "text/html",
                "text/xml",
                "text/x-c++hdr",
                "text/x-c++src",
                "text/x-chdr",
                "text/x-csrc",
                "text/x-dsrc",
                "text/x-csh",
                "text/x-haskell",
                "text/x-java",
                "text/x-literate-haskell",
                "text/x-pascal",
                "text/x-tcl",
                "text/x-tex",
                "application/x-latex",
                "application/x-texinfo",
                "application/atom+xml",
                "application/ecmascript",
                "application/json",
                "application/javascript",
                "application/xml",
                "text/javascript",
                "application/x-javascript"
        );
        putKeys(Constants.FileType.COMPRESSED,
                "application/mac-binhex40",
                "application/rar",
                "application/zip",
                "application/java-archive",
                "application/x-apple-diskimage",
                "application/x-debian-package",
                "application/x-gtar",
                "application/x-iso9660-image",
                "application/x-lha",
                "application/x-lzh",
                "application/x-lzx",
                "application/x-stuffit",
                "application/x-tar",
                "application/x-webarchive",
                "application/x-webarchive-xml",
                "application/x-gzip",
                "application/x-7z-compressed",
                "application/x-deb",
                "application/x-rar-compressed"
        );
        putKeys(Constants.FileType.CONTACT,
                "text/x-vcard",
                "text/vcard"
        );
        putKeys(Constants.FileType.EVENTS,
                "text/calendar",
                "text/x-vcalendar"
        );
        putKeys(Constants.FileType.FONT,
                "application/x-font",
                "application/font-woff",
                "application/x-font-woff",
                "application/x-font-ttf"
        );
        putKeys(Constants.FileType.IMAGE,
                "application/vnd.oasis.opendocument.graphics",
                "application/vnd.oasis.opendocument.graphics-template",
                "application/vnd.oasis.opendocument.image",
                "application/vnd.stardivision.draw",
                "application/vnd.sun.xml.draw",
                "application/vnd.sun.xml.draw.template",
                "image/jpeg",
                "image/png"
        );
        putKeys(Constants.FileType.PDF,
                "application/pdf"
        );
        putKeys(Constants.FileType.PRESENTATION,
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.presentationml.template",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                "application/vnd.stardivision.impress",
                "application/vnd.sun.xml.impress",
                "application/vnd.sun.xml.impress.template",
                "application/x-kpresenter",
                "application/vnd.oasis.opendocument.presentation"
        );
        putKeys(Constants.FileType.SPREADSHEETS,
                "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.oasis.opendocument.spreadsheet-template",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                "application/vnd.stardivision.calc",
                "application/vnd.sun.xml.calc",
                "application/vnd.sun.xml.calc.template",
                "application/x-kspread",
                "text/comma-separated-values"
        );
        putKeys(Constants.FileType.DOCUMENTS,
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.oasis.opendocument.text-master",
                "application/vnd.oasis.opendocument.text-template",
                "application/vnd.oasis.opendocument.text-web",
                "application/vnd.stardivision.writer",
                "application/vnd.stardivision.writer-global",
                "application/vnd.sun.xml.writer",
                "application/vnd.sun.xml.writer.global",
                "application/vnd.sun.xml.writer.template",
                "application/x-abiword",
                "application/x-kword",
                "text/markdown"
        );
        putKeys(Constants.FileType.TEXT,
                "text/plain"
        );
        putKeys(Constants.FileType.VIDEO,
                "application/x-quicktimeplayer",
                "application/x-shockwave-flash"
        );
        putKeys(Constants.FileType.ENCRYPTED,
                "application/octet-stream"
        );
    }

    /**
     * Judge whether the file exists
     * @param path The path of file
     * @return Whether the file exists
     */
    public static boolean isExists(@NonNull String path) {
        File file = new File(path);
        return file.exists();
    }

    @NonNull
    public static Constants.FileType getFileType(@NonNull String path) {
        String mimeType = MimeTypes.getMimeType(path);
        return getFileTypeByMime(mimeType);
    }

    @NonNull
    public static Constants.FileType getFileTypeByMime(@Nullable String mimeType) {
        if (mimeType == null) return Constants.FileType.UNKNOWN;

        Constants.FileType type = MIME_IDS.get(mimeType);
        if (type != null) return type;
        else {
            if (checkType(mimeType, "text")) return Constants.FileType.TEXT;
            else if (checkType(mimeType, "image")) return Constants.FileType.IMAGE;
            else if (checkType(mimeType, "video")) return Constants.FileType.VIDEO;
            else if (checkType(mimeType, "audio")) return Constants.FileType.AUDIO;
            else if (checkType(mimeType, "crypt")) return Constants.FileType.ENCRYPTED;
            else return Constants.FileType.UNKNOWN;
        }
    }

    private static boolean checkType(@Nullable String mime, @NonNull String check) {
        return mime != null && mime.contains("/") && check.equals(mime.substring(0, mime.indexOf("/")));
    }

    @Nullable
    public static String getMD5(@NonNull InputStream inputStream) throws IOException {
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            inputStream.close();
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    public static long getSpaceAvailable(@NonNull Context context, @NonNull DocumentFile documentFile) throws ErrnoException, FileNotFoundException {
        ParcelFileDescriptor pfd = context.getApplicationContext().getContentResolver().openFileDescriptor(documentFile.getUri(), "r");
        assert pfd != null;
        StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
        return stats.f_bavail * stats.f_bsize;
    }
}