package com.bari_ikutsu.linemsgbridge.utils

import android.content.AsyncQueryHandler
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.function.Consumer

class AutoConnectionDetector(private val context: Context) {
    companion object {
        private const val TAG = "AutoConnectionDetector"

        // columnName for provider to query on connection status
        private const val CAR_CONNECTION_STATE = "CarConnectionState"

        // auto app on your phone will send broadcast with this action when connection state changes
        private const val ACTION_CAR_CONNECTION_UPDATED =
            "androidx.car.app.connection.action.CAR_CONNECTION_UPDATED"

        // phone is not connected to car
        private const val CONNECTION_TYPE_NOT_CONNECTED = 0

        // phone is connected to Automotive OS
        private const val CONNECTION_TYPE_NATIVE = 1

        // phone is connected to Android Auto
        private const val CONNECTION_TYPE_PROJECTION = 2

        private const val QUERY_TOKEN = 42
        private const val CAR_CONNECTION_AUTHORITY = "androidx.car.app.connection"
        private val PROJECTION_HOST_URI =
            Uri.Builder().scheme("content").authority(CAR_CONNECTION_AUTHORITY).build()

        private val listeners: MutableList<OnCarConnectionStateListener> = ArrayList()
        private fun notifyCarConnected() {
            listeners.forEach(Consumer { l: OnCarConnectionStateListener -> l.onCarConnected() })
        }

        private fun notifyCarDisconnected() {
            listeners.forEach(Consumer { l: OnCarConnectionStateListener -> l.onCarDisconnected() })
        }

        private class CarConnectionQueryHandler(contentResolver: ContentResolver?) :
            AsyncQueryHandler(contentResolver) {
            override fun onQueryComplete(token: Int, cookie: Any?, response: Cursor?) {
                if (response == null) {
                    Log.d(
                        TAG,
                        "Null response from content provider when checking connection to the car, treating as disconnected"
                    )
                    notifyCarDisconnected()
                    return
                }
                val carConnectionTypeColumn = response.getColumnIndex(CAR_CONNECTION_STATE)
                if (carConnectionTypeColumn < 0) {
                    Log.d(
                        TAG,
                        "Connection to car response is missing the connection type, treating as disconnected"
                    )
                    notifyCarDisconnected()
                    return
                }
                if (!response.moveToNext()) {
                    Log.w(TAG, "Connection to car response is empty, treating as disconnected")
                    notifyCarDisconnected()
                    return
                }
                val connectionState = response.getInt(carConnectionTypeColumn)
                if (connectionState == CONNECTION_TYPE_NOT_CONNECTED) {
                    Log.i(TAG, "Android Auto disconnected")
                    notifyCarDisconnected()
                } else {
                    Log.i(TAG, "Android Auto connected")
                    Log.i(
                        TAG,
                        "onQueryComplete: $connectionState"
                    )
                    notifyCarConnected()
                }
            }
        }
    }

    private val carConnectionReceiver = CarConnectionBroadcastReceiver()
    private val carConnectionQueryHandler: CarConnectionQueryHandler =
        CarConnectionQueryHandler(context.contentResolver)

    interface OnCarConnectionStateListener {
        fun onCarConnected()
        fun onCarDisconnected()
    }

    fun setListener(listener: OnCarConnectionStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnCarConnectionStateListener) {
        listeners.remove(listener)
    }

    fun registerCarConnectionReceiver() {
        ContextCompat.registerReceiver(
            context, carConnectionReceiver, IntentFilter(ACTION_CAR_CONNECTION_UPDATED),
            ContextCompat.RECEIVER_EXPORTED
        )
        queryForState()
        Log.i(TAG, "registerCarConnectionReceiver: ")
    }

    fun unRegisterCarConnectionReceiver() {
        context.unregisterReceiver(carConnectionReceiver)
        Log.i(TAG, "unRegisterCarConnectionReceiver: ")
    }

    private fun queryForState() {
        val projection = arrayOf(CAR_CONNECTION_STATE)
        carConnectionQueryHandler.startQuery(
            QUERY_TOKEN,
            null,
            PROJECTION_HOST_URI,
            projection,
            null,
            null,
            null
        )
    }

    private inner class CarConnectionBroadcastReceiver : BroadcastReceiver() {
        // query for connection state every time the receiver receives the broadcast
        override fun onReceive(context: Context, intent: Intent) {
            queryForState()
        }
    }
}