package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.core.security.BiometricHelper
import com.example.features.CoinflowApp
import com.example.features.CoinflowSplashScreen
import com.example.features.CoinflowViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: CoinflowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.darkMode.collectAsState()
            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (showSplash) {
                    CoinflowSplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    CoinflowApp(
                        viewModel = viewModel,
                        onTriggerBiometrics = { onSuccess ->
                            if (BiometricHelper.isBiometricsAvailable(this)) {
                                BiometricHelper.showBiometricPrompt(
                                    activity = this,
                                    title = "App Locked",
                                    subtitle = "Authenticate to unlock Coinflow",
                                    onSuccess = {
                                        onSuccess()
                                    },
                                    onError = { error ->
                                        Toast.makeText(this, "Lockscreen error: $error", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                // If biometric security is unavailable/unconfigured on emulator/device, bypass overlay gracefully
                                onSuccess()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-lock on background resume if lock preference is enabled
        viewModel.lockApp()
    }
}
