package com.blockwar.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import android.view.Window
import android.view.ViewTreeObserver
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Keep controls outside the system navigation area, including transient bars.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.appSafeDrawingPadding(): Modifier {
    val view = LocalView.current
    DisposableEffect(view) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (focused && dialogWindow != null) hideGameNavigation(dialogWindow)
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        view.post { if (dialogWindow != null && view.isAttachedToWindow) hideGameNavigation(dialogWindow) }
        onDispose {
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }
    return windowInsetsPadding(
        WindowInsets.safeDrawing.union(WindowInsets.navigationBarsIgnoringVisibility)
    )
}

fun hideGameNavigation(window: Window) {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.navigationBars())
}
