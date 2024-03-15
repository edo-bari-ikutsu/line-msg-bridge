package com.bari_ikutsu.linemsgbridge.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bari_ikutsu.linemsgbridge.R
import com.bari_ikutsu.linemsgbridge.data.PrefStore
import com.bari_ikutsu.linemsgbridge.utils.AutoConnectionDetector
import com.bari_ikutsu.linemsgbridge.utils.Consts
import com.bari_ikutsu.linemsgbridge.utils.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Random

class MsgBridgeService : NotificationListenerService() {
    companion object {
        private const val TAG = "LINEMsgBridge:MsgBridgeService"
    }

    private lateinit var prefStore: PrefStore
    private lateinit var autoConnectionListener: AutoConnection
    private lateinit var autoDetector: AutoConnectionDetector

    override fun onCreate() {
        super.onCreate()
        prefStore = PrefStore(applicationContext)

        // subscribe to Android Auto connection state
        autoConnectionListener = AutoConnection()
        autoDetector = AutoConnectionDetector(applicationContext)
        autoDetector.setListener(autoConnectionListener)
        autoDetector.registerCarConnectionReceiver()
    }

    override fun onDestroy() {
        // unsubscribe from Android Auto connection state
        autoDetector.unRegisterCarConnectionReceiver()
        super.onDestroy()
    }

    /**
     * Called when a new notification is posted
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        // connected to Android Auto?
        if (!autoConnectionListener.isConnected()) {
            return
        }

        // If the notification is not from LINE, do nothing
        if (sbn?.packageName != Consts.LINE_PACKAGENAME) {
            return
        }

        // If the notification is a group summary, do nothing
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return
        }

        // If the notification has public version, do nothing
        // Sometimes LINE sends two notifications (with and without publicVersion)
        // We only want to send one notification
        if (sbn.notification.publicVersion != null) {
            return
        }

        // Retrieve settings from DataStore
        var notificationTimeoutEnabled: Boolean
        var notificationTimeout: Float
        runBlocking(Dispatchers.IO) {
            notificationTimeoutEnabled = prefStore.getNotificationTimeoutEnabled.first()
            notificationTimeout = prefStore.getNotificationTimeout.first()
        }

        // Retrieve the notification and related information
        val notification = sbn.notification
        val bundle = notification.extras
        val title = bundle.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        var text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT, "").toString()
        if ("" == text) {
            text = bundle.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        }

        if (notification.channelId == Consts.LINE_MSG_CHANNEL_ID) {
            // If the notification does not have action for reply, do nothing
            // Action for reply should be the second action in the notification
            if (notification.actions == null || notification.actions.size <= 1 || notification.actions[1].title != getString(
                    R.string.action_reply
                )
            ) {
                Log.d(TAG, "No reply action in notification")
                return
            }

            Log.d(TAG, sbn.toString())

            // Retrieve the notification icon
            val notificationIcon =
                NotificationUtil.getNotificationIconBitmap(
                    applicationContext,
                    notification,
                    R.drawable.ic_notification_message
                )
            // Send the notification
            NotificationUtil.sendNotification(
                applicationContext,
                title,
                text,
                R.drawable.ic_notification_message,
                notificationIcon,
                Random().nextInt(100000),
                notification.actions[1],
                null,
                Consts.NOTIFICATION_CHANNEL_ID,
                if (notificationTimeoutEnabled) (notificationTimeout * 1000).toLong() else -1
            )
        }
    }

    private inner class AutoConnection : AutoConnectionDetector.OnCarConnectionStateListener {
        private var isConnected = false

        override fun onCarConnected() {
            isConnected = true
            Log.d(TAG, "Connected to Android Auto")
        }

        override fun onCarDisconnected() {
            isConnected = false
            Log.d(TAG, "Disconnected from Android Auto")
        }

        fun isConnected(): Boolean {
            return isConnected
        }
    }
}
