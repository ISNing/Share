package org.exthmui.share.web.conf;

import android.content.Context;

import com.yanzhenjie.andserver.annotation.Config;
import com.yanzhenjie.andserver.framework.config.WebConfig;
import com.yanzhenjie.andserver.framework.website.AssetsWebsite;
import com.yanzhenjie.andserver.framework.website.StorageWebsite;

@Config
public class ServerConfig implements WebConfig {

    @Override
    public void onConfig(Context context, Delegate delegate) {
        // 增加一个位于assets的web目录的网站
        delegate.addWebsite(new AssetsWebsite(context, "/web/"));

        // 增加一个位于/sdcard/Download/AndServer/目录的网站
        delegate.addWebsite(new StorageWebsite("/sdcard/Download/AndServer/"));
    }
}