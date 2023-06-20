package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.EnvInputStream
import com.sifsstudio.botjs.env.EnvOutputStream
import com.sifsstudio.botjs.env.Parker
import kotlinx.coroutines.Job
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException
import kotlin.coroutines.coroutineContext

interface TickableTask<out T : Any> {
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
                task.deserialize(compound.get("data")!!)
                task
            } else null
        }
    }
}

class PollResult<out T : Any>(val isDone: Boolean, val result: T?) {
    companion object {
        fun <T : Any> done(result: T): PollResult<T> = PollResult(true, result)

        fun <T : Any> pending() = PollResult<T>(false, null)

        fun done() = PollResult(true, Unit)
    }
}

class TaskFuture<out T : Any> internal constructor() : java.io.Serializable {
    var isDone: Boolean = false
        private set

    private lateinit var result: T

    @JvmField
    internal var ordinal = -1

    fun getResult(): T {
        return result
    }

    @Synchronized
    internal fun done(result: Any) {
        check(!isDone) { "Future already done" }
        isDone = true
        @Suppress("UNCHECKED_CAST")
        this.result = result as T
    }

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        check(stream is EnvOutputStream)
        check(ordinal >= 0)
        stream.writeBoolean(isDone)
        if (::result.isInitialized) {
            stream.writeBoolean(true)
            stream.writeObject(result)
        } else {
            stream.writeBoolean(false)
        }
        stream.writeInt(ordinal)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        check(stream is EnvInputStream)
        isDone = stream.readBoolean()
        if (stream.readBoolean()) {
            result = stream.readObject() as T
        }
        ordinal = stream.readInt()
        check(ordinal >= -1)
        stream.env.taskHandler.associate(this)
    }

    @Throws(ObjectStreamException::class)
    private fun readObjectNoData() {
        throw UnsupportedOperationException()
    }

    internal suspend fun join(it: Parker): PollResult<T> {
        synchronized(it) {
            if (isDone) {
                return PollResult.done(result)
            }
        }
        coroutineContext[Job]?.invokeOnCompletion { t ->
            if(t is InterruptedException) {
                it.interrupt()
            }
        }
        it.park()
        return synchronized(this) {
            if (!isDone) {
                PollResult.pending()
            } else PollResult.done(result)
        }
    }
}