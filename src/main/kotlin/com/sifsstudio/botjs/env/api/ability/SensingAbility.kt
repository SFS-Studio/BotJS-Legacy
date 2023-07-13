package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.SuspensionContext
import com.sifsstudio.botjs.env.api.wrapper.BlockSnapshot
import com.sifsstudio.botjs.env.api.wrapper.EntitySnapshot
import com.sifsstudio.botjs.env.task.PollResult
import com.sifsstudio.botjs.env.task.TickableTask
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.phys.AABB

class SensingAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {
    override val id = "sensing"

    @Suppress("unused")
    fun searchEntity(range: Double): List<EntitySnapshot> =
        SuspensionContext.invokeSuspend { it.block(EntitySearchTask(range, environment)) }

    @Suppress("unused")
    fun searchBlock(range: Double): List<BlockSnapshot> =
        SuspensionContext.invokeSuspend { it.block(BlockStateSearchTask(range, environment)) }
}

class BlockStateSearchTask internal constructor(
    private val environment: BotEnv
) : TickableTask<List<BlockSnapshot>> {
    companion object {
        const val ID = "search_block"
    }

    private var range: Double = 0.0

    @Suppress("unused")
    constructor(range: Double, environment: BotEnv) : this(environment) {
        this.range = range
    }

    override val id = ID

    override fun tick(): PollResult<List<BlockSnapshot>> {
        return environment.entity.run {
            BlockPos.betweenClosedStream(AABB.ofSize(position(), range, range, range))
                .map { BlockSnapshot(it, level.getBlockState(it)) }
        }.toList().let { PollResult.done(it) }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("range", range)
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        range = compound.getDouble("range")
    }
}

class EntitySearchTask(
    private val environment: BotEnv,
) : TickableTask<List<EntitySnapshot>> {
    companion object {
        const val ID = "search_entity"
    }

    private var range: Double = 0.0

    @Suppress("unused")
    constructor(range: Double, environment: BotEnv) : this(environment) {
        this.range = range
    }

    override val id = ID

    override fun tick(): PollResult<List<EntitySnapshot>> {
        return environment.entity.level.getEntities(
            environment.entity,
            AABB.ofSize(environment.entity.position(), range, range, range)
        ) { true }.map {
            EntitySnapshot(it)
        }.let { PollResult.done(it) }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("range", range)
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        range = compound.getDouble("range")
    }
}