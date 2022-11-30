package com.sifsstudio.botjs.env

import net.minecraft.nbt.Tag

interface TickableTask<T : Any> {
    val id: String
    fun tick(): FutureResult<T>
    fun serialize(): Tag
    fun deserialize(tag: Tag)
}

class FutureResult<T : Any>(val isDone: Boolean, val result: T?) {
    companion object {
        fun <T : Any> done(result: T) = FutureResult<T>(true, result)

        fun <T : Any> pending() = FutureResult<T>(false, null)
    }
}
