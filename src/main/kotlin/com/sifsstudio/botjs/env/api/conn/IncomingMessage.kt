package com.sifsstudio.botjs.env.api.conn

@JvmRecord
data class IncomingMessage internal constructor(val sender: Remote, val message: String)