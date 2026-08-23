package com.atharok.btremote.domain.entity.dlna

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DlnaDevice(
    val name: String,
    val controlUrl: String
)
