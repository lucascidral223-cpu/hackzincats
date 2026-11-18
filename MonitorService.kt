package com.lucas.catsbot
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import org.json.JSONObject
import java.net.URL

class MonitorService : Service() {
    @Volatile private var on = false
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("mon", "Monitor", NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel("alert", "Alertas", NotificationManager.IMPORTANCE_HIGH))
        startForeground(1, Notification.Builder(this, "mon").setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Monitorando ${Store.name(this)}").build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        if (!on) { on = true; val topic = Store.topic(this); Thread { listen(topic) }.start() }
        return START_STICKY
    }

    private fun listen(topic: String) {
        var n = 100
        while (on) {
            try {
                val c = URL("https://ntfy.sh/$topic/json").openConnection().apply { readTimeout = 120000 }
                c.getInputStream().bufferedReader().forEachLine {
                    val j = JSONObject(it)
                    if (j.optString("event") == "message") {
                        getSystemService(NotificationManager::class.java).notify(n++,
                            Notification.Builder(this, "alert").setSmallIcon(android.R.drawable.stat_notify_error)
                                .setContentTitle(j.optString("title", "CATS Bot")).setContentText(j.optString("message")).build())
                    }
                }
            } catch (_: Exception) { Thread.sleep(5000) }
        }
    }
    override fun onDestroy() { on = false; super.onDestroy() }
}
