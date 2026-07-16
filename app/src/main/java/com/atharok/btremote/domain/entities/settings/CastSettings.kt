package com.atharok.btremote.domain.entities.settings

import kotlinx.serialization.Serializable

@Serializable
data class CastLink(
    val id: String,
    val url: String
)

@Serializable
data class CastDevice(
    val name: String,
    val controlUrl: String
)

@Serializable
data class CastSettings(
    val links: List<CastLink> = emptyList(),
    val selectedLinkId: String? = null,
    val savedDevice: CastDevice? = null
)
