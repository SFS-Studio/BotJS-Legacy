package com.sifsstudio.botjs.env.api.wrapper

import net.minecraft.world.entity.Entity
import net.minecraftforge.registries.ForgeRegistries
import java.io.Serializable

@Suppress("unused")
class EntitySnapshot(entity: Entity) : Serializable {

    @JvmField
    val x = entity.x

    @JvmField
    val y = entity.y

    @JvmField
    val z = entity.z

    @JvmField
    val type = ForgeRegistries.ENTITY_TYPES.getKey(entity.type)

    @JvmField
    val uuid: String = entity.stringUUID

    @JvmField
    val id = entity.id
}