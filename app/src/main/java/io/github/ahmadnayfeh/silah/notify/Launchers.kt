package io.github.ahmadnayfeh.silah.notify

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.ahmadnayfeh.silah.domain.PhoneNumbers

/** Builds the intents that leave the app: WhatsApp and the phone dialer. No permissions needed. */
object Launchers {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    private fun installedWhatsApp(context: Context): String? =
        WHATSAPP_PACKAGES.firstOrNull { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }

    /**
     * With a number: the wa.me chat link, sent straight to WhatsApp when it is installed.
     * Without a number: just opens WhatsApp. Null when there is nothing to open.
     */
    fun whatsapp(context: Context, phone: String?): Intent? {
        val pkg = installedWhatsApp(context)
        val intent = if (phone != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${PhoneNumbers.waDigits(phone)}")).apply {
                if (pkg != null) setPackage(pkg)
            }
        } else {
            pkg?.let { context.packageManager.getLaunchIntentForPackage(it) } ?: return null
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Opens the dialer with the number ready; the user presses call (no CALL_PHONE permission). */
    fun dial(phone: String): Intent =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
