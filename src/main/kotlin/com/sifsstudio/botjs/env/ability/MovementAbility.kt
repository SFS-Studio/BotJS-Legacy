package com.sifsstudio.botjs.env.ability

import com.sifsstudio.botjs.env.task.TaskBase
import com.sifsstudio.botjs.env.task.TaskFuture
import net.minecraft.core.Direction
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3

class MovementAbility : AbilityBase() {

    override val id = "movement"

    @Suppress("unused")
    fun moveTo(x: Double, z: Double) = env.pending(MovementTask(x, z)).join()

    @Suppress("unused")
    fun moveToAsync(x: Double, z: Double) = env.pending(MovementTask(x, z))

    @Suppress("unused")
    fun move(direction: Direction, distance: Double): MoveResult {
        check(distance >= 0)
        return if(distance > 1E-7) env.pending(DirectionalMovementTask(direction, distance)).join()
                else successResult
    }

    @Suppress("unused")
    fun moveAsync(direction: Direction, distance: Double): TaskFuture<MoveResult> {
        check(distance >= 0)
        return if(distance > 1E-7) env.pending(DirectionalMovementTask(direction, distance))
                else TaskFuture.successFuture(successResult)
    }

    companion object {

        const val moveSpeed = 0.1
        val successResult = MoveResult(true)

        class MovementTask(private val endX: Double, private val endZ: Double): TaskBase<MoveResult>() {
            override fun tick() {
                val normal = Vec3(endX - env.entity.x, 0.0, endZ - env.entity.z).normalize()
                val distance = env.entity.distanceToSqr(endX, env.entity.y, endZ)
                val movement = distance.coerceAtMost(moveSpeed)
                env.entity.deltaMovement = normal.scale(movement)
                if(distance - movement < 1E-7) {
                    done(successResult)
                }
            }
        }

        class DirectionalMovementTask(direction: Direction, private var distance: Double): TaskBase<MoveResult>() {
            private val normal = Vec3(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())
            override fun tick() {
                val movement = distance.coerceAtMost(moveSpeed)
                env.entity.deltaMovement = normal.scale(movement)
                distance -= movement
                if(distance < 1E-7) {
                    done(successResult)
                }
            }
        }

        data class MoveResult(val success: Boolean, val remaining: Double = 0.0)
    }
}
