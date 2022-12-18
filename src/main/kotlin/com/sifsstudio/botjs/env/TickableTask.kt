package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.concurrent.Parker
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag

interface TickableTask<T : Any> {
    val id: String

    fun tick(): PollResult<T>
    fun serialize(): Tag
    fun deserialize(tag: Tag)

    companion object {
        fun serialize(task: TickableTask<*>) = CompoundTag().apply {
            putString("id", task.id)
            put("data", task.serialize())
        }

        fun deserialize(compound: CompoundTag, environment: BotEnv): TickableTask<*>? {
            return if (!compound.isEmpty) {
                val task = TaskRegistry.constructTask(compound.getString("id"), environment)!!
                task.deserialize(compound.get("task")!!)
                task
            } else null
        }
    }
}

class PollResult<T : Any>(val isDone: Boolean, val result: T?) {
    companion object {
        fun <T : Any> done(result: T) = PollResult(true, result)

        fun <T : Any> pending() = PollResult<T>(false, null)
    }
}

class TaskFuture<T : Any>(internal val task: TickableTask<T>) {
    private var isDone: Boolean = false
    private var released: Boolean = false
    private lateinit var result: T
    private lateinit var parker: Parker

    @Synchronized
    internal fun done(result: Any) {
        check(!released) { "Future already finished" }
        released = true
        isDone = true
        @Suppress("UNCHECKED_CAST")
        this.result = result as T
        if (this::parker.isInitialized) {
            parker.unpark()
        }
    }

    internal fun join(): PollResult<T> {
        synchronized(this) {
            if (isDone) {
                return PollResult.done(result)
            }
            check(!this::parker.isInitialized) { "Future blocks multiple threads! Should only be one!" }
            parker = Parker()
        }
        parker.park()
        return synchronized(this) {
            if (!isDone) {
                PollResult.pending()
            } else PollResult.done(result)
        }
    }

    @Synchronized
    internal fun suspend() {
        check(!released) { "Future already finished" }
        released = true
        if (this::parker.isInitialized) {
            parker.unpark()
        }
    }
}