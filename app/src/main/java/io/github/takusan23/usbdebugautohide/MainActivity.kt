package io.github.takusan23.usbdebugautohide

import android.app.*
import android.content.*
import android.os.*
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity

/**
 * pm grant io.github.takusan23.usbdebugautohide android.permission.WRITE_SECURE_SETTINGS
 * */
class MainActivity : AppCompatActivity() {


    private val PACKAGE_NAME = "jp.co.smbc.direct"
    private val activityCloseCallback = registerForActivityResult(ActivityClose()) {
        // アプリ終了時にUSBデバッグをONに戻す
        Settings.Global.putLong(contentResolver, Settings.Global.ADB_ENABLED, 1)
        // バックキーで戻ってきたらここに来る。アプリ閉じる
        finishAndRemoveTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SMBC起動前にUSBデバッグをOFF
        Settings.Global.putLong(contentResolver, Settings.Global.ADB_ENABLED, 0)

        // USBデバッグを無効にしないといけないアプリを起動
        val launchIntent = packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        launchIntent?.flags = 0 // 勝手に Intent.FLAG_ACTIVITY_NEW_TASK を指定するので消す
        activityCloseCallback.launch(launchIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, USBDebugAutoOnService::class.java))
        } else {
            startService(Intent(this, USBDebugAutoOnService::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, USBDebugAutoOnService::class.java))
    }

}

class USBDebugAutoOnService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "usbdebug_auto_on"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(channelId, "USBデバックを戻すサービス", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }.apply {
            setContentTitle("USBデバックを戻すサービス")
            setContentText("アプリが終了したらUSBデバッグが有効になります")
            setSmallIcon(R.drawable.ic_launcher_background).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // すぐに通知を出す
                setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
        }.build()

        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        println("アプリ終了")
        Settings.Global.putLong(contentResolver, Settings.Global.ADB_ENABLED, 1)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null;
    }

}

class ActivityClose : ActivityResultContract<Intent, Unit>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        // launch()のIntentがinput
        return input
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        // とくになし
    }
}
