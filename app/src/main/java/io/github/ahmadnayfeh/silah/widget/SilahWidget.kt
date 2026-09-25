package io.github.ahmadnayfeh.silah.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import io.github.ahmadnayfeh.silah.MainActivity
import io.github.ahmadnayfeh.silah.R
import io.github.ahmadnayfeh.silah.container
import io.github.ahmadnayfeh.silah.domain.ArabicText
import io.github.ahmadnayfeh.silah.domain.TodayDecision
import io.github.ahmadnayfeh.silah.notify.ActionActivity
import kotlinx.coroutines.launch

/** What the widget shows. */
sealed interface WidgetData {
    data class Suggest(val personId: Long, val name: String, val subtitle: String) : WidgetData
    data class Done(val name: String) : WidgetData
    data object NoOne : WidgetData
}

/**
 * Plain RemoteViews widget (no WorkManager, so no extra permissions):
 * today's person, "X days ago", and one WhatsApp button that works like the notification's.
 */
object SilahWidget {

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, SilahWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, render(context, load(context)))
    }

    suspend fun load(context: Context): WidgetData {
        val repo = context.container.repo
        val snapshot = repo.currentSnapshot()
        return when (val d = repo.decideToday()) {
            is TodayDecision.Suggest -> {
                val person = snapshot.person(d.ranked.personId) ?: return WidgetData.NoOne
                val occasion = d.ranked.occasion
                val subtitle = if (occasion != null) {
                    ArabicText.occasion(occasion.occasion.title, person.name, occasion.daysUntil)
                } else {
                    ArabicText.lastContact(repo.daysSince(snapshot.lastContactMillis(person.id)))
                }
                WidgetData.Suggest(person.id, person.name, subtitle)
            }
            is TodayDecision.Done -> WidgetData.Done(snapshot.person(d.personId)?.name.orEmpty())
            TodayDecision.NoOne -> WidgetData.NoOne
        }
    }

    fun render(context: Context, data: WidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val openApp = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        when (data) {
            is WidgetData.Suggest -> {
                views.setViewVisibility(R.id.widget_person, View.VISIBLE)
                views.setViewVisibility(R.id.widget_message, View.GONE)
                views.setTextViewText(R.id.widget_name, data.name)
                views.setTextViewText(R.id.widget_subtitle, data.subtitle)
                views.setOnClickPendingIntent(
                    R.id.widget_whatsapp,
                    ActionActivity.pendingIntent(context, data.personId, ActionActivity.SOURCE_WIDGET),
                )
            }
            is WidgetData.Done -> {
                views.setViewVisibility(R.id.widget_person, View.GONE)
                views.setViewVisibility(R.id.widget_message, View.VISIBLE)
                views.setTextViewText(R.id.widget_message, "تواصلت اليوم مع ${data.name}")
            }
            WidgetData.NoOne -> {
                views.setViewVisibility(R.id.widget_person, View.GONE)
                views.setViewVisibility(R.id.widget_message, View.VISIBLE)
                views.setTextViewText(R.id.widget_message, "لا أحد اليوم")
            }
        }
        return views
    }
}

class SilahWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        context.container.appScope.launch {
            try {
                SilahWidget.updateAll(context)
            } finally {
                pending?.finish()
            }
        }
    }
}
