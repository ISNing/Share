package org.exthmui.share.controller;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.util.Size;

import androidx.annotation.RequiresApi;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.FileUtils;
import org.exthmui.share.shared.base.AppInfo;
import org.exthmui.share.shared.base.AudioFile;
import org.exthmui.share.shared.base.File;
import org.exthmui.share.shared.base.ImgFolderBean;
import org.exthmui.share.shared.base.VideoFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileManager {

    private static FileManager mInstance;
    private final Context mContext;
    private static ContentResolver mContentResolver;
    private static final Object mLock = new Object();

    public static FileManager getInstance(Context context){
        if (mInstance == null){
            synchronized (mLock){
                if (mInstance == null){
                    mInstance = new FileManager(context);
                }
            }
        }
        return mInstance;
    }

    public FileManager(Context ctx){
        mContext = ctx;
        mContentResolver = ctx.getContentResolver();
    }

    public List<AudioFile> getAudios() {
        ArrayList<AudioFile> audios = new ArrayList<>();
        try (Cursor c = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER)) {

            while (c.moveToNext()) {
                AudioFile audioFile = new AudioFile(c);
                if (!FileUtils.isExists(audioFile.getFilePath())) {
                    continue;
                }

                audios.add(audioFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return audios;
    }

    public List<VideoFile> getVideos() {

        List<VideoFile> videos = new ArrayList<>();

        try (Cursor c = mContentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER)) {
            while (c.moveToNext()) {
                VideoFile videoFile = new VideoFile(c);
                if (!FileUtils.isExists(videoFile.getFilePath())) {
                    continue;
                }
                videos.add(videoFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    // For Api lower than Q
    @SuppressWarnings("deprecated")
    public Bitmap getVideoThumbnail(int id) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmap = MediaStore.Video.Thumbnails.getThumbnail(mContentResolver, id, MediaStore.Images.Thumbnails.MICRO_KIND, options);
        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public Bitmap getVideoThumbnail(Context context, Uri mediaUri, Size size) throws IOException {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        CancellationSignal cs = new CancellationSignal();
        bitmap = context.getContentResolver().loadThumbnail(mediaUri, size, cs);
        return bitmap;
    }

    public List<File> getFilesByType(int fileType) {
        List<File> files = new ArrayList<>();
        try (Cursor c = mContentResolver.query(MediaStore.Files.getContentUri("external"), null, null, null, null)) {
            while (c.moveToNext()) {
                File file = new File(c);

                if (FileUtils.getFileType(file.getFilePath()).getNumVal() == fileType) {
                    if (!FileUtils.isExists(file.getFilePath())) {
                        continue;
                    }
                    files.add(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public List<ImgFolderBean> getImageFolders() {
        List<ImgFolderBean> folders = new ArrayList<>();
        try (Cursor c = mContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore.Images.Media.MIME_TYPE + "= ?",
                new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED)) {
            List<String> mDirs = new ArrayList<>();// Directories added
            while (c.moveToNext()) {
                String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                java.io.File parentFile = new java.io.File(path).getParentFile();
                if (parentFile == null)
                    continue;

                String dir = parentFile.getAbsolutePath();
                if (mDirs.contains(dir))// If added, then continue
                    continue;

                mDirs.add(dir);
                ImgFolderBean folderBean = new ImgFolderBean();
                folderBean.setDir(dir);
                folderBean.setFistImgPath(path);
                if (parentFile.list() == null)
                    continue;
                int count = Objects.requireNonNull(parentFile.list((dir1, filename) -> filename.endsWith(".jpeg") || filename.endsWith(".jpg") || filename.endsWith(".png"))).length;

                folderBean.setCount(count);
                folders.add(folderBean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return folders;
    }

    public List<String> getImgListByDir(String dir) {
        ArrayList<String> imgPaths = new ArrayList<>();
        java.io.File directory = new java.io.File(dir);
        if (!directory.exists()) {
            return imgPaths;
        }
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                String path = file.getAbsolutePath();
                if (FileUtils.getFileType(path) == Constants.FileType.IMAGE) {
                    imgPaths.add(path);
                }
            }
        }
        return imgPaths;
    }

    @SuppressWarnings("deprecated")
    public List<AppInfo> getAppInfos(Context ctx) {

        ArrayList<AppInfo> appInfos = new ArrayList<>();
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);

        for (PackageInfo packageInfo : installedPackages) {

            AppInfo appInfo = new AppInfo();

            appInfo.setApplicationInfo(packageInfo.applicationInfo);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                appInfo.setVersionCode(packageInfo.getLongVersionCode());
            else appInfo.setVersionCode(packageInfo.versionCode);

            Drawable drawable = packageInfo.applicationInfo.loadIcon(packageManager);
            appInfo.setIcon(drawable);

            String apkName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            appInfo.setApkName(apkName);

            String packageName = packageInfo.packageName;
            appInfo.setApkPackageName(packageName);

            String sourceDir = packageInfo.applicationInfo.sourceDir;
            java.io.File file = new java.io.File(sourceDir);

            long size = file.length();
            appInfo.setApkSize(size);

            int flags = packageInfo.applicationInfo.flags;

            appInfo.setIsUserApp((flags & ApplicationInfo.FLAG_SYSTEM) == 0);

            appInfo.setIsRom((flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == 0);
            appInfos.add(appInfo);
        }
        return appInfos;
    }

}