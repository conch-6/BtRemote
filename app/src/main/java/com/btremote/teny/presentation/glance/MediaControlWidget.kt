package com.btremote.teny.presentation.glance

import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import com.btremote.teny.R
import com.btremote.teny.domain.usecases.BluetoothHidServiceUseCase
import com.btremote.teny.presentation.activities.MainActivity
import com.btremote.teny.presentation.glance.MediaControlWidgetDimensions.Companion.BUTTON_SPACES
import com.btremote.teny.presentation.glance.MediaControlWidgetDimensions.Companion.HEIGHT_THRESHOLD
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_BACK
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_DOWN
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_HOME
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_LEFT
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_MULTIMEDIA_NEXT
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_MULTIMEDIA_PLAY_PAUSE
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_MULTIMEDIA_PREVIOUS
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_PICK
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_RIGHT
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_UP
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_VOLUME_DOWN
import com.btremote.teny.presentation.services.NotificationBroadcastReceiver.Companion.ACTION_VOLUME_UP
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MediaControlWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediaControlWidget()
}

class MediaControlWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bluetoothHidServiceUseCase by inject<BluetoothHidServiceUseCase>()
        provideContent {
            GlanceTheme {
                val dimensions = MediaControlWidgetDimensions.calculateDimensions(
                    width = LocalSize.current.width,
                    height = LocalSize.current.height
                )
                val state by bluetoothHidServiceUseCase.getDeviceHidConnectionState().collectAsState()
                if (state.state != BluetoothHidDevice.STATE_CONNECTED) {
                    AppLauncher(
                        dimensions = dimensions,
                        context = context
                    )
                } else {
                    MediaControls(
                        deviceName = state.deviceName,
                        dimensions = dimensions,
                        context = context
                    )
                }
            }
        }
    }

    @Composable
    private fun AppLauncher(
        dimensions: MediaControlWidgetDimensions,
        context: Context = LocalContext.current
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(dimensions.padding)
                .background(GlanceTheme.colors.widgetBackground),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            GlanceWidgetTitle(
                appName = context.getString(R.string.app_name),
                connectionState = context.getString(R.string.connected_off),
                dimensions = dimensions
            )

            Spacer(GlanceModifier.defaultWeight())

            if(dimensions.height > HEIGHT_THRESHOLD) {
                Image(
                    provider = ImageProvider(R.drawable.round_link_off_24),
                    contentDescription = context.getString(R.string.connected_off),
                    modifier = GlanceModifier.fillMaxSize().defaultWeight(),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                )
                Spacer(GlanceModifier.defaultWeight())
            }

            Button(
                text = context.getString(R.string.connect),
                onClick = actionStartActivity(
                    intent = getStartActivityIntent(context)
                ),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun MediaControls(
        deviceName: String,
        dimensions: MediaControlWidgetDimensions,
        context: Context = LocalContext.current
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(dimensions.padding)
                .background(GlanceTheme.colors.widgetBackground),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Top
            ) {

                GlanceWidgetTitle(
                    appName = context.getString(R.string.app_name),
                    connectionState = context.getString(R.string.connected_on, deviceName),
                    dimensions = dimensions,
                    modifier = GlanceModifier.defaultWeight()
                )

                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.round_open_in_full_24),
                    contentDescription = context.getString(R.string.open),
                    onClick = actionStartActivity(
                        intent = getStartActivityIntent(context)
                    ),
                    modifier = GlanceModifier.size(dimensions.minIconSize),
                    backgroundColor = GlanceTheme.colors.tertiary,
                    contentColor = GlanceTheme.colors.onTertiary
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Top
            ) {

                // Multimedia

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_fast_rewind_24),
                    contentDescription = context.getString(R.string.rewind),
                    onClick = mediaControlAction(context, ACTION_MULTIMEDIA_PREVIOUS),
                    rippleOverride = R.drawable.glance_button_ripple_left,
                    backgroundDrawable = R.drawable.glance_button_shape_left,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_play_pause_24),
                    contentDescription = context.getString(R.string.play),
                    onClick = mediaControlAction(context, ACTION_MULTIMEDIA_PLAY_PAUSE),
                    rippleOverride = R.drawable.glance_button_ripple_middle,
                    backgroundDrawable = R.drawable.glance_button_shape_middle,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_fast_forward_24),
                    contentDescription = context.getString(R.string.forward),
                    onClick = mediaControlAction(context, ACTION_MULTIMEDIA_NEXT),
                    rippleOverride = R.drawable.glance_button_ripple_right,
                    backgroundDrawable = R.drawable.glance_button_shape_right,
                    dimensions = dimensions
                )

                // Separator

                Spacer(GlanceModifier.defaultWeight())

                // Audio

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_volume_down_24),
                    contentDescription = context.getString(R.string.volume_down),
                    onClick = mediaControlAction(context, ACTION_VOLUME_DOWN),
                    rippleOverride = R.drawable.glance_button_ripple_left,
                    backgroundDrawable = R.drawable.glance_button_shape_left,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_volume_up_24),
                    contentDescription = context.getString(R.string.volume_up),
                    onClick = mediaControlAction(context, ACTION_VOLUME_UP),
                    rippleOverride = R.drawable.glance_button_ripple_right,
                    backgroundDrawable = R.drawable.glance_button_shape_right,
                    dimensions = dimensions
                )
            }

            if(dimensions.height > HEIGHT_THRESHOLD) {

                Spacer(GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlanceDpad(
                        context = context,
                        dimensions = dimensions,
                        modifier = GlanceModifier
                    )

                    Spacer(GlanceModifier.defaultWeight())

                    GlanceCustomIconButton(
                        imageProvider = ImageProvider(R.drawable.round_arrow_back_24),
                        contentDescription = context.getString(R.string.back),
                        onClick = mediaControlAction(context, ACTION_BACK),
                        rippleOverride = R.drawable.glance_button_ripple_left,
                        backgroundDrawable = R.drawable.glance_button_shape_left,
                        dimensions = dimensions
                    )

                    Spacer(GlanceModifier.width(BUTTON_SPACES))

                    GlanceCustomIconButton(
                        imageProvider = ImageProvider(R.drawable.round_home_24),
                        contentDescription = context.getString(R.string.home),
                        onClick = mediaControlAction(context, ACTION_HOME),
                        rippleOverride = R.drawable.glance_button_ripple_right,
                        backgroundDrawable = R.drawable.glance_button_shape_right,
                        dimensions = dimensions
                    )
                }
            }
        }
    }

    private fun getStartActivityIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    private fun mediaControlAction(context: Context, action: String) =
        actionSendBroadcast(Intent(context, NotificationBroadcastReceiver::class.java).apply { setAction(action) })

    /**
     * D-Pad
     */
    @Composable
    fun GlanceDpad(
        context: Context,
        dimensions: MediaControlWidgetDimensions,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Column(modifier = modifier) {
            Row(
                modifier = GlanceModifier.wrapContentWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(GlanceModifier.width(dimensions.maxIconSize))

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_keyboard_arrow_up_24),
                    contentDescription = context.getString(R.string.up),
                    onClick = mediaControlAction(context, ACTION_UP),
                    rippleOverride = R.drawable.glance_button_ripple_up,
                    backgroundDrawable = R.drawable.glance_button_shape_up,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                Spacer(GlanceModifier.width(dimensions.maxIconSize))
            }

            Spacer(GlanceModifier.height(BUTTON_SPACES))

            Row(
                modifier = GlanceModifier.wrapContentWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_keyboard_arrow_left_24),
                    contentDescription = context.getString(R.string.left),
                    onClick = mediaControlAction(context, ACTION_LEFT),
                    rippleOverride = R.drawable.glance_button_ripple_left,
                    backgroundDrawable = R.drawable.glance_button_shape_left,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.outline_circle_24),
                    contentDescription = context.getString(R.string.pick),
                    onClick = mediaControlAction(context, ACTION_PICK),
                    rippleOverride = R.drawable.glance_button_ripple_middle,
                    backgroundDrawable = R.drawable.glance_button_shape_middle,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_keyboard_arrow_right_24),
                    contentDescription = context.getString(R.string.right),
                    onClick = mediaControlAction(context, ACTION_RIGHT),
                    rippleOverride = R.drawable.glance_button_ripple_right,
                    backgroundDrawable = R.drawable.glance_button_shape_right,
                    backgroundColor = GlanceTheme.colors.primary,
                    contentColor = GlanceTheme.colors.onPrimary,
                    dimensions = dimensions
                )
            }

            Spacer(GlanceModifier.height(BUTTON_SPACES))

            Row(
                modifier = GlanceModifier.wrapContentWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(GlanceModifier.width(dimensions.maxIconSize))

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                GlanceCustomIconButton(
                    imageProvider = ImageProvider(R.drawable.round_keyboard_arrow_down_24),
                    contentDescription = context.getString(R.string.down),
                    onClick = mediaControlAction(context, ACTION_DOWN),
                    rippleOverride = R.drawable.glance_button_ripple_down,
                    backgroundDrawable = R.drawable.glance_button_shape_down,
                    dimensions = dimensions
                )

                Spacer(GlanceModifier.width(BUTTON_SPACES))

                Spacer(GlanceModifier.width(dimensions.maxIconSize))
            }
        }
    }
}
