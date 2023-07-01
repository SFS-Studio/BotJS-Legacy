package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.*
import com.sifsstudio.botjs.env.api.wrapper.BlockSnapshot
import com.sifsstudio.botjs.env.api.wrapper.EntitySnapshot
import com.sifsstudio.botjs.env.task.PollResult
import com.sifsstudio.botjs.env.task.TickableTask
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.phys.AABB
import org.mozilla.javascript.Function

class SensingAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {
    override val id = "sensing"

    @Suppress("unused")
    fun searchEntity(range: Double, predicate: Function): List<EntitySnapshot> =
        SuspensionContext.invokeSuspend { block(EntitySearchTask(range, environment, predicate)) }

    @Suppress("unused")
    fun searchBlock(range: Double, predicate: Function): List<BlockSnapshot> =
        SuspensionContext.invokeSuspend { block(BlockStateSearchTask(range, environment, predicate)) }
}

class BlockStateSearchTask internal constructor(
    private val environment: BotEnv
) : TickableTask<List<BlockSnapshot>> {
    companion object {
        const val ID = "search_block"
    }

    private var range: Double = 0.0
    private lateinit var predicate: Function

    @Suppress("unused")
    constructor(range: Double, environment: BotEnv, predicate: Function) : this(environment) {
        this.range = range
        this.predicate = predicate
    }

    override val id = ID

    override fun tick(): PollResult<List<BlockSnapshot>> {
        check(::predicate.isInitialized)
        val predicate: (BlockSnapshot) -> Boolean = wrapJsFunction(predicate)
        return environment.entity.run {
            BlockPos.betweenClosedStream(AABB.ofSize(position(), range, range, range))
                .map { BlockSnapshot(it, level.getBlockState(it)) }
        }.filter(predicate).toList().let { PollResult.done(it) }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("range", range)
        putString("predicate", environment.addCache(predicate))
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        range = compound.getDouble("range")
        predicate = environment.getCache(compound.getString("predicate")) as Function
    }
}

class EntitySearchTask(
    private val environment: BotEnv,
) : TickableTask<List<EntitySnapshot>> {
    companion object {
        const val ID = "search_entity"
    }

    private lateinit var predicate: Function
    private var range: Double = 0.0

    @Suppress("unused")
    constructor(range: Double, environment: BotEnv, predicate: Function) : this(environment) {
        this.range = range
        this.predicate = predicate
    }

    override val id = ID

    override fun tick(): PollResult<List<EntitySnapshot>> {
        check(::predicate.isInitialized)
        val predicate: (EntitySnapshot) -> Boolean = wrapJsFunction(predicate)
        return environment.entity.level.getEntities(
            environment.entity,
            AABB.ofSize(environment.entity.position(), range, range, range)
        ) { true }.map {
            EntitySnapshot(it)
        }.filter(predicate).let { PollResult.done(it) }
    }

    override fun serialize() = CompoundTag().apply {
        check(::predicate.isInitialized)
        putDouble("range", range)
        putString("predicate", environment.addCache(predicate))
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        range = compound.getDouble("range")
        predicate = environment.getCache(compound.getString("predicate")) as Function
    }
}