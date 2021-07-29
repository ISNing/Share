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
import android.provider.MediaStore;

import org.exthmui.share.beans.*;
import org.exthmui.share.beans.trash.Audio;
import org.exthmui.share.beans.trash.Video;
import org.exthmui.share.misc.FileUtils;

import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static FileManager mInstance;
    private Context mContext;
    private static ContentResolver mContentResolver;
    private static Object mLock = new Object();

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

    /**
     * 获取本机音乐列表
     * @return
     */
    public List<AudioFile> getAudios() {
        ArrayList<AudioFile> audios = new ArrayList<>();
        Cursor c = null;
        try {
            c = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

            while (c.moveToNext()) {
                AudioFile audioFile = new AudioFile(c);
                if (!FileUtils.isExists(audioFile.getFilePath())) {
                    continue;
                }

                audios.add(audioFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return audios;
    }

    /**
     * 获取本机视频列表
     * @return
     */
    public List<VideoFile> getVideos() {

        List<VideoFile> videos = new ArrayList<>();

        Cursor c = null;
        try {
            c = mContentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
            while (c.moveToNext()) {
                VideoFile videoFile = new VideoFile(c);
                if (!FileUtils.isExists(videoFile.getFilePath())) {
                    continue;
                }
                videos.add(videoFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return videos;
    }

    // 获取视频缩略图
    public Bitmap getVideoThumbnail(int id) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmap = MediaStore.Video.Thumbnails.getThumbnail(mContentResolver, id, MediaStore.Images.Thumbnails.MICRO_KIND, options);
        return bitmap;
    }

    /**
     * 通过文件类型得到相应文件的集合
     **/
    public List<FileBean> getFilesByType(int fileType) {
        List<FileBean> files = new ArrayList<FileBean>();
        // 扫描files文件库
        Cursor c = null;
        try {
            c = mContentResolver.query(MediaStore.Files.getContentUri("external"), null, null, null, null);

            while (c.moveToNext()) {
                FileBean fileBean = new FileBean(c);

                if (FileUtils.getFileType(fileBean.getFilePath(), false) == fileType) {
                    if (!FileUtils.isExists(fileBean.getFilePath())) {
                        continue;
                    }
                    files.add(fileBean);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return files;
    }

    /**
     * 得到图片文件夹集合
     */
    public List<ImgFolderBean> getImageFolders() {
        List<ImgFolderBean> folders = new ArrayList<>();
        // 扫描图片
        Cursor c = null;
        try {
            c = mContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore.Images.Media.MIME_TYPE + "= ?",
                    new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);
            List<String> mDirs = new ArrayList<String>();//用于保存已经添加过的文件夹目录
            while (c.moveToNext()) {
                String path = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));// 路径
                java.io.File parentFile = new java.io.File(path).getParentFile();
                if (parentFile == null)
                    continue;

                String dir = parentFile.getAbsolutePath();
                if (mDirs.contains(dir))//如果已经添加过
                    continue;

                mDirs.add(dir);//添加到保存目录的集合中
                ImgFolderBean folderBean = new ImgFolderBean();
                folderBean.setDir(dir);
                folderBean.setFistImgPath(path);
                if (parentFile.list() == null)
                    continue;
                int count = parentFile.list(new FilenameFilter() {
                    @Override
                    public boolean accept(java.io.File dir, String filename) {
                        if (filename.endsWith(".jpeg") || filename.endsWith(".jpg") || filename.endsWith(".png")) {
                            return true;
                        }
                        return false;
                    }
                }).length;

                folderBean.setCount(count);
                folders.add(folderBean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return folders;
    }

    /**
     * 通过图片文件夹的路径获取该目录下的图片
     */
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
                if (FileUtils.getFileType(path, false) == FileUtils.IMAGE) {
                    imgPaths.add(path);
                }
            }
        }
        return imgPaths;
    }

    /**
     * 获取已安装apk的列表
     */
    public List<AppInfo> getAppInfos(Context ctx) {

        ArrayList<AppInfo> appInfos = new ArrayList<AppInfo>();
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);

        //遍历每个安装包，获取对应的信息
        for (PackageInfo packageInfo : installedPackages) {

            AppInfo appInfo = new AppInfo();

            appInfo.setApplicationInfo(packageInfo.applicationInfo);
            appInfo.setVersionCode(packageInfo.versionCode);

            //得到icon
            Drawable drawable = packageInfo.applicationInfo.loadIcon(packageManager);
            appInfo.setIcon(drawable);

            //得到程序的名字
            String apkName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            appInfo.setApkName(apkName);

            //得到程序的包名
            String packageName = packageInfo.packageName;
            appInfo.setApkPackageName(packageName);

            //得到程序的资源文件夹
            String sourceDir = packageInfo.applicationInfo.sourceDir;
            java.io.File file = new java.io.File(sourceDir);
            //得到apk的大小
            long size = file.length();
            appInfo.setApkSize(size);

            System.out.println("---------------------------");
            System.out.println("程序的名字:" + apkName);
            System.out.println("程序的包名:" + packageName);
            System.out.println("程序的大小:" + size);


            //获取到安装应用程序的标记
            int flags = packageInfo.applicationInfo.flags;

            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                //表示系统app
                appInfo.setIsUserApp(false);
            } else {
                //表示用户app
                appInfo.setIsUserApp(true);
            }

            if ((flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
                //表示在sd卡
                appInfo.setIsRom(false);
            } else {
                //表示内存
                appInfo.setIsRom(true);
            }
            appInfos.add(appInfo);
        }
        return appInfos;
    }

}