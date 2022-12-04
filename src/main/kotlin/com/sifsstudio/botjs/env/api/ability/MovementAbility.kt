package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.PollResult
import com.sifsstudio.botjs.env.TaskFuture
import com.sifsstudio.botjs.env.TickableTask
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.phys.Vec3

class MovementAbility(private val environment: BotEnv) : AbilityBase(environment) {
    override val id = "movement"

    @Suppress("unused")
    fun moveTo(x: Double, y: Double, z: Double): TaskFuture {
        return submit(DestinationMovementTask(x, y, z, environment))
    }

    @Suppress("unused")
    fun lookAt(x: Double, y: Double, z: Double) {
        environment.entity.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3(x, y, z))
        environment.entity.lookControl.setLookAt(x, y, z)
        submit(SleepTask(2))
    }
}

class DestinationMovementTask(
    private var x: Double,
    private var y: Double,
    private var z: Double,
    private val environment: BotEnv
) :
    TickableTask<Boolean> {
    companion object {
        const val ID = "destination_movement"
        const val MOVE_SPEED = 0.5
    }

    @Suppress("unused")
    constructor(environment: BotEnv) : this(0.0, 0.0, 0.0, environment)

    override val id = ID

    override fun tick(): PollResult<Boolean> {
        // FIXME
        if (environment.entity.position().distanceToSqr(x, y, z) <= 0.5) {
            environment.entity.navigation.stop()
            return PollResult.done(true)
        }
        val nav = environment.entity.navigation
        if (!nav.moveTo(x, y, z, MOVE_SPEED) || nav.isStuck) {
            return PollResult.done(false)
        }
        return if (nav.isDone) {
            environment.entity.navigation.stop()
            PollResult.done(true)
        } else {
            PollResult.pending()
        }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("x", x)
        putDouble("y", y)
        putDouble("z", z)
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        x = compound.getDouble("x")
        y = compound.getDouble("y")
        z = compound.getDouble("z")
    }
}
