package com.ffflicker.ages_android

import android.annotation.TargetApi
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

        val CHANNEL_ID_AGES = "Ages"
        val CHANNEL_ID_COUNTDOWN = "CountDown"
        var year = 365 * 24 * 3600 * 1000.00
        val NOTIFY_ID_AGES = 1
        val NOTIFY_ID_COUNTDOWN = 2
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var agesChannel: NotificationChannel
    private lateinit var countdownChannel: NotificationChannel

    // ages
    private lateinit var agesNotification: Notification
    private lateinit var agesNotifyRemoteView: RemoteViews
    private lateinit var agesWidgetRemoteView: RemoteViews

    // countdown
    private lateinit var countdownNotification: Notification
    private lateinit var countdownNotifyRemoteView: RemoteViews
    private lateinit var countdownWidgetRemoteView: RemoteViews

    private val agesRunState: AtomicBoolean = AtomicBoolean(false)
    private val countdownRunState: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        appWidgetManager = AppWidgetManager.getInstance(this)
        agesNotifyRemoteView = RemoteViews(packageName, R.layout.layout_ages_notify)
        agesWidgetRemoteView = RemoteViews(packageName, R.layout.layout_ages_widget)
        setTextColor(agesNotifyRemoteView, ACTION_AGES)

        countdownNotifyRemoteView = RemoteViews(packageName, R.layout.layout_countdown_notify)
        countdownWidgetRemoteView = RemoteViews(packageName, R.layout.layout_countdown_widget)
        setTextColor(countdownNotifyRemoteView, ACTION_AGES)

        initAgesChannel()
        initCountdownChannel()
        initCountdownNotification()
        initAgesNotification()

        countdownWorkThread.name = "countdownWorkThread"
        agesWorkThread.name = "agesWorkThread"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val state = intent.getBooleanExtra(ACTION_STATE, false)
        if (ACTION_AGES == intent.getIntExtra(ACTION_TYPE, 0)) {
            if (state && !agesWorkThread.isAlive)
                agesWorkThread.start()
            agesRunState.set(state)
            if (!state)
                closeNotify(ACTION_AGES)
        } else {
            if (state && !countdownWorkThread.isAlive)
                countdownWorkThread.start()
            countdownRunState.set(state)
            if (!state)
                closeNotify(ACTION_COUNTDOWN)
        }

        return START_NOT_STICKY
    }

    private fun closeNotify(type: Int) {
        if (ACTION_AGES == type) {
            notificationManager.cancel(NOTIFY_ID_AGES);
        }

        if (ACTION_COUNTDOWN == type) {
            notificationManager.cancel(NOTIFY_ID_COUNTDOWN)
        }
    }

    private fun initAgesNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!this::agesNotification.isInitialized) {
                agesNotification = Notification.Builder(this, CHANNEL_ID_AGES)
                    .setContent(agesNotifyRemoteView)
                    .setCustomContentView(agesNotifyRemoteView)
                    .setCustomBigContentView(agesNotifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setOnlyAlertOnce(true)
                    .build()
            }
            // 前台service保活 这个就不关了
            startForeground(NOTIFY_ID_AGES, agesNotification)
        } else {
            if (!this::agesNotification.isInitialized) {
                agesNotification = Notification.Builder(this)
                    .setContent(agesNotifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setDefaults(8) // NotificationCompat.FLAG_ONLY_ALERT_ONCE
                    .setVibrate(LongArray(1) { 0 })
                    .setOnlyAlertOnce(true)
                    .setSound(null)
                    .build()
            }
        }
    }

    private fun initCountdownNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!this::countdownNotification.isInitialized) {
                countdownNotification = Notification.Builder(this, CHANNEL_ID_COUNTDOWN)
                    .setContent(countdownNotifyRemoteView)
                    .setCustomContentView(countdownNotifyRemoteView)
                    .setCustomBigContentView(countdownNotifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setOnlyAlertOnce(true)
                    .build()
            }
            startForeground(NOTIFY_ID_COUNTDOWN, countdownNotification)
        } else {
            if (!this::countdownNotification.isInitialized) {
                countdownNotification = Notification.Builder(this)
                    .setContent(countdownNotifyRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.icon_ages)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_ages))
                    .setDefaults(8) // NotificationCompat.FLAG_ONLY_ALERT_ONCE
                    .setVibrate(LongArray(1) { 0 })
                    .setOnlyAlertOnce(true)
                    .setSound(null)
                    .build()
            }
        }
    }


    private fun initAgesChannel() {
        if (!this::agesChannel.isInitialized && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            agesChannel = NotificationChannel(
                CHANNEL_ID_AGES,
                CHANNEL_ID_AGES,
                NotificationManager.IMPORTANCE_HIGH
            )
            agesChannel.setShowBadge(false)
            agesChannel.enableLights(false)
            agesChannel.enableVibration(false)
            agesChannel.setVibrationPattern(LongArray(1) { 0 })
            agesChannel.setSound(null, null)
            agesChannel.description = CHANNEL_ID_AGES
            agesChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(agesChannel)
        }
    }

    private fun initCountdownChannel() {
        if (!this::countdownChannel.isInitialized && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            countdownChannel = NotificationChannel(
                CHANNEL_ID_COUNTDOWN,
                CHANNEL_ID_COUNTDOWN,
                NotificationManager.IMPORTANCE_HIGH
            )
            countdownChannel.setShowBadge(false)
            countdownChannel.enableLights(false)
            countdownChannel.enableVibration(false)
            countdownChannel.setVibrationPattern(LongArray(1) { 0 })
            countdownChannel.setSound(null, null)
            countdownChannel.description = CHANNEL_ID_COUNTDOWN
            countdownChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(countdownChannel)
        }
    }


    private val agesWorkThread: Thread = Thread {
        while (true) {
            if (!agesRunState.get()) continue

            // 算时间
            val time = DecimalFormat("0.00000000").format(
                ((System.currentTimeMillis() - MainActivity.mDateOfBirth) / year * 100000000).roundToLong()
                    .toDouble() / 100000000
            )
            Log.d(TAG, "agesWorkThread time: $time")

            // 通知栏刷新
            agesNotification.contentView.setTextViewText(R.id.widget_years, time)
            setTextColor(agesNotification.contentView, ACTION_AGES)
            notificationManager.notify(NOTIFY_ID_AGES, agesNotification)

            // widget刷新
            agesWidgetRemoteView.setTextViewText(R.id.widget_years, time)
            appWidgetManager.updateAppWidget(
                ComponentName(this, AgesWidgetProvider::class.java),
                agesWidgetRemoteView
            )

            try {
                Thread.sleep(300)
            } catch (e: Error) {
                e.printStackTrace()
            }
        }
    }

    private val countdownWorkThread: Thread = Thread {
        var mTargetYear = 35
        while (true) {
            if (!countdownRunState.get()) continue

            // 算时间
            mTargetYear = MainActivity.mTargetAge
            val time = DecimalFormat("0.00000000").format(
                ((MainActivity.mTargetTimpStamp - System.currentTimeMillis()) / year * 100000000).roundToLong()
                    .toDouble() / 100000000
            )
            Log.d(TAG, "countdownWorkThread time: $time")

            // 通知栏刷新
            countdownNotification.contentView.setTextViewText(
                R.id.widget_far,
                "距离" + mTargetYear + "岁还有"
            )
            countdownNotification.contentView.setTextViewText(R.id.widget_years, time)
            setTextColor(countdownNotification.contentView, ACTION_COUNTDOWN)
            notificationManager.notify(NOTIFY_ID_COUNTDOWN, countdownNotification)

            // widget刷新
            countdownWidgetRemoteView.setTextViewText(
                R.id.widget_far,
                "距离" + mTargetYear + "岁还有"
            )
            countdownWidgetRemoteView.setTextViewText(R.id.widget_years, time)
            appWidgetManager.updateAppWidget(
                ComponentName(this, CountdownWidgetProvider::class.java),
                countdownWidgetRemoteView
            )

            try {
                Thread.sleep(300)
            } catch (e: Error) {
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

    private fun setTextColor(remoteView: RemoteViews, type: Int) {
        remoteView.setTextColor(R.id.widget_years_end, MainActivity.textColor)
        remoteView.setTextColor(R.id.widget_years, MainActivity.textColor)

        if (ACTION_COUNTDOWN == type)
            remoteView.setTextColor(R.id.widget_far, MainActivity.textColor)
    }

}