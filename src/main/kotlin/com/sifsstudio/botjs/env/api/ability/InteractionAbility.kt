package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.wrapper.EntitySnapshot
import com.sifsstudio.botjs.env.task.PollResult
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.env.task.TickableTask
import com.sifsstudio.botjs.util.extinguishFire
import com.sifsstudio.botjs.util.plus
import com.sifsstudio.botjs.util.times
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.ClipContext
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.eventbus.api.Event
import java.util.*

class InteractionAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {

    override val id = "interaction"

    @Suppress("unused")
    fun breakBlock(x: Double, y: Double, z: Double) =
        submit(BreakBlockTask(BlockPos(x, y, z), environment))

    @Suppress("unused")
    fun attack(entity: EntitySnapshot) =
        submit(AttackTask(entity.id, environment))

    @Suppress("unused")
    fun sweepAttack() =
        submit(AttackTask(null, environment))
}

class AttackTask internal constructor(private val environment: BotEnv) : TickableTask<Boolean> {
    companion object {
        const val ID = "attack"
    }

    private var entity: Int? = null

    constructor(entity: Int?, environment: BotEnv) : this(environment) {
        this.entity = entity
    }

    override val id = ID

    override fun tick(): PollResult<Boolean> {
        val fakePlayer = FakePlayerFactory.getMinecraft(environment.entity.level as ServerLevel)
        fakePlayer.setPos(environment.entity.position())
        return if (entity == null) {
            fakePlayer.sweepAttack()
            PollResult.done(true)
        } else {
            val ent = environment.entity.level.getEntity(entity!!)
            if (ent == null) {
                PollResult.done(false)
            } else {
                fakePlayer.attack(ent)
                PollResult.done(true)
            }
        }
    }

    override fun serialize() = CompoundTag().apply {
        entity?.let { putInt("id", it) }
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        compound.get("id")?.let {
            entity = (it as IntTag).asInt
        }
    }
}

class BreakBlockTask(private var pos: BlockPos, private val environment: BotEnv) : TickableTask<Boolean> {
    companion object {
        const val ID = "break_block"
    }

    @Suppress("unused")
    constructor(environment: BotEnv) : this(BlockPos.ZERO, environment)

    private lateinit var fakePlayer: FakePlayer
    private var breakBlockProgress: Float = 0.0f

    override val id = ID

    override fun tick(): PollResult<Boolean> {
        val level = environment.entity.level as ServerLevel
        if (!this::fakePlayer.isInitialized) {
            fakePlayer = FakePlayerFactory.getMinecraft(level)
        }
        val rayContext = ClipContext(
            environment.entity.eyePosition,
            environment.entity.eyePosition + environment.entity.lookAngle * 6.0,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            fakePlayer
        )
        val hitResult = level.clip(rayContext)
        val blockPos = hitResult.blockPos
        if (!blockPos.equals(pos)) {
            return PollResult.done(false)
        }
        fakePlayer.setPos(environment.entity.position())
        if (!level.mayInteract(fakePlayer, blockPos)) {
            return PollResult.done(false)
        }
        val destroyState = level.getBlockState(blockPos)
        if (destroyState.getShape(level, blockPos).isEmpty) {
            return PollResult.done(false)
        }
        val event = ForgeHooks.onLeftClickBlock(fakePlayer, blockPos, hitResult.direction)
        if (event.isCanceled) {
            return PollResult.done(false)
        }
        if (level.extinguishFire(fakePlayer, blockPos, hitResult.direction)) {
            return PollResult.done(true)
        }
        if (event.useBlock != Event.Result.DENY) {
            destroyState.attack(level, blockPos, fakePlayer)
        }
        var progress = destroyState.getDestroyProgress(fakePlayer, level, blockPos) * 16
        val before = breakBlockProgress
        progress += before
        level.playSound(null, blockPos, destroyState.soundType.hitSound, SoundSource.NEUTRAL, 0.25f, 1.0f)
        if (progress >= 1) {
            fakePlayer.gameMode.destroyBlock(blockPos)
            level.destroyBlockProgress(fakePlayer.id, blockPos, -1)
            breakBlockProgress = 0.0f
            return PollResult.done(true)
        }
        if (progress <= 0) {
            breakBlockProgress = 0.0f
            return PollResult.done(false)
        }
        if ((before * 10).toInt() != (progress * 10).toInt()) {
            level.destroyBlockProgress(fakePlayer.id, blockPos, (progress * 10).toInt())
        }
        breakBlockProgress = progress
        return PollResult.pending()
    }

    override fun serialize(): Tag = CompoundTag().apply {
        putFloat("breakBlockProgress", breakBlockProgress)
        put("position", NbtUtils.writeBlockPos(pos))
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        breakBlockProgress = compound.getFloat("breakBlockProgress")
        pos = NbtUtils.readBlockPos(compound.getCompound("position"))
    }

}
