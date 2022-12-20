package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.api.ability.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

object TaskRegistry {

    private val TASK_REGISTRY: Map<String, KClass<out TickableTask<*>>> = mapOf(
        Pair(SleepTask.ID, SleepTask::class),
        Pair(SpeakTask.ID, SpeakTask::class),
        Pair(DestinationMovementTask.ID, DestinationMovementTask::class),
        Pair(BreakBlockTask.ID, BreakBlockTask::class),
        Pair(LookAtTask.ID, LookAtTask::class),
        Pair(EntitySearchTask.ID, EntitySearchTask::class),
        Pair(BlockStateSearchTask.ID, BlockStateSearchTask::class)
    )

    fun constructTask(id: String, environment: BotEnv) = TASK_REGISTRY[id]?.constructors?.first {
        it.parameters.size == 1 && it.parameters[0].type == BotEnv::class.createType()
    }?.call(environment)

}