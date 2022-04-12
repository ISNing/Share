package org.exthmui.share.shared;

import android.content.Context;
import android.content.Intent;

public interface IShareBroadcastReceiver {
    String ACTION_ENABLE_DISCOVER = "org.exthmui.share.intent.action.ENABLE_DISCOVER";
    String ACTION_DISABLE_DISCOVER = "org.exthmui.share.intent.action.DISABLE_DISCOVER";
    String ACTION_ENABLE_RECEIVER = "org.exthmui.share.intent.action.ENABLE_RECEIVER";
    String ACTION_DISABLE_RECEIVER = "org.exthmui.share.intent.action.DISABLE_RECEIVER";
    String ACTION_START_DISCOVER = "org.exthmui.share.intent.action.START_DISCOVER";
    String ACTION_STOP_DISCOVER = "org.exthmui.share.intent.action.STOP_DISCOVER";
    String ACTION_START_RECEIVER = "org.exthmui.share.intent.action.START_RECEIVER";
    String ACTION_STOP_RECEIVER = "org.exthmui.share.intent.action.STOP_RECEIVER";

    String EXTRA_PLUGIN_CODE = "org.exthmui.share.extra.PLUGIN_CODE";

    void onReceive(Context context, Intent intent);
}
