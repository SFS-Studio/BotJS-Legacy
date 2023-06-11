package com.sifsstudio.botjs.env.api.conn

import java.util.*

@JvmRecord
data class IncomingMessage internal constructor(val sender: UUID, val message: String)