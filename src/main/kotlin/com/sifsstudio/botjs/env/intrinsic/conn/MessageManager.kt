package com.sifsstudio.botjs.env.intrinsic.conn

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import com.sifsstudio.botjs.env.api.conn.IncomingMessage
import com.sifsstudio.botjs.env.api.conn.Remote

object MessageManager {
    private val messages: SetMultimap<Remote, IncomingMessage> =
        MultimapBuilder.hashKeys().linkedHashSetValues().build()

    fun send(sender: Remote, receiver: Remote, message: String) {
        messages.put(receiver, IncomingMessage(sender.uid, message))
    }

    fun poll(receiver: Remote): Set<IncomingMessage> = messages.removeAll(receiver)
}