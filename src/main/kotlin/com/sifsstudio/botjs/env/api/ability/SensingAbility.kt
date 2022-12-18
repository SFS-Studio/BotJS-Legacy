package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.wrapper.WrappedEntity
import net.minecraft.world.phys.AABB

class SensingAbility(private val environment: BotEnv) : AbilityBase(environment) {
    override val id = "sensing"

    @Suppress("unused")
    fun searchEntity(predicate: (WrappedEntity) -> Boolean, range: Double): List<WrappedEntity> =
        // FIXME: return non-null when suspended
        environment.entity.level.getEntities(
            environment.entity,
            AABB.ofSize(environment.entity.position(), range, range, range)
        ) {
            predicate(WrappedEntity(it))
        }.map {
            WrappedEntity(it)
        }.also { suspendIfNecessary(null) }
}