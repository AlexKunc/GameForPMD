package com.example.gameforpmd

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class GoldWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val prefs = context.getSharedPreferences("gold_prefs", Context.MODE_PRIVATE)
            val goldRate = prefs.getString("gold_rate", "...")

            val views = RemoteViews(context.packageName, R.layout.widget_gold)
            views.setTextViewText(R.id.textGold, "Золото: $goldRate ₽")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
