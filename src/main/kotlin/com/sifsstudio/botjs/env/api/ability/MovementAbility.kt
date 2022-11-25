package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.FutureResult
import com.sifsstudio.botjs.env.TickableTask
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.phys.Vec3

class MovementAbility(private val environment: BotEnv) : AbilityBase(environment) {
    override val id = "movement"

    @Suppress("unused")
    fun moveTo(x: Double, z: Double) {
        setPendingTaskAndWait(DestinationMovementTask(x, z, environment))
    }
}

class DestinationMovementTask(private var x: Double, private var z: Double, private val environment: BotEnv) :
    TickableTask {
    companion object {
        const val ID = "destination_movement"
        const val moveSpeed = 0.1
        const val moveSpeedSq = moveSpeed * moveSpeed
    }

    @Suppress("unused")
    constructor(environment: BotEnv) : this(0.0, 0.0, environment)

    override val id = ID

    override fun tick(): FutureResult {
        val normal = Vec3(x - environment.entity.x, 0.0, z - environment.entity.z).normalize()
        val distanceSq = environment.entity.distanceToSqr(x, environment.entity.y, z)
        val movementSq = if (distanceSq >= moveSpeedSq) {
            environment.entity.deltaMovement = normal.scale(moveSpeed)
            moveSpeedSq
        } else {
            environment.entity.deltaMovement = normal.scale(kotlin.math.sqrt(distanceSq))
            distanceSq
        }
        return if (distanceSq - movementSq < 1E-14) {
            FutureResult.DONE
        } else {
            FutureResult.PENDING
        }
    }

    override fun serialize() = CompoundTag().apply {
        putDouble("x", x)
        putDouble("z", z)
    }

    override fun deserialize(tag: Tag) {
        val compound = tag as CompoundTag
        x = compound.getDouble("x")
        z = compound.getDouble("z")
    }
}
