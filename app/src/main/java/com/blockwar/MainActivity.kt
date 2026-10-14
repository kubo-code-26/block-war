package com.blockwar

import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.blockwar.ui.BlockWarScreen
import com.blockwar.ui.BlockWarViewModel
import com.blockwar.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BlockWarViewModel by viewModels()
    private val playGames by lazy { com.blockwar.auth.PlayGamesLogin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force standard 32-bit RGBA_8888 window format and default color mode
        // Prevents unsupported 101010-2 wide color gamut format and EGL buffer swap issues in web emulators
        window.setFormat(PixelFormat.RGBA_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()
        com.blockwar.ui.components.hideGameNavigation(window)
        connectPlayGames(false)
        setContent {
            MyApplicationTheme(darkTheme = true) {
                BlockWarScreen(viewModel = viewModel, onGooglePlayGamesSignIn = { connectPlayGames(true) })
            }
        }
    }

    private fun connectPlayGames(interactive: Boolean) {
        if (!viewModel.beginPlayGamesLogin(interactive)) return
        playGames.authenticate(interactive,
            onSuccess = { viewModel.onPlayGamesAuthenticated(it, interactive) },
            onError = { viewModel.onPlayGamesLoginError(it, interactive) })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) com.blockwar.ui.components.hideGameNavigation(window)
        // A dialog can take window focus while this Activity remains resumed.
        // Foreground/background changes are handled by onPause and onResume.
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResume()
    }
}

