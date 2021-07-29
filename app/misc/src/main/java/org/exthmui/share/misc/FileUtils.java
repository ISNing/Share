package org.exthmui.share.misc;

import java.io.File;
import java.util.HashMap;

public class FileUtils {
    public static final int NOT_KNOWN = -1;
    public static final int APK = 0, AUDIO = 1, CERTIFICATE = 2, CODE = 3, COMPRESSED = 4,
            CONTACT = 5, EVENTS = 6, FONT = 7, IMAGE = 8, PDF = 9, PRESENTATION = 10,
            SPREADSHEETS = 11, DOCUMENTS = 12, TEXT = 13, VIDEO = 14, ENCRYPTED = 15, GIF = 16;

    // construct a with an approximation of the capacity
    private static HashMap<String, Integer> sMimeIds = new HashMap<>(1 + (int)(114 / 0.75));
    private static void put(String mimeType, int resId) {
        if (sMimeIds.put(mimeType, resId) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    private static void putKeys(int resId, String... mimeTypes) {
        for (String type : mimeTypes) {
            put(type, resId);
        }
    }

    static {
        putKeys(APK,
                "application/vnd.android.package-archive"
        );
        putKeys(AUDIO,
                "application/ogg",
                "application/x-flac"
        );
        putKeys(CERTIFICATE,
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
        putKeys(CODE,
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
        putKeys(COMPRESSED,
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
        putKeys(CONTACT,
                "text/x-vcard",
                "text/vcard"
        );
        putKeys(EVENTS,
                "text/calendar",
                "text/x-vcalendar"
        );
        putKeys(FONT,
                "application/x-font",
                "application/font-woff",
                "application/x-font-woff",
                "application/x-font-ttf"
        );
        putKeys(IMAGE,
                "application/vnd.oasis.opendocument.graphics",
                "application/vnd.oasis.opendocument.graphics-template",
                "application/vnd.oasis.opendocument.image",
                "application/vnd.stardivision.draw",
                "application/vnd.sun.xml.draw",
                "application/vnd.sun.xml.draw.template",
                "image/jpeg",
                "image/png"
        );
        putKeys(PDF,
                "application/pdf"
        );
        putKeys(PRESENTATION,
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
        putKeys(SPREADSHEETS,
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
        putKeys(DOCUMENTS,
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
        putKeys(TEXT,
                "text/plain"
        );
        putKeys(VIDEO,
                "application/x-quicktimeplayer",
                "application/x-shockwave-flash"
        );
        putKeys(ENCRYPTED,
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

    public static int getFileType(String path, boolean isDirectory) {
        String mimeType = MimeTypes.getMimeType(path, isDirectory);
        if(mimeType == null) return NOT_KNOWN;

        Integer type = sMimeIds.get(mimeType);
        if(type != null) return type;
        else {
            if(checkType(mimeType, "text")) return TEXT;
            else if (checkType(mimeType, "image")) return IMAGE;
            else if(checkType(mimeType, "video")) return VIDEO;
            else if(checkType(mimeType, "audio")) return AUDIO;
            else if (checkType(mimeType, "crypt")) return ENCRYPTED;
            else return NOT_KNOWN;
        }
    }

    private static boolean checkType(String mime, String check) {
        return mime != null && mime.contains("/") && check.equals(mime.substring(0, mime.indexOf("/")));
    }
}