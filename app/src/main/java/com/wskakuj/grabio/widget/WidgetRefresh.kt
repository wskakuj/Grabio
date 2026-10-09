package com.wskakuj.grabio.widget

import android.content.Context
import android.content.Intent

/** Prosi widget o odświeżenie (np. po zmianie danych w aplikacji). */
object WidgetRefresh {
    fun update(context: Context) {
        val intent = Intent(context, GrabioWidget::class.java).apply {
            action = GrabioWidget.ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }
}
