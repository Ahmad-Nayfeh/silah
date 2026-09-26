package io.github.ahmadnayfeh.silah

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ahmadnayfeh.silah.ui.MainViewModel
import io.github.ahmadnayfeh.silah.ui.OnboardingScreen
import io.github.ahmadnayfeh.silah.ui.SilahAppUi
import io.github.ahmadnayfeh.silah.ui.theme.SilahTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = container
        setContent {
            val vm: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(container) } })
            val settings by vm.settings.collectAsStateWithLifecycle()
            val state by vm.appState.collectAsStateWithLifecycle()
            val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                vm.finishOnboarding()
            }
            SilahTheme(settings.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val s = state
                    when {
                        s == null -> Unit
                        !s.onboarded -> OnboardingScreen(onStart = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.finishOnboarding()
                            }
                        })
                        else -> SilahAppUi(vm)
                    }
                }
            }
        }
    }
}
