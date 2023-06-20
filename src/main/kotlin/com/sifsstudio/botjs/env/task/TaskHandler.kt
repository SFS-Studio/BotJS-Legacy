package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.Parker
import com.sifsstudio.botjs.util.getList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag

@Suppress("unchecked_cast")
class TaskHandler(private val env: BotEnv) {
    private val tickingTasks: MutableList<TaskHandle<*>> = mutableListOf()
    private var pendingTask: Pair<TaskHandle<*>, Boolean>? = null
    private val parker: Parker = Parker()
    private var tickable: Boolean = false

    fun<T: Any> associate(future: TaskFuture<T>) {
        check(tickingTasks.isNotEmpty())
        check(future.ordinal in -1..tickingTasks.size)
        val handle = if(future.ordinal == -1) {
            check(pendingTask != null)
            pendingTask!!.first
        } else {
            tickingTasks[future.ordinal]
        } as TaskHandle<T>
        check(!handle.hasFuture) {"The task to associate already owned a future! This should not be possible."}
        handle.future = future
    }

    suspend fun resume(): Any? {
        check(pendingTask?.second != null)
        tickable = true
        val pendingTask = pendingTask
        return if (pendingTask != null && !pendingTask.second) {
            val res = pendingTask.first.future.join(parker)
            if (!res.isDone) {
                Unit
            } else res.result
        } else pendingTask?.first
    }

    @Synchronized
    fun tick() {
        if(!tickable) {
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
        val pendingFuture = pendingTask
        if (pendingFuture != null && !pendingFuture.second) {
            val result = pendingFuture.first.task.tick()
            if (result.isDone) {
                this.pendingTask = null
                pendingFuture.first.future.done(result.result!!)
                check(parker.parking)
                parker.unpark()
            }
        }
    }

    @Synchronized
    fun reset() {
        if (pendingTask != null) {
            if (parker.parking) {
                parker.interrupt()
            }
            pendingTask = null
        }
        tickingTasks.clear()
    }

    @Synchronized
    private fun<T: Any> findTask(future: TaskFuture<T>) : TaskHandle<T>? {
        if (pendingTask != null && pendingTask!!.first.future == future) {
            return pendingTask!!.first as TaskHandle<T>
        } else {
            for (it in tickingTasks) {
                if (it.future == future) {
                    return it as TaskHandle<T>
                }
            }
            return null
        }
    }

    fun <T : Any> submit(task: TickableTask<T>): TaskFuture<T> {
        val future = TaskFuture<T>()
        synchronized(this) {
            tickingTasks.add(TaskHandle(task, future))
        }
        return future
    }

    suspend fun <T : Any> block(future: TaskFuture<T>): PollResult<T> {
        synchronized(this) {
            tickingTasks.iterator().run {
                while (hasNext()) {
                    val now = next()
                    if (now.future == future) {
                        pendingTask = Pair(now, true)
                        remove()
                        break
                    }
                }
            }
        }
        return future.join(parker)
    }

    fun storeReturn(future: TaskFuture<*>) {
        check(pendingTask == null)
        pendingTask = Pair(checkNotNull(findTask(future)), false)
    }

    @Synchronized
    fun serialize() = CompoundTag().apply {
        pendingTask?.let { putBoolean("pendingTaskSuspended", it.second)
            TickableTask.serialize(it.first.task) }?.let { this@apply.put("pendingTask", it) }
        val others = ListTag()
        tickingTasks.forEach {
            others.add(TickableTask.serialize(it.task))
        }
        put("tickingTasks", others)
    }

    @Synchronized
    fun deserialize(compound: CompoundTag) {
        pendingTask = TickableTask.deserialize(compound.getCompound("pendingTask"), env)?.let { Pair(TaskHandle(it), false) }
        val others = compound.getList("tickingTasks", Tag.TAG_COMPOUND)
        others.forEach {
            TickableTask.deserialize(it as CompoundTag, env)!!.let { it2 -> tickingTasks.add(TaskHandle(it2)) }
        }
    }
}

class TaskHandle<T: Any>(val task: TickableTask<T>) {
    lateinit var future: TaskFuture<T>
    constructor(task: TickableTask<T>, future: TaskFuture<T>) : this(task) {
        this.future = future
    }

    val hasFuture by ::future::isInitialized
}