package com.btremote.teny.presentation.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MediaControlWidgetDimensions {

    val height: Dp
    val padding: Dp
    val minIconSize: Dp
    val maxIconSize: Dp
    val minTextSp: TextUnit
    val maxTextSp: TextUnit

    private constructor(
        height: Dp,
        padding: Dp,
        minIconSize: Dp,
        maxIconSize: Dp,
        minTextSp: TextUnit,
        maxTextSp: TextUnit,
    ) {
        this.height = height
        this.padding = padding
        this.minTextSp = minTextSp
        this.maxTextSp = maxTextSp
        this.minIconSize = minIconSize
        this.maxIconSize = maxIconSize
    }

    companion object {

        val HEIGHT_THRESHOLD: Dp = 192.dp
        val BUTTON_SPACES: Dp = 2.dp

        fun calculateDimensions(width: Dp, height: Dp): MediaControlWidgetDimensions {
            val padding = 12.dp
            val divH = if(height > HEIGHT_THRESHOLD) 5.5f else 2.25f
            val iconSize: Dp = minOf((height - padding * 2) / divH, (width - padding * 2) / 5.5f)
            return MediaControlWidgetDimensions(
                height = height,
                padding = 12.dp,
                minIconSize = iconSize - 8.dp,
                maxIconSize = iconSize,
                minTextSp = 11.sp,
                maxTextSp = 16.sp
            )
        }
    }
}