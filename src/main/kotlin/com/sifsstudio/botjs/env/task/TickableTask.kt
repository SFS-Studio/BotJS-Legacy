package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.SuspensionContext
import com.sifsstudio.botjs.env.save.EnvInputStream
import com.sifsstudio.botjs.env.save.EnvOutputStream
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException

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

class TaskFuture<T : Any> internal constructor() : java.io.Serializable {
    var isDone: Boolean = false
        private set

    lateinit var result: T
        private set

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
        stream.writeBoolean(stream.simpleFuture)
        if (stream.simpleFuture) {
            return
        }
        val ordinal = stream.env.taskHandler.ordinal(this)
        if (ordinal == -1) {
            check(isDone)
        }
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
        if (stream.readBoolean()) {
            return
        }
        isDone = stream.readBoolean()
        if (stream.readBoolean()) {
            @Suppress("UNCHECKED_CAST")
            result = stream.readObject() as T
        }
        val ordinal = stream.readInt()
        if (ordinal >= 0) {
            stream.env.taskHandler.associate(this, ordinal)
        }
    }

    @Throws(ObjectStreamException::class)
    private fun readObjectNoData() {
    }

    internal suspend fun join(it: Parker, cx: SuspensionContext) {
        if (isDone) {
            return
        }
        it.park(cx)
    }
}
