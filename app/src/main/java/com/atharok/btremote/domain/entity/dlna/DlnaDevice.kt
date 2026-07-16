package com.atharok.btremote.domain.entity.dlna

import kotlinx.serialization.Serializable

@Serializable
data class DlnaDevice(
    val name: String,
    val controlUrl: String
)
