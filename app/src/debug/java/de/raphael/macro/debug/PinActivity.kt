package de.raphael.macro.debug

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle

/**
 * Nur im Debug-Bau (für Emulator-Tests): legt ein Widget per Pin-Anfrage auf den Startbildschirm.
 * Aufruf: adb shell am start -n de.raphael.macro/.debug.PinActivity --es w tag
 */
class PinActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cls = when (intent.getStringExtra("w")) {
            "tag" -> "TagWidgetReceiver"
            "kcal" -> "KcalWidgetReceiver"
            else -> null
        }
        if (cls != null) {
            val mgr = getSystemService(AppWidgetManager::class.java)
            if (mgr.isRequestPinAppWidgetSupported) {
                mgr.requestPinAppWidget(ComponentName(packageName, "de.raphael.macro.widget.$cls"), null, null)
            }
        }
        finish()
    }
}
