package org.exthmui.share.controller;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.file.AppInfo;
import org.exthmui.share.shared.base.file.AudioFile;
import org.exthmui.share.shared.base.file.File;
import org.exthmui.share.shared.base.file.ImageFile;
import org.exthmui.share.shared.base.file.ImageFolder;
import org.exthmui.share.shared.base.file.VideoFile;
import org.exthmui.share.shared.base.mediastore.Audio;
import org.exthmui.share.shared.base.mediastore.Image;
import org.exthmui.share.shared.base.mediastore.Utils;
import org.exthmui.share.shared.base.mediastore.Video;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileManager {
    private final ContentResolver mContentResolver;
    private final Size mThumbSize;
    private final int mThumbKind;

    public FileManager(@NonNull Context ctx, @Nullable Size thumbSize, int mThumbKind){
        mContentResolver = ctx.getContentResolver();
        mThumbSize = thumbSize;
        this.mThumbKind = mThumbKind;
    }

    @NonNull
    public List<AudioFile> getAudios() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        ArrayList<AudioFile> audios = new ArrayList<>();

        try (Cursor c = mContentResolver.query(collection, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER)) {

            while (c.moveToNext()) {
                Audio audio = new Audio(c);
                AudioFile audioFile = new AudioFile(audio);

                Bitmap thumbnail;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    thumbnail = Utils.getThumbnail(mContentResolver, audio.getUri(), mThumbSize);
                else thumbnail = Utils.getAudioAlbumArt(mContentResolver, audio.getAlbumUri());
                audioFile.setThumbnail(thumbnail);

                audios.add(audioFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return audios;
    }

    @NonNull
    public List<VideoFile> getVideos() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        List<VideoFile> videos = new ArrayList<>();

        try (Cursor c = mContentResolver.query(collection, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER)) {
            while (c.moveToNext()) {
                Video video = new Video(c);
                VideoFile videoFile = new VideoFile(video);

                Bitmap thumbnail;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    thumbnail = Utils.getThumbnail(mContentResolver, video.getUri(), mThumbSize);
                else thumbnail = Utils.getVideoThumbnail(mContentResolver, video.getId(), mThumbKind);
                videoFile.setThumbnail(thumbnail);

                videos.add(videoFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    @NonNull
    public List<ImageFile> getImages() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        List<ImageFile> images = new ArrayList<>();

        try (Cursor c = mContentResolver.query(collection, null, null, null, MediaStore.Images.Media.DEFAULT_SORT_ORDER)) {
            while (c.moveToNext()) {
                Image image = new Image(c);
                ImageFile imageFile = new ImageFile(image);

                Bitmap thumbnail;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    thumbnail = Utils.getThumbnail(mContentResolver, image.getUri(), mThumbSize);
                else thumbnail = Utils.getImageThumbnail(mContentResolver, image.getId(), mThumbKind);
                imageFile.setThumbnail(thumbnail);

                images.add(imageFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return images;
    }

    @NonNull
    public List<File> getFilesByType(int fileType) {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Files.getContentUri("external");
        }

        List<File> files = new ArrayList<>();

        try (Cursor c = mContentResolver.query(collection, null, null, null, null)) {
            while (c.moveToNext()) {
                org.exthmui.share.shared.base.mediastore.File fileM = new org.exthmui.share.shared.base.mediastore.File(c);
                File file = new File(fileM);

                if (FileUtils.getFileType(file.getName() == null ? "" : file.getName()).getNumVal() == fileType) {
                    files.add(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    @NonNull
    public List<ImageFolder> getImageFolders() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        List<ImageFolder> folders = new ArrayList<>();

        try (Cursor c = mContentResolver.query(collection, null,
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
                ImageFolder folderBean = new ImageFolder();
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

    @NonNull
    public List<String> getImageListByDir(@NonNull String dir) {
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

    @NonNull
    public List<AppInfo> getAppInfos(@NonNull Context ctx) {

        ArrayList<AppInfo> appInfos = new ArrayList<>();
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);

        for (PackageInfo packageInfo : installedPackages) {
            AppInfo appInfo = new AppInfo(packageInfo, packageManager);
            appInfos.add(appInfo);
        }

        return appInfos;
    }

}