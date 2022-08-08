package com.sifsstudio.botjs.env.ability

import com.sifsstudio.botjs.env.task.TaskBase
import com.sifsstudio.botjs.env.task.TaskFuture

class TimingAbility: AbilityBase() {

    override val id = "timing"

    @Suppress("unused")
    fun delay(ticks: Int) {
        check(ticks >= 0)
        if(ticks == 0) return
        env.pending(WaitTask(ticks))
            .join()
    }

    @Suppress("unused")
    fun delayAsync(ticks: Int): TaskFuture<Unit> {
        check(ticks >= 0)
        return if(ticks == 0) TaskFuture.successFuture(Unit)
        else TaskFuture.successUnitFuture
    }

    companion object {
        class WaitTask(private var ticks: Int) : TaskBase<Unit>() {
            override fun tick() {
                if(ticks-- == 0) {
                    done(Unit)
                }
            }
        }
    }
}