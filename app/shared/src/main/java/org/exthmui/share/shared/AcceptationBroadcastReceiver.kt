package org.exthmui.share.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.exthmui.share.shared.base.FileInfo
import org.exthmui.share.shared.base.receive.SenderInfo
import org.exthmui.share.shared.listeners.OnReceiveShareBroadcastActionListener

class AcceptationBroadcastReceiver : BroadcastReceiver() {
    private var mOnReceiveShareBroadcastActionListener: OnReceiveShareBroadcastActionListener? =
        null

    fun setListener(listener: OnReceiveShareBroadcastActionListener?) {
        mOnReceiveShareBroadcastActionListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (mOnReceiveShareBroadcastActionListener == null) return
        val notificationId = intent.getIntExtra(NotificationCompat.EXTRA_NOTIFICATION_ID, -1)
        val pluginCode = intent.getStringExtra(EXTRA_PLUGIN_CODE)
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        val action = intent.action
        if (action != null) when (action) {
            ACTION_ACCEPTATION_DIALOG -> {
                val senderInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_PEER_INFO_TRANSFER, SenderInfo::class.java)
                } else {
                    intent.getSerializableExtra(EXTRA_PEER_INFO_TRANSFER) as SenderInfo?
                }
                val fileInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_FILE_INFOS, Array<FileInfo>::class.java)
                } else {
                    intent.getSerializableExtra(EXTRA_FILE_INFOS) as Array<FileInfo>?
                }
                mOnReceiveShareBroadcastActionListener!!.onReceiveActionAcceptationDialog(
                    pluginCode,
                    requestId,
                    senderInfo,
                    fileInfos,
                    notificationId
                )
            }

            ACTION_ACCEPT -> {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
                mOnReceiveShareBroadcastActionListener!!.onReceiveActionAcceptShare(
                    pluginCode,
                    requestId
                )
            }

            ACTION_REJECT -> {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
                mOnReceiveShareBroadcastActionListener!!.onReceiveActionRejectShare(
                    pluginCode,
                    requestId
                )
            }
        }
    }

    companion object {
        const val ACTION_ACCEPTATION_DIALOG = "org.exthmui.share.intent.action.ACCEPTATION_DIALOG"
        const val ACTION_ACCEPT = "org.exthmui.share.intent.action.ACCEPT_SHARE"
        const val ACTION_REJECT = "org.exthmui.share.intent.action.REJECT_SHARE"
        const val EXTRA_PLUGIN_CODE = "org.exthmui.share.extra.PLUGIN_CODE"
        const val EXTRA_REQUEST_ID = "org.exthmui.share.extra.REQUEST_ID"
        const val EXTRA_PEER_INFO_TRANSFER = "org.exthmui.share.extra.PEER_INFO_TRANSFER"
        const val EXTRA_FILE_INFOS = "org.exthmui.share.extra.FILE_INFOS"

        @JvmStatic
        val intentFilter: IntentFilter
            get() {
                val intentFilter = IntentFilter()
                intentFilter.addAction(ACTION_ACCEPTATION_DIALOG)
                intentFilter.addAction(ACTION_ACCEPT)
                intentFilter.addAction(ACTION_REJECT)
                return intentFilter
            }
    }
}
