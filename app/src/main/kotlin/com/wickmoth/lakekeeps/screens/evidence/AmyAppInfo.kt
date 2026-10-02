package com.wickmoth.lakekeeps.screens.evidence

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.game.littlebird.Flag

/**
 * The app info page for System Sync Service: installed in April, never made to sleep, allowed
 * her location all the time, her messages and her microphone. The fine print gives its real
 * package name away.
 */
internal object AppInfoShot {
    private const val DETAILS_TOP = 300f
    private const val DETAIL_H = 56f
    private const val PERMISSIONS_TOP = 508f
    private const val PERMISSION_H = 52f

    private val details = listOf(
        "Installed" to "9 April",
        "Mobile data" to "1.4 GB since 9 April",
        "Battery" to "Unrestricted",
    )

    private val permissions = listOf(
        "Location" to "Allowed all the time",
        "Messages" to "Allowed",
        "Microphone" to "Allowed",
        "Accessibility" to "On",
    )

    private fun detailTop(i: Int) = DETAILS_TOP + i * DETAIL_H
    private fun permissionTop(i: Int) = PERMISSIONS_TOP + i * PERMISSION_H

    /** What the board's printout of this page crops to: the app, its install date and its permissions. */
    val header = Rect(16f, 92f, 344f, 224f)
    val installed = Rect(20f, detailTop(0), 340f, detailTop(1))
    val permissionRows = Rect(20f, PERMISSIONS_TOP - 8f, 340f, permissionTop(permissions.size))
    val packageLine = Rect(20f, 730f, 340f, 770f)

    val hotspots = listOf(
        Hotspot(installed, Find.Pin(Flag.PIN_INSTALLED, "Installed 9 April")),
        Hotspot(permissionRows, Find.Pin(Flag.PIN_PERMISSIONS, "Location all the time, messages, microphone, accessibility")),
        Hotspot(packageLine, Find.Pin(Flag.PIN_PACKAGE, "com.keypr.familysafe")),
        Hotspot(Rect(240f, 236f, 336f, 282f), Find.Pass("A system app you can uninstall. Real ones can't be.")),
        Hotspot(Rect(20f, detailTop(1), 340f, detailTop(2)), Find.Pass("A system service with a data habit.")),
        Hotspot(Rect(20f, detailTop(2), 340f, detailTop(3)), Find.Pass("Unrestricted. It never has to sleep.")),
        Hotspot(header, Find.Pass("Look at it beside the real system apps.")),
    )

    @Composable
    fun BoxScope.Page() {
        StatusBar("8:41", battery = 33)
        PageTitle("App info")
        ImpostorIcon(148f, 100f, 64f)
        At(20f, 174f, "System Sync Service", shotText(20.sp, FontWeight.Bold, align = TextAlign.Center), width = 320f)
        At(20f, 202f, "Version 4.2.1", shotText(13.sp, color = Shot.Muted, align = TextAlign.Center), width = 320f)
        PillButton(24f, 240f, 96f, "Open")
        PillButton(132f, 240f, 96f, "Force stop")
        PillButton(240f, 240f, 96f, "Uninstall")
        details.forEachIndexed { i, (label, value) ->
            val y = detailTop(i)
            At(24f, y + 6f, label, shotText(15.sp, FontWeight.SemiBold), width = 300f)
            At(24f, y + 27f, value, shotText(13.sp, color = Shot.Muted), width = 300f)
            if (i < details.lastIndex) Divider(y + DETAIL_H - 1f, from = 24f)
        }
        At(24f, 476f, "Permissions", shotText(14.sp, FontWeight.Bold, Shot.Accent), width = 200f)
        permissions.forEachIndexed { i, (label, value) ->
            val y = permissionTop(i)
            At(24f, y + 4f, label, shotText(15.sp, FontWeight.SemiBold), width = 300f)
            At(24f, y + 25f, value, shotText(13.sp, color = Shot.Muted), width = 300f)
        }
        At(20f, 742f, "Package  com.keypr.familysafe", shotText(10.5.sp, color = Shot.Faint, align = TextAlign.Center), width = 320f)
    }
}
