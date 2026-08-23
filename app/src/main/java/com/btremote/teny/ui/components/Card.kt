package com.btremote.teny.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.btremote.teny.R
import com.btremote.teny.ui.theme.surfaceElevationMedium
import com.btremote.teny.ui.theme.surfaceElevationShadow

@Composable
fun DefaultElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(dimensionResource(id = R.dimen.card_corner_radius)),
    elevation: Dp = surfaceElevationMedium(),
    shadowElevation: Dp = surfaceElevationShadow(),
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = shadowElevation
        )
    ) {
        content()
    }
}