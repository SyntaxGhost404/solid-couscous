package com.wickmoth.lakekeeps.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

enum class Haptic { Tick, Press, Reject, Confirm }

fun View.haptic(kind: Haptic) {
    val constant = when (kind) {
        Haptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
        Haptic.Press -> HapticFeedbackConstants.CONTEXT_CLICK
        Haptic.Reject ->
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        Haptic.Confirm ->
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
    }
    performHapticFeedback(constant)
}

/**
 * Ripple-free tap handling for artwork: reports press state so each object can animate its own
 * physical response, and exposes the object to accessibility services as a button.
 */
@Composable
fun Modifier.tactile(
    label: String,
    onPress: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    onTap: () -> Unit,
): Modifier {
    val tap by rememberUpdatedState(onTap)
    val press by rememberUpdatedState(onPress)
    return this
        .semantics(mergeDescendants = true) {
            contentDescription = label
            role = Role.Button
            if (!enabled) disabled()
            onClick { tap(); true }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    press(true)
                    tryAwaitRelease()
                    press(false)
                },
                onTap = { tap() },
            )
        }
}
