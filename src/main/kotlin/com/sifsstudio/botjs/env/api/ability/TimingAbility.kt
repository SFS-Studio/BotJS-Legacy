package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.FutureResult
import com.sifsstudio.botjs.env.TickableTask
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag

class TimingAbility(environment: BotEnv) : AbilityBase(environment) {
    override val id = "timing"

    @Suppress("unused")
    fun sleep(ticks: Int) {
        if (ticks >= 1) {
            setPendingTaskAndWait(SleepTask(ticks))
        } else {
            return
        }
    }
}

class SleepTask(private var ticks: Int) : TickableTask<Unit> {

    @Suppress("unused", "UNUSED_PARAMETER")
    constructor(environment: BotEnv) : this(0)

    companion object {
        const val ID = "sleep"
    }

    override val id = ID

    override fun tick(): FutureResult<Unit> {
        ticks--
        return if (ticks <= 0) {
            FutureResult.done(Unit)
        } else {
            FutureResult.pending()
        }
    }

    override fun serialize() = CompoundTag().apply {
        putInt("ticks", ticks)
    }

    override fun deserialize(tag: Tag) {
        ticks = (tag as CompoundTag).getInt("ticks")
    }

}