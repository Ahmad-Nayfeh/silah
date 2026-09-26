package io.github.ahmadnayfeh.silah

import android.app.Application
import android.content.Context
import android.os.Build
import kotlinx.coroutines.launch

class SilahApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.reminders.ensureChannel()
        // Re-arm alarms whenever the process starts (covers force-stops and missed alarms).
        // Skipped under Robolectric so unit tests control every step themselves.
        if (Build.FINGERPRINT != "robolectric") {
            container.appScope.launch { container.reminders.onSystemEvent() }
        }
    }

    /** Lets tests swap in a container with a fixed clock or an in-memory database. */
    fun replaceContainer(newContainer: AppContainer) {
        container = newContainer
    }
}

val Context.container: AppContainer get() = (applicationContext as SilahApp).container
