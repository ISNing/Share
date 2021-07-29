package org.exthmui.share.shared;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class FileUtils {
    // construct a with an approximation of the capacity
    private static final HashMap<String, Constants.FileTypes> sMimeIds = new HashMap<>(1 + (int)(114 / 0.75));
    private static void put(String mimeType, Constants.FileTypes type) {
        if (sMimeIds.put(mimeType, type) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    private static void putKeys(Constants.FileTypes fileType, String... mimeTypes) {
        for (String type : mimeTypes) {
            put(type, fileType);
        }
    }

    static {
        putKeys(Constants.FileTypes.APK,
                "application/vnd.android.package-archive"
        );
        putKeys(Constants.FileTypes.AUDIO,
                "application/ogg",
                "application/x-flac"
        );
        putKeys(Constants.FileTypes.CERTIFICATE,
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
        putKeys(Constants.FileTypes.CODE,
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
        putKeys(Constants.FileTypes.COMPRESSED,
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
        putKeys(Constants.FileTypes.CONTACT,
                "text/x-vcard",
                "text/vcard"
        );
        putKeys(Constants.FileTypes.EVENTS,
                "text/calendar",
                "text/x-vcalendar"
        );
        putKeys(Constants.FileTypes.FONT,
                "application/x-font",
                "application/font-woff",
                "application/x-font-woff",
                "application/x-font-ttf"
        );
        putKeys(Constants.FileTypes.IMAGE,
                "application/vnd.oasis.opendocument.graphics",
                "application/vnd.oasis.opendocument.graphics-template",
                "application/vnd.oasis.opendocument.image",
                "application/vnd.stardivision.draw",
                "application/vnd.sun.xml.draw",
                "application/vnd.sun.xml.draw.template",
                "image/jpeg",
                "image/png"
        );
        putKeys(Constants.FileTypes.PDF,
                "application/pdf"
        );
        putKeys(Constants.FileTypes.PRESENTATION,
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
        putKeys(Constants.FileTypes.SPREADSHEETS,
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
        putKeys(Constants.FileTypes.DOCUMENTS,
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
        putKeys(Constants.FileTypes.TEXT,
                "text/plain"
        );
        putKeys(Constants.FileTypes.VIDEO,
                "application/x-quicktimeplayer",
                "application/x-shockwave-flash"
        );
        putKeys(Constants.FileTypes.ENCRYPTED,
                "application/octet-stream"
        );
    }

    /**
     * 判断文件是否存在
     * @param path 文件的路径
     * @return
     */
    public static boolean isExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static Constants.FileTypes getFileType(String path) {
        String mimeType = MimeTypes.getMimeType(path);
        return getFileTypeByMime(mimeType);
    }

    public static Constants.FileTypes getFileTypeByMime(String mimeType) {
        if(mimeType == null) return Constants.FileTypes.UNKNOWN;

        Constants.FileTypes type = sMimeIds.get(mimeType);
        if(type != null) return type;
        else {
            if(checkType(mimeType, "text")) return Constants.FileTypes.TEXT;
            else if (checkType(mimeType, "image")) return Constants.FileTypes.IMAGE;
            else if(checkType(mimeType, "video")) return Constants.FileTypes.VIDEO;
            else if(checkType(mimeType, "audio")) return Constants.FileTypes.AUDIO;
            else if (checkType(mimeType, "crypt")) return Constants.FileTypes.ENCRYPTED;
            else return Constants.FileTypes.UNKNOWN;
        }
    }

    private static boolean checkType(String mime, String check) {
        return mime != null && mime.contains("/") && check.equals(mime.substring(0, mime.indexOf("/")));
    }

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