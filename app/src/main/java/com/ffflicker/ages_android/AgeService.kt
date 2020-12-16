package com.ffflicker.ages_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RelativeLayout
import android.widget.RemoteViews
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

class AgeService : Service() {

    companion object {
        private val TAG = AgeService::class.java.simpleName
        val CHANNEL_ID = "Ages"
        val CHANNEL_NAME = "Ages"
        var year = 365 * 24 * 3600 * 1000.00
    }

    private lateinit var notificationManager:NotificationManager
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var notification:Notification
    private lateinit var notifyRemoteView:RemoteViews
    private lateinit var widgetRemoteView:RemoteViews
    private val runState:AtomicBoolean = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        appWidgetManager = AppWidgetManager.getInstance(this)
        notifyRemoteView = RemoteViews(packageName, R.layout.layout_notify)
        widgetRemoteView = RemoteViews(packageName, R.layout.layout_widget)

        initNotification()
        initWidget()

        workThread.start()
        runState.set(true)
    }

    private fun initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(false)
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setVibrationPattern(LongArray(1){0})
            channel.setSound(null, null)
            channel.description = CHANNEL_NAME
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)

            notification = Notification.Builder(this, CHANNEL_ID)
                    .setContent(notifyRemoteView)
                    .setCustomContentView(notifyRemoteView)
                    .setCustomBigContentView(notifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setOnlyAlertOnce(true)
                    .build()

            startForeground(1, notification)
        } else {
            notification = Notification.Builder(this)
                    .setContent(notifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setDefaults(8) // NotificationCompat.FLAG_ONLY_ALERT_ONCE
                    .setVibrate(LongArray(1){0})
                    .setOnlyAlertOnce(true)
                    .setSound(null)
                    .build()

            notificationManager.notify(1, notification)
        }
    }

    private fun initWidget() {



    }


    private val workThread:Thread = Thread {
        while (runState.get()) {
            // 算时间
            val time = DecimalFormat("0.00000000").format(((System.currentTimeMillis() - MainActivity.mDateOfBirth) / year * 100000000).roundToLong().toDouble() / 100000000)
            Log.d(TAG, "workThread time: $time")

            // 通知栏刷新
            notification.contentView.setTextViewText(R.id.widget_years, time)
            notificationManager.notify(1, notification)

            // widget刷新
            widgetRemoteView.setTextViewText(R.id.widget_years, time)
            appWidgetManager.updateAppWidget(ComponentName(this, WidgetProvider::class.java), widgetRemoteView)

            try {
                Thread.sleep(300)
            }catch(e: Error) {
                e.printStackTrace()
            }
        }
    }


    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true)
        runState.set(false)
        super.onDestroy()
    }



    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}