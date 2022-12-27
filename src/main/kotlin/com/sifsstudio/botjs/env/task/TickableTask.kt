package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.EnvInputStream
import com.sifsstudio.botjs.env.EnvOutputStream
import com.sifsstudio.botjs.util.concurrent.Parker
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException

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

        fun done() = PollResult(true, Unit)
    }
}

class TaskFuture<T : Any> internal constructor(): java.io.Serializable {
    var isDone: Boolean = false
        private set
    lateinit var result: T
        private set

    @JvmField
    internal var ordinal = -1

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
        stream.defaultWriteObject()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        check(stream is EnvInputStream)
        stream.defaultReadObject()
        check(ordinal >= -1)
        stream.env.taskHandler.associate(this)
    }

    @Throws(ObjectStreamException::class)
    private fun readObjectNoData() {
        throw NotImplementedError()
    }
}

fun<T: Any> Parker.join(it: TaskFuture<T>): PollResult<T> {
    synchronized(it) {
        if (it.isDone) {
            return PollResult.done(it.result)
        }
    }
    park()
    return synchronized(this) {
        if (!it.isDone) {
            PollResult.pending()
        } else PollResult.done(it.result)
    }
}

fun suspend(parker: Parker) {
    check(parker.parking)
    parker.unpark()
}