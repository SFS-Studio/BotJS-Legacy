package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.apache.commons.codec.binary.Base64
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.WrappedException
import org.mozilla.javascript.serialize.ScriptableInputStream
import org.mozilla.javascript.serialize.ScriptableOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BotEnv(val entity: BotEntity) : Runnable {

    var script = ""
    var running = false
        private set
    private var pendingTask: Pair<TickableTask<*>, Pair<ReentrantLock, Condition>>? = null
    var lastPendingTaskResult: Any? = null
    private lateinit var scope: ScriptableObject
    private var abilities: MutableMap<String, AbilityBase> = mutableMapOf()
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
                val pendingTask = pendingTask
                if (pendingTask != null) {
                    running = true
                    val lockPair = pendingTask.second
                    lockPair.first.withLock {
                        lockPair.second.await()
                    }
                }
                context.resumeContinuation(continuation, scope, lastPendingTaskResult)
                ""
            } catch (pending: ContinuationPending) {
                val baos = ByteArrayOutputStream()
                val sos = ScriptableOutputStream(baos, context.initStandardObjects())
                sos.writeObject(pending.continuation)
                sos.writeObject(scope)
                Base64.encodeBase64String(baos.toByteArray())
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

    fun tick() {
        val result = pendingTask?.first?.tick()
        val lockPair = pendingTask?.second
        if (result != null) {
            if (result.isDone) {
                synchronized(this) {
                    lastPendingTaskResult = result.result
                    pendingTask = null
                }
                lockPair?.first?.withLock {
                    lockPair.second.signalAll()
                }
            }
        }
    }

    fun remove() {
        val lockPair = pendingTask?.second
        lockPair?.first?.withLock {
            lockPair.second.signalAll()
        }
    }

    fun removeBotFromScope() {
        scope.delete("bot")
    }

    fun setPendingTask(newTask: TickableTask<*>, newLock: ReentrantLock, newCondition: Condition) = synchronized(this) {
        if (pendingTask == null) {
            pendingTask = Pair(newTask, Pair(newLock, newCondition))
        } else {
            throw IllegalStateException()
        }
    }

    fun isFree(): Boolean = pendingTask == null

    fun serialize() = CompoundTag().apply {
        putString("script", script)
        putString("serializedFrame", serializedFrame)
        putString("pendingTask", pendingTask?.first?.id.orEmpty())
        pendingTask?.first?.serialize()?.let { put("task", it) }
    }

    fun deserialize(compound: CompoundTag) = synchronized(this) {
        script = compound.getString("script")
        serializedFrame = compound.getString("serializedFrame")
        val pendingTaskId = compound.getString("pendingTask")
        if (pendingTaskId.isNotEmpty()) {
            val task = TaskRegistry.constructTask(pendingTaskId, this)!!
            task.deserialize(compound.get("task")!!)
            val lock = ReentrantLock()
            pendingTask = Pair(task, Pair(lock, lock.newCondition()))
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