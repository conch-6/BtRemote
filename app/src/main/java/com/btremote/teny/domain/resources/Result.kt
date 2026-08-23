package com.btremote.teny.domain.resources

data class Result<T> (
    val value: T,
    val message: String
)