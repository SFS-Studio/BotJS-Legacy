package com.sifsstudio.botjs.env.ability

import com.sifsstudio.botjs.env.task.TaskBase

class WaitAbility: AbilityBase() {

    override val id = "wait"

    @Suppress("unused")
    fun waitFor(ticks: Int) {
        check(ticks >= 0)
        if(ticks == 0) return
        env.pending(WaitTask(ticks))
            .join()
    }

    companion object {
        class WaitTask(var ticks: Int) : TaskBase<Unit>() {
            override fun tick() {
                if(ticks-- == 0) {
                    done(Unit)
                }
            }
        }
    }
}