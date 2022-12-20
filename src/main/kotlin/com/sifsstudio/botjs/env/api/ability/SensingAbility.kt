package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.*
import com.sifsstudio.botjs.env.api.wrapper.WrappedBlockState
import com.sifsstudio.botjs.env.api.wrapper.WrappedEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.phys.AABB
import org.mozilla.javascript.Function

class SensingAbility(private val environment: BotEnv) : AbilityBase(environment) {
    override val id = "sensing"

    @Suppress("unused")
    fun searchEntity(range: Double, predicate: Function): List<WrappedEntity> =
        block(EntitySearchTask(range, environment, predicate))

    @Suppress("unused")
    fun searchBlock(range: Double, predicate: Function): List<WrappedBlockState> =
        block(BlockStateSearchTask(range, environment, predicate))
}

class BlockStateSearchTask(
    private var range: Double,
    private var environment: BotEnv,
    private var predicate: Function?
) : TickableTask<List<WrappedBlockState>> {
    companion object {
        const val ID = "search_block_entity"
    }

    @Suppress("unused")
    constructor(environment: BotEnv) : this(0.0, environment, null)
    override val id = ID

    override fun tick(): PollResult<List<WrappedBlockState>> {
        val predicator: (WrappedBlockState) -> Boolean = wrapJsFunction(predicate!!)
        return environment.entity.run {
            BlockPos.betweenClosedStream(AABB.ofSize(position(), range, range, range))
                .map { WrappedBlockState(it, level.getBlockState(it)) }
        }.filter(predicator).toList().let{ PollResult.done(it) }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("range", range)
        putString("predicate", environment.addCache(predicate!!))
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        range = compound.getDouble("range")
        predicate = environment.getCache(compound.getString("predicate")) as Function
    }
}

class EntitySearchTask(
    private var range: Double,
    private val environment: BotEnv,
    private var predicate: Function?
) : TickableTask<List<WrappedEntity>> {
    companion object {
        const val ID = "search_entity"
    }

    @Suppress("unused")
    constructor(environment: BotEnv) : this(0.0, environment, null)
    override val id = ID

    override fun tick(): PollResult<List<WrappedEntity>> {
        val predicator: (WrappedEntity) -> Boolean = wrapJsFunction(predicate!!)
        return environment.entity.level.getEntities(
            environment.entity,
            AABB.ofSize(environment.entity.position(), range, range, range)
        ) {
            predicator(WrappedEntity(it))
        }.map {
            WrappedEntity(it)
        }.let { PollResult.done(it) }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("range", range)
        putString("predicate", environment.addCache(predicate!!))
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        range = compound.getDouble("range")
        predicate = environment.getCache(compound.getString("predicate")) as Function
    }
}