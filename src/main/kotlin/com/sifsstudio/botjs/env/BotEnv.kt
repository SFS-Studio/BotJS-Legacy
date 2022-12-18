package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.util.getList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
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
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BotEnv(val entity: BotEntity) : Runnable {

    var script = ""
    var running = false
        private set
    private var tickable = false
    private val tickingFutures: MutableSet<TaskFuture<*>> = mutableSetOf()
    private var pendingFuture: Pair<TaskFuture<*>, Boolean>? = null // second: is suspended
    private lateinit var scope: ScriptableObject
    private val abilities: MutableMap<String, AbilityBase> = mutableMapOf()
    private var runFuture: Future<*>? = null
    var serializedFrame = ""

    fun install(ability: AbilityBase) = abilities.put(ability.id, ability)
    fun clearAbility() = abilities.clear()

    override fun run() {
        running = true
        val context = Context.enter()
        context.optimizationLevel = -1
        scope = context.initStandardObjects().apply {
            defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
        }
        if (serializedFrame.isEmpty()) {
            try {
                val botScript = context.compileString(script, "Bot Code", 0, null)
                context.executeScriptWithContinuations(botScript, scope)
            } catch (pending: ContinuationPending) {
                serializedFrame = getSerializedFrame(context, pending.continuation)
                if (pending.applicationState != null) {
                    check(pending.applicationState is TaskFuture<*>)
                    tickingFutures.remove(pending.applicationState)
                    pendingFuture = Pair(pending.applicationState as TaskFuture<*>, true)
                }
            } catch (exception: Exception) {
                if (exception is WrappedException && exception.wrappedException is InterruptedException) {
                    // DO NOTHING
                } else {
                    exception.printStackTrace()
                }
            } finally {
                synchronized(this) {
                    tickingFutures.clear()
                    pendingFuture = null
                }
                Context.exit()
                running = false
            }
        } else {
            val bais = ByteArrayInputStream(Base64.decodeBase64(serializedFrame))
            val sis = ScriptableInputStream(bais, scope)
            val continuation = sis.readObject()
            scope = (sis.readObject() as ScriptableObject).apply {
                defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
            }
            try {
                val ret = synchronized(this) {
                    val pendingTask = pendingFuture
                    if (pendingTask != null && !pendingTask.second) {
                        val res = blockWithoutCapture(pendingTask.first)
                        if (!res.isDone) {
                            return
                        }
                        return@synchronized res.result
                    } else if (pendingTask != null) {
                        return@synchronized pendingTask.first
                    } else {
                        return@synchronized null
                    }
                }
                context.resumeContinuation(continuation, scope, ret)
                serializedFrame = ""
            } catch (pending: ContinuationPending) {
                serializedFrame = getSerializedFrame(context, pending.continuation)
                if (pending.applicationState != null) {
                    check(pending.applicationState is TaskFuture<*>)
                    tickingFutures.remove(pending.applicationState)
                    pendingFuture = Pair(pending.applicationState as TaskFuture<*>, true)
                }
            } catch (exception: Exception) {
                if ((exception is WrappedException && exception.wrappedException is InterruptedException) || exception is InterruptedException) {
                    // DO NOTHING
                } else {
                    exception.printStackTrace()
                }
                serializedFrame = ""
            } finally {
                synchronized(this) {
                    tickingFutures.clear()
                    pendingFuture = null
                }
                Context.exit()
                running = false
            }
        }
    }

    fun tick() {
        tickable = true
        synchronized(this) {
            tickingFutures.iterator().run {
                while (hasNext()) {
                    val now = next()
                    val result = now.task.tick()
                    if (result.isDone) {
                        now.done(result.result!!)
                        remove()
                    }
                }
            }
        }
        val pendingFuture = pendingFuture
        if (pendingFuture != null && !pendingFuture.second) {
            val result = pendingFuture.first.task.tick()
            if (result.isDone) {
                this.pendingFuture = null
                pendingFuture.first.done(result.result!!)
            }
        }
    }

    fun remove() {
        tickable = false
        if (pendingFuture != null) {
            pendingFuture!!.first.suspend()
            pendingFuture = null
        }
        tickingFutures.clear()
    }

    private fun removeBotFromScope() {
        scope.delete("bot")
    }

    fun <T : Any> submit(task: TickableTask<T>): TaskFuture<T> {
        val future = TaskFuture(task)
        synchronized(this) {
            tickingFutures.add(future)
        }
        suspendIfNecessary(future)
        return future
    }

    fun <T : Any> block(future: TaskFuture<T>): T {
        synchronized(this) {
            check(tickingFutures.remove(future))
            pendingFuture = Pair(future, false)
        }
        val res = future.join()
        if (!res.isDone) {
            suspendExecution(null)
        }
        return res.result!!
    }

    private fun <T : Any> blockWithoutCapture(future: TaskFuture<T>): PollResult<T> {
        pendingFuture = Pair(future, false)
        return future.join()
    }

    fun suspendIfNecessary(data: Any?) {
        if (!tickable) {
            suspendExecution(data)
        }
    }

    private fun suspendExecution(data: Any?) {
        removeBotFromScope()
        val cx = Context.getCurrentContext()
        val pending = cx.captureContinuation()
        pending.applicationState = data
        throw pending
    }

    private fun getSerializedFrame(context: Context, continuation: Any): String {
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
        pendingFuture?.let { putBoolean("pendingTaskSuspended", it.second)
            TickableTask.serialize(it.first.task) }?.let { this@apply.put("pendingTask", it) }
        val others = ListTag()
        tickingFutures.forEach {
            others.add(TickableTask.serialize(it.task))
        }
        put("tickingTasks", others)
    }

    @Synchronized
    fun deserialize(compound: CompoundTag) {
        script = compound.getString("script")
        serializedFrame = compound.getString("serializedFrame")
        pendingFuture = TickableTask.deserialize(compound.getCompound("pendingTask"), this)?.let { Pair(TaskFuture(it), compound.getBoolean("pendingTaskSuspended")) }
        if (pendingFuture?.second == true) {
            tickingFutures.add(pendingFuture?.first!!)
        }
        val others = compound.getList("tickingTasks", Tag.TAG_COMPOUND)
        others.forEach {
            check(it is CompoundTag)
            tickingFutures.add(TaskFuture(TickableTask.deserialize(it, this@BotEnv)!!))
        }
    }

    @Synchronized
    fun terminateExecution() {
        check(running)
        runFuture!!.cancel(true)
        runFuture = null
    }

    @Synchronized
    fun launch() {
        if (runFuture?.isDone == false) {
            return
        }
        runFuture = EXECUTOR_SERVICE!!.submit(this)
    }

    companion object {
        private val BOT_THREAD_ID = AtomicInteger(0)
        private val CTX_FACTORY: ContextFactory = object : ContextFactory() {
            override fun hasFeature(cx: Context?, featureIndex: Int): Boolean {
                check(cx != null)
                check(cx.factory == this)
                if (featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS) {
                    return true
                }
                return super.hasFeature(cx, featureIndex)
            }
        }
        var EXECUTOR_SERVICE: ExecutorService? = null

        init {
            ContextFactory.initGlobal(CTX_FACTORY)
        }

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