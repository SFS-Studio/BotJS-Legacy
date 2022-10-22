package com.sifsstudio.botjs.env.api.storage

import com.sifsstudio.botjs.env.api.storage.Failure

enum class Failures(private val reason: String): Failure {
    FILE_OCCUPIED("file_occupied"),
    ACCESS_DENIED("access_denied"),
    FILE_NOT_FOUND("file_not_found");

    override fun getReason() = reason
}