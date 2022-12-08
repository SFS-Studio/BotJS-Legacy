package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.concurrent.Parker
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraftforge.fml.unsafe.UnsafeHacks
import sun.misc.Unsafe
import java.util.concurrent.locks.LockSupport

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

class TaskFuture (internal val task: TickableTask<*>) {
    var isDone: Boolean = false
        private set
    private var released: Boolean = false
    lateinit var result: Any
        private set
    private lateinit var parker: Parker

    @Synchronized
    @JvmName("#done")
    fun done(result: Any) {
        check(!released) {"Future already finished"}
        released = true
        isDone = true
        this.result = result
        if(this::parker.isInitialized) {
            parker.unpark()
        }
    }

    @JvmName("#join")
    fun<T: Any> join(): PollResult<T> {
        synchronized(this) {
            if (isDone) {
                return PollResult.done(result as T)
            }
            check(!this::parker.isInitialized) { "Future blocks multiple threads! Should only be one!" }
            parker = Parker()
        }
        parker.park()
        return synchronized(this) {
            if (!isDone) {
                PollResult.pending()
            } else PollResult.done(result as T)
        }
    }

    @Synchronized
    @JvmName("#suspend")
    fun suspend() {
        check(!released) {"Future already finished"}
        released = true
        if(this::parker.isInitialized) {
            parker.unpark()
        }
    }
}