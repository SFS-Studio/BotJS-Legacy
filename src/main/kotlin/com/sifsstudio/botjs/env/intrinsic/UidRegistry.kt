package com.sifsstudio.botjs.env.intrinsic

import java.util.*

object UidRegistry {

    private val allUid: MutableSet<UUID> = mutableSetOf()
    fun generate(): UUID {
        var result: UUID
        do {
            result = UUID.randomUUID()
        } while (tryRegister(result))
        return result
    }

    fun dispose(uid: UUID) =
        allUid.remove(uid)

    fun tryRegister(uid: UUID) =
        allUid.add(uid)

}