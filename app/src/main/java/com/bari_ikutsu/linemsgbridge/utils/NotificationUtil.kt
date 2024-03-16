package com.bari_ikutsu.linemsgbridge.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput

class NotificationUtil {
    companion object {

        @SuppressLint("MissingPermission")
        fun sendNotification(
            context: Context,
            title: String,
            message: String,
            smallIcon: Int,
            largeIcon: Bitmap,
            conversationId: Int,
            replyAction: Notification.Action?,
            markAsReadAction: Notification.Action?,
            notificationChannelId: String,
            clearTimeout: Long
        ) {
            // Create a PendingIntent for the reply action to trigger.
            val msgReplyPendingIntent =
                if (replyAction != null) replyAction.actionIntent
                else PendingIntent.getBroadcast( // Dummy intent
                    context,
                    conversationId,
                    Intent(),
                    PendingIntent.FLAG_IMMUTABLE
                )
            // Build the reply action and add the remote input.
            val builder: NotificationCompat.Action.Builder = NotificationCompat.Action.Builder(
                smallIcon,
                "Reply",
                msgReplyPendingIntent
            )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
            for (remoteInput in replyAction!!.remoteInputs) {
                builder.addRemoteInput(RemoteInput.Builder(remoteInput.resultKey).build())
            }
            val newReplyAction = builder.build()

            // Create a PendingIntent for the mark as read action to trigger.
            val msgReadPendingIntent =
                if (markAsReadAction != null) markAsReadAction.actionIntent
                else
                    PendingIntent.getBroadcast( // Dummy intent
                        context,
                        conversationId,
                        Intent(),
                        PendingIntent.FLAG_IMMUTABLE
                    )
            // Build the mark as read action
            val newMarkAsReadAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
                smallIcon,
                "Mark as Read",
                msgReadPendingIntent
            )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build()

            val user = Person.Builder().setName("Dummy").build()
            val style = NotificationCompat.MessagingStyle(user)
                .addMessage(
                    message,
                    System.currentTimeMillis(),
                    Person.Builder().setName(title).build()
                )

            val mBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, notificationChannelId)
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(smallIcon)
                    .addInvisibleAction(newReplyAction)
                    .addInvisibleAction(newMarkAsReadAction)
                    .setStyle(style)
            if (clearTimeout >= 0) {
                mBuilder.setTimeoutAfter(clearTimeout)
            }
            val msgNotificationManager = NotificationManagerCompat.from(context)
            msgNotificationManager.notify(conversationId, mBuilder.build())
        }

        /**
         * Get the best notification icon (large, small, default) and return it as bitmap
         */
        fun getNotificationIconBitmap(
            context: Context,
            notification: Notification,
            defaultIcon: Int
        ): Bitmap {
            var bmp: Bitmap? = null
            var icon = notification.getLargeIcon()
            if (icon == null) {
                icon = notification.smallIcon
            }
            if (icon != null) {
                bmp = drawableToBitMap(icon.loadDrawable(context))
            }
            if (bmp == null) {
                bmp = BitmapFactory.decodeResource(context.resources, defaultIcon)
            }
            return bmp as Bitmap
        }

        /**
         * Convert a drawable to a bitmap
         */
        private fun drawableToBitMap(drawable: Drawable?): Bitmap? {
            if (drawable == null) {
                return null
            }
            return if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                drawable.draw(canvas)
                bitmap
            }
        }
    }
}
