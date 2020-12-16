package com.ffflicker.ages_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

class AgeService : Service() {

    companion object {
        private val TAG = AgeService::class.java.simpleName
        public val ACTION_TYPE = "action_type"
        public val ACTION_AGES = 0
        public val ACTION_COUNTDOWN = 1
        public val ACTION_STATE = "action_state"

        val CHANNEL_ID = "Ages"
        val CHANNEL_NAME = "Ages"
        var year = 365 * 24 * 3600 * 1000.00
    }

    private lateinit var notificationManager:NotificationManager
    private lateinit var appWidgetManager: AppWidgetManager

    // ages
    private lateinit var agesNotification:Notification
    private lateinit var agesNotifyRemoteView:RemoteViews
    private lateinit var agesWidgetRemoteView:RemoteViews

    // countdown
    private lateinit var countdownNotification:Notification
    private lateinit var countdownNotifyRemoteView:RemoteViews
    private lateinit var countdownWidgetRemoteView:RemoteViews

    private val agesRunState:AtomicBoolean = AtomicBoolean(false)
    private val countdownRunState:AtomicBoolean = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        appWidgetManager = AppWidgetManager.getInstance(this)
        agesNotifyRemoteView = RemoteViews(packageName, R.layout.layout_ages_notify)
        agesWidgetRemoteView = RemoteViews(packageName, R.layout.layout_ages_widget)

        countdownNotifyRemoteView = RemoteViews(packageName, R.layout.layout_countdown_notify)
        countdownWidgetRemoteView = RemoteViews(packageName, R.layout.layout_countdown_widget)

        initNotification()
        initWidget()

        agesWorkThread.start()
        countdownWorkThread.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val state = intent.getBooleanExtra(ACTION_STATE, false)
        if (ACTION_AGES == intent.getIntExtra(ACTION_TYPE, 0)) {
            agesRunState.set(state)
        } else {
            countdownRunState.set(state)
        }

        return START_NOT_STICKY
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

            agesNotification = Notification.Builder(this, CHANNEL_ID)
                    .setContent(agesNotifyRemoteView)
                    .setCustomContentView(agesNotifyRemoteView)
                    .setCustomBigContentView(agesNotifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setOnlyAlertOnce(true)
                    .build()

            countdownNotification = Notification.Builder(this, CHANNEL_ID)
                .setContent(countdownNotifyRemoteView)
                .setCustomContentView(countdownNotifyRemoteView)
                .setCustomBigContentView(countdownNotifyRemoteView)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.icon_ages)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                .setOnlyAlertOnce(true)
                .build()

            startForeground(1, agesNotification)
            startForeground(2, countdownNotification)
        } else {
            agesNotification = Notification.Builder(this)
                    .setContent(agesNotifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setDefaults(8) // NotificationCompat.FLAG_ONLY_ALERT_ONCE
                    .setVibrate(LongArray(1){0})
                    .setOnlyAlertOnce(true)
                    .setSound(null)
                    .build()

            countdownNotification = Notification.Builder(this)
                .setContent(countdownNotifyRemoteView)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.icon_ages)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                .setDefaults(8) // NotificationCompat.FLAG_ONLY_ALERT_ONCE
                .setVibrate(LongArray(1){0})
                .setOnlyAlertOnce(true)
                .setSound(null)
                .build()

            notificationManager.notify(1, agesNotification)
            notificationManager.notify(2, countdownNotification)
        }
    }

    private fun initWidget() {



    }


    private val agesWorkThread:Thread = Thread {
        while (true) {
            if (!agesRunState.get()) continue

            // 算时间
            val time = DecimalFormat("0.00000000").format(((System.currentTimeMillis() - MainActivity.mDateOfBirth) / year * 100000000).roundToLong().toDouble() / 100000000)
            Log.d(TAG, "agesWorkThread time: $time")

            // 通知栏刷新
            agesNotification.contentView.setTextViewText(R.id.widget_years, time)
            notificationManager.notify(1, agesNotification)

            // widget刷新
            agesWidgetRemoteView.setTextViewText(R.id.widget_years, time)
            appWidgetManager.updateAppWidget(ComponentName(this, WidgetProvider::class.java), agesWidgetRemoteView)

            try {
                Thread.sleep(300)
            }catch(e: Error) {
                e.printStackTrace()
            }
        }
    }

    private val countdownWorkThread:Thread = Thread {
        while (true) {
            if (!countdownRunState.get()) continue

            // 算时间
            val time = DecimalFormat("0.00000000").format(((System.currentTimeMillis() - MainActivity.mDateOfBirth) / year * 100000000).roundToLong().toDouble() / 100000000)
            Log.d(TAG, "countdownWorkThread time: $time")

            // 通知栏刷新
            countdownNotification.contentView.setTextViewText(R.id.widget_far, "距离"+ MainActivity.mTargetYear + "岁还有")
            countdownNotification.contentView.setTextViewText(R.id.widget_years, time)
            notificationManager.notify(1, countdownNotification)

            // widget刷新
            countdownWidgetRemoteView.setTextViewText(R.id.widget_years, time)
            appWidgetManager.updateAppWidget(ComponentName(this, WidgetProvider::class.java), countdownWidgetRemoteView)

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
        super.onDestroy()
    }



    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}