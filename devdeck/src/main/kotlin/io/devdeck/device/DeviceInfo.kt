package io.devdeck.device

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.BulletItem
import io.devdeck.components.InfoGroup
import java.util.Locale
import kotlin.math.roundToInt

@Composable
public fun DeviceInfo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val info = remember(context) { deviceInfo(context) }
    InfoGroup(modifier.fillMaxWidth()) {
        info.forEach { (label, value) -> BulletItem(label = label, value = value) }
    }
}

internal fun deviceInfo(context: Context): List<Pair<String, String>> {
    val metrics = DisplayMetrics()
    val display = (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY)
    @Suppress("DEPRECATION")
    display.getRealMetrics(metrics)
    val density = metrics.density
    return listOf(
        "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.US) },
        "Model" to Build.MODEL,
        "Resolution (px)" to "${metrics.widthPixels} * ${metrics.heightPixels}",
        "Resolution (dp)" to "${(metrics.widthPixels / density).roundToInt()} * ${(metrics.heightPixels / density).roundToInt()}",
        "Density (dpi)" to metrics.densityDpi.toString(),
        "Android SDK version" to Build.VERSION.SDK_INT.toString(),
    )
}
