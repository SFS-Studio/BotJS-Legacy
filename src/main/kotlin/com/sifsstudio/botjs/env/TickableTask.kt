package com.sifsstudio.botjs.env

import net.minecraft.nbt.Tag

interface TickableTask {
    val id: String
    fun tick(): FutureResult
    fun serialize(): Tag
    fun deserialize(tag: Tag)
}

enum class FutureResult {
    DONE,
    PENDING,
}
