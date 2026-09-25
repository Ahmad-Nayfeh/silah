package io.github.ahmadnayfeh.silah.notify

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import io.github.ahmadnayfeh.silah.container
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Invisible screen behind the WhatsApp button of the notification and the widget.
 * Android does not let a notification button start another app from the background,
 * so this records the contact and hands over to WhatsApp immediately — no app UI shows.
 */
class ActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val personId = intent.getLongExtra(Reminders.EXTRA_PERSON, -1)
        val container = container
        container.appScope.launch {
            val person = container.repo.person(personId)
            val launch = person?.let { Launchers.whatsapp(this@ActionActivity, it.phone) }
            if (person != null && launch != null) {
                container.contactedViaWhatsApp(person.id)
            }
            withContext(Dispatchers.Main) {
                if (launch != null) {
                    runCatching { startActivity(launch) }
                } else {
                    Toast.makeText(this@ActionActivity, "واتساب غير مثبّت على الجوال", Toast.LENGTH_LONG).show()
                }
                finish()
            }
        }
    }

    companion object {
        const val SOURCE_NOTIFICATION = 1
        const val SOURCE_WIDGET = 2

        fun intent(context: Context, personId: Long): Intent =
            Intent(context, ActionActivity::class.java)
                .setData(Uri.parse("silah://whatsapp/$personId"))
                .putExtra(Reminders.EXTRA_PERSON, personId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)

        fun pendingIntent(context: Context, personId: Long, source: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                source,
                intent(context, personId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
