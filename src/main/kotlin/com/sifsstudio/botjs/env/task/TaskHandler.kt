package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.SuspensionContext
import com.sifsstudio.botjs.env.switchAware
import com.sifsstudio.botjs.util.getList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unchecked_cast")
class TaskHandler(private val env: BotEnv) {
    private val tickingTasks: MutableList<TaskHandle<*>> = CopyOnWriteArrayList()
    private var pendingTask: TaskHandle<*>? = null
    private val parker: Parker = Parker()
    private var suspended: AtomicBoolean = AtomicBoolean(false)

    fun <T : Any> associate(future: TaskFuture<T>, ordinal: Int) {
        check(ordinal in -1..tickingTasks.size)
        val handle = if (ordinal == -1) {
            val pT = pendingTask
            check(pT != null)
            pT
        } else {
            tickingTasks[ordinal]
        } as TaskHandle<T>
        check(!handle.hasFuture) { "The task to associate already owned a future! This should not be possible." }
        handle.future = future
    }

    fun ordinal(future: TaskFuture<*>): Int = tickingTasks.indexOfFirst { it.hasFuture && it.future == future }

    suspend fun resume(): Any? {
        val pendingTask = pendingTask ?: return null
        val future = pendingTask.future
        future.join(parker)
        return if (!future.isDone) {
            null
        } else future.result
    }

    fun tick() {
        if (!env.controller.runState.get().tickable || suspended.get()) {
            return
        }
        tickingTasks.removeIf {
            val result = it.task.tick()
            if (result.isDone) {
                if (it.hasFuture) {
                    it.future.done(result.result!!)
                }
                true
            } else false
        }
        val pending = pendingTask
        if (pending != null) {
            val result = pending.task.tick()
            if (result.isDone) {
                this.pendingTask = null
                pending.future.done(result.result!!)
                parker.unpark()
            }
        }
    }

    fun suspend(terminate: Boolean) {
        suspended.set(true)
        if (terminate) {
            parker.interrupt()
        } else {
            parker.unpark()
        }
    }

    fun pause() {
        suspended.set(false)
    }

    fun resumeExecution() {
        suspended.set(false)
    }

    private fun <T : Any> findTask(future: TaskFuture<T>): TaskHandle<T>? {
        val pT = pendingTask
        return if (pT != null && pT.future == future) {
            pT as TaskHandle<T>
        } else {
            tickingTasks.firstOrNull { it.future == future } as TaskHandle<T>?
        }
    }

    @Synchronized
    fun reset() {
        pendingTask = null
        tickingTasks.clear()
        suspended.set(false)
    }

    fun <T : Any> submit(task: TickableTask<T>): TaskFuture<T> {
        val future = TaskFuture<T>()
        synchronized(this) {
            tickingTasks.add(TaskHandle(task, future))
        }
        return future
    }

    suspend fun <T : Any> block(future: TaskFuture<T>, cx: SuspensionContext) {
        synchronized(this) {
            tickingTasks.iterator().run {
                while (hasNext()) {
                    val now = next()
                    if (now.future == future) {
                        pendingTask = now
                        remove()
                        break
                    }
                }
            }
        }
        if (!suspended.get()) {
            cx.switchAware {
                future.join(parker)
            }
        }
    }

    @Synchronized
    fun serialize() = CompoundTag().apply {
        pendingTask?.let {
            TickableTask.serialize(it.task)
        }?.let { this@apply.put("pendingTask", it) }
        val others = ListTag()
        tickingTasks.forEach {
            others.add(TickableTask.serialize(it.task))
        }
        put("tickingTasks", others)
    }

    @Synchronized
    fun deserialize(compound: CompoundTag) {
        pendingTask = TickableTask.deserialize(compound.getCompound("pendingTask"), env)
            ?.let { TaskHandle(it) }
        if (pendingTask != null) {
            associate(TaskFuture(), -1)
        }
        tickingTasks.clear()
        val others = compound.getList("tickingTasks", Tag.TAG_COMPOUND)
        others.forEach {
            tickingTasks.add(TaskHandle(TickableTask.deserialize(it as CompoundTag, env)!!))
        }
    }
}

class TaskHandle<T : Any>(val task: TickableTask<T>) {
    lateinit var future: TaskFuture<T>

    constructor(task: TickableTask<T>, future: TaskFuture<T>) : this(task) {
        this.future = future
    }

    val hasFuture
        get() = ::future.isInitialized
}