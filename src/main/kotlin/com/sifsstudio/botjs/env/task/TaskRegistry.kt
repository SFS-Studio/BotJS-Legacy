package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.ability.*

object TaskRegistry {

    private val DELEGATE: Map<String, (BotEnv) -> TickableTask<*>> = mapOf(
        Pair(SleepTask.ID, ::SleepTask),
        Pair(SpeakTask.ID, ::SpeakTask),
        Pair(DestinationMovementTask.ID, ::DestinationMovementTask),
        Pair(BreakBlockTask.ID, ::BreakBlockTask),
        Pair(LookAtTask.ID, ::LookAtTask),
        Pair(EntitySearchTask.ID, ::EntitySearchTask),
        Pair(BlockStateSearchTask.ID, ::BlockStateSearchTask),
    )

    fun constructTask(id: String, environment: BotEnv) = DELEGATE[id]?.let { it(environment) }

}