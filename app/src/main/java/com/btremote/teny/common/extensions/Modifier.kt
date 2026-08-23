package com.btremote.teny.common.extensions

import android.util.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.text.layoutDirection
import com.btremote.teny.common.utils.AppIcons.Help
import com.btremote.teny.common.utils.AppIcons.Mute
import com.btremote.teny.common.utils.AppIcons.VolumeDown
import com.btremote.teny.common.utils.AppIcons.VolumeUp
import org.koin.mp.KoinPlatform.getKoin
import java.util.Locale

// Workaround for some icons which should not be mirrored in RTL layout.
fun Modifier.autoMirroredIcon(icon: ImageVector): Modifier {
    when (icon) {
        Help -> {
            if (getKoin().get<Locale>().language == Locale.forLanguageTag("he").language) {
                return this.scale(scaleX = -1f, scaleY = 1f)
            }
        }

        Mute, VolumeUp, VolumeDown -> {
            if (getKoin().get<Locale>().layoutDirection == LayoutDirection.RTL) {
                return this.scale(scaleX = -1f, scaleY = 1f)
            }
        }
    }
    return this
}