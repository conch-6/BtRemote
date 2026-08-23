package com.atharok.btremote.domain.entities.settings

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CastLink(
    val id: String,
    val url: String
)

@Keep
@Serializable
data class CastDevice(
    val name: String,
    val controlUrl: String
)

@Keep
@Serializable
data class CastSettings(
    val links: List<CastLink> = emptyList(),
    val selectedLinkId: String? = null,
    val savedDevice: CastDevice? = null
)
