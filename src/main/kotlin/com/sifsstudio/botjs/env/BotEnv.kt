package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.apache.commons.codec.binary.Base64
import org.mozilla.javascript.*
import org.mozilla.javascript.serialize.ScriptableInputStream
import org.mozilla.javascript.serialize.ScriptableOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BotEnv(val entity: BotEntity) : Runnable {

    var script = ""
    var running = false
        private set
    var ticking = false
    private val tickingTasks: MutableSet<TaskFuture> = mutableSetOf()
    private var pendingTask: TaskFuture? = null
    var lastPendingTaskResult: Any? = null
    private lateinit var scope: ScriptableObject
    private val abilities: MutableMap<String, AbilityBase> = mutableMapOf()
    var serializedFrame = ""

    fun install(ability: AbilityBase) = abilities.put(ability.id, ability)
    fun clearAbility() = abilities.clear()

    override fun run() {
        val context = Context.enter()
        context.optimizationLevel = -1
        scope = context.initStandardObjects().apply {
            defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
        }
        if (serializedFrame.isEmpty()) {
            try {
                val botScript = context.compileString(script, "Bot Code", 0, null)
                running = true
                context.executeScriptWithContinuations(botScript, scope)
            } catch (pending: ContinuationPending) {
                val baos = ByteArrayOutputStream()
                val sos = ScriptableOutputStream(baos, context.initStandardObjects())
                sos.writeObject(pending.continuation)
                sos.writeObject(scope)
                serializedFrame = Base64.encodeBase64String(baos.toByteArray())
            } catch (exception: Exception) {
                if (exception is WrappedException && exception.wrappedException is InterruptedException) {
                    // DO NOTHING
                } else {
                    exception.printStackTrace()
                }
                synchronized(this) { pendingTask = null }
            } finally {
                running = false
                Context.exit()
            }
        } else {
            val bais = ByteArrayInputStream(Base64.decodeBase64(serializedFrame))
            val sis = ScriptableInputStream(bais, scope)
            val continuation = sis.readObject()
            scope = (sis.readObject() as ScriptableObject).apply {
                defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
            }
            serializedFrame = try {
                synchronized(this) {
                    val pendingTask = pendingTask
                    if (pendingTask != null) {
                        running = true
                        if (!blockNoCapture(pendingTask)) {
                            toSerializedFrame(context, continuation)
                            return
                        }
                    }
                }
                context.resumeContinuation(continuation, scope, lastPendingTaskResult)
                ""
            } catch (pending: ContinuationPending) {
                toSerializedFrame(context, pending.continuation)
            } catch (exception: Exception) {
                if ((exception is WrappedException && exception.wrappedException is InterruptedException) || exception is InterruptedException) {
                    // DO NOTHING
                } else {
                    exception.printStackTrace()
                }
                synchronized(this) { this.pendingTask = null }
                ""
            } finally {
                running = false
                Context.exit()
            }
        }
    }

    @Synchronized
    fun tick() {
        ticking = true
        tickingTasks.iterator().run {
            while (hasNext()) {
                val now = next()
                val result = now.task.tick()
                if (result.isDone) {
                    now.done(result.result!!)
                    remove()
                }
            }
        }
        if (pendingTask != null) {
            check(pendingTask != null)
            val result = pendingTask!!.task.tick()
            if (result.isDone) {
                pendingTask!!.done(result.result!!)
                pendingTask = null
            }
        }
    }

    @Synchronized
    fun remove() {
        if (pendingTask != null) {
            pendingTask!!.suspend()
        }
    }

    fun removeBotFromScope() {
        scope.delete("bot")
    }

    @Synchronized
    fun <T : Any> submit(task: TickableTask<T>): TaskFuture {
        val result = TaskFuture(task)
        tickingTasks.add(result)
        if(!ticking) {
            suspendExecution()
        }
        return result
    }

    @Synchronized
    fun <T : Any> block(future: TaskFuture): T {
        check(tickingTasks.remove(future))
        pendingTask = future
        val res = future.join<T>()
        if (!res.isDone) {
            suspendExecution()
        }
        return res.result!!
    }

    @Synchronized
    private fun blockNoCapture(future: TaskFuture): Boolean {
        pendingTask = future
        val pResult = future.join<Any>()
        return pResult.isDone
    }

    @Synchronized
    private fun suspendExecution() {
        removeBotFromScope()
        val cx = Context.getCurrentContext()
        val pending = cx.captureContinuation()
        throw pending
    }

    @Synchronized
    private fun toSerializedFrame(context: Context, continuation: Any): String {
        val baos = ByteArrayOutputStream()
        val sos = ScriptableOutputStream(baos, context.initStandardObjects())
        sos.writeObject(continuation)
        sos.writeObject(scope)
        return Base64.encodeBase64String(baos.toByteArray())
    }

    @Synchronized
    fun serialize() = CompoundTag().apply {
        putString("script", script)
        putString("serializedFrame", serializedFrame)
        pendingTask?.task?.let { TickableTask.serialize(it) }?.let { put("pendingTask", it) }
        val others = ListTag()
        tickingTasks.forEach {
            others.add(TickableTask.serialize(it.task))
        }
        put("tickingTasks", others)
    }

    @Synchronized
    fun deserialize(compound: CompoundTag) = synchronized(this) {
        script = compound.getString("script")
        serializedFrame = compound.getString("serializedFrame")
        pendingTask = TaskFuture(TickableTask.deserialize(compound.getCompound("pendingTask"), this))
        val others = compound.getList("tickingTasks", 0)
        others.forEach {
            check(it is CompoundTag)
            tickingTasks.add(TaskFuture(TickableTask.deserialize(it, this@BotEnv)))
        }
    }

    companion object {
        private val BOT_THREAD_ID = AtomicInteger(0)
        var EXECUTOR_SERVICE: ExecutorService? = null

        fun onServerSetup(@Suppress("UNUSED_PARAMETER") event: ServerAboutToStartEvent) {
            EXECUTOR_SERVICE = Executors.newCachedThreadPool {
                Thread(it, "BotJS-BotThread-${BOT_THREAD_ID.getAndIncrement()}")
            }
        }

        fun onServerStop(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
            EXECUTOR_SERVICE!!.awaitTermination(500, TimeUnit.MILLISECONDS)
            EXECUTOR_SERVICE!!.shutdownNow()
            BOT_THREAD_ID.set(0)
            EXECUTOR_SERVICE = null
        }
    }

}