package com.sifsstudio.botjs.env.api.wrapper

import net.minecraft.world.entity.Entity

@Suppress("unused")
class WrappedEntity(internal val entity: Entity) {
    val x
        get() = entity.x
    val y
        get() = entity.y
    val z
        get() = entity.z
    val type
        get() = entity.type.registryName.toString()
}