@file:OptIn(ExperimentalTime::class)

package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.env.task.TaskHandler
import com.sifsstudio.botjs.env.task.TickableTask
import kotlinx.coroutines.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.apache.commons.codec.binary.Base64
import org.mozilla.javascript.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

class BotEnv(val entity: BotEntity) {

    var script = ""
    var running = false
        private set
    var tickable = false
        private set
    val taskHandler = TaskHandler(this)
    private lateinit var scope: ScriptableObject
    lateinit var cacheScope: NativeObject
    private val abilities: MutableMap<String, AbilityBase> = mutableMapOf()

    //For quick matching
    private val characteristics: MutableMap<EnvCharacteristic.Key<*>, EnvCharacteristic> = mutableMapOf()
    var runJob: Job? = null
        private set
    var serializedFrame = ""

    fun install(ability: AbilityBase) = abilities.put(ability.id, ability)

    inline fun install(ability: (BotEnv) -> AbilityBase) = install(ability(this))

    fun characteristics(): Set<EnvCharacteristic.Key<*>> = characteristics.keys

    @Suppress("UNCHECKED_CAST")
    operator fun <T : EnvCharacteristic> get(key: EnvCharacteristic.Key<T>) = characteristics[key] as T?

    operator fun <T : EnvCharacteristic> set(key: EnvCharacteristic.Key<T>, char: T?) {
        if (char == null) {
            characteristics.remove(key)?.onRemovedFromEnv(this)
        } else {
            characteristics[key] = char
            char.onAddedToEnv(this)
        }
    }

    fun clearUpgrades() {
        abilities.clear()
        characteristics.forEach { (_, v) -> v.onRemovedFromEnv(this) }
        characteristics.clear()
    }

    private suspend fun run() {
        // fixme: particle not generated
        running = true
        suspendableContext { context ->
            context.optimizationLevel = -1
            characteristics.values.forEach { it.onActive(this@BotEnv) }
            scope = context.initStandardObjects().apply {
                defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
            }
            if (serializedFrame.isEmpty()) {
                try {
                    val botScript = context.compileString(script, "Bot Code", 0, null)
                    cacheScope = NativeObject()
                    context.runScriptSuspend(botScript, scope)
                } catch (pending: ContinuationPending) {
                    scope.delete("bot") // Depends on the bot entity
                    serializedFrame = getSerializedFrame(context, pending.continuation)
                    if (pending.applicationState != null) {
                        check(pending.applicationState is TaskFuture<*>)
                        taskHandler.storeReturn(pending.applicationState as TaskFuture<*>)
                    }
                } catch (exception: Exception) {
                    if (exception is WrappedException && exception.wrappedException is CancellationException || exception is CancellationException) {
                        entity.level.server?.submitAsync {
                            for (i in 0..10) {
                                val vx = entity.random.nextGaussian() * 0.02
                                val vy = entity.random.nextGaussian() * 0.02
                                val vz = entity.random.nextGaussian() * 0.02
                                entity.level.addParticle(
                                    ParticleTypes.CRIT,
                                    entity.getRandomX(1.0),
                                    entity.randomY + 1.0,
                                    entity.getRandomZ(1.0),
                                    vx,
                                    vy,
                                    vz
                                )
                            }
                        }
                    } else if (exception is WrappedException && exception.wrappedException is TimeoutException || exception is TimeoutException) {
                        entity.level.server?.submitAsync {
                            for (i in 0..10) {
                                val vx = entity.random.nextGaussian() * 0.02
                                val vy = entity.random.nextGaussian() * 0.02
                                val vz = entity.random.nextGaussian() * 0.02
                                entity.level.addParticle(
                                    ParticleTypes.SMOKE,
                                    entity.getRandomX(1.0),
                                    entity.randomY + 1.0,
                                    entity.getRandomZ(1.0),
                                    vx,
                                    vy,
                                    vz
                                )
                            }
                        }
                    } else {
                        exception.printStackTrace()
                    }
                } finally {
                    characteristics.values.forEach { it.onDeactive(this@BotEnv) }
                    running = false
                }
            } else {
                val bais = ByteArrayInputStream(Base64.decodeBase64(serializedFrame))
                val sis = EnvInputStream(this@BotEnv, bais, scope)
                val continuation = sis.readObject()
                scope = (sis.readObject() as ScriptableObject).apply {
                    defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
                }
                cacheScope = sis.readObject() as NativeObject
                try {
                    val ret = taskHandler.resume() ?: return
                    context.resumeSuspend(continuation, scope, ret)
                    serializedFrame = ""
                } catch (pending: ContinuationPending) {
                    scope.delete("bot") // Depends on the bot entity
                    serializedFrame = getSerializedFrame(context, pending.continuation)
                    if (pending.applicationState != null) {
                        check(pending.applicationState is TaskFuture<*>)
                        taskHandler.storeReturn(pending.applicationState as TaskFuture<*>)
                    }
                } catch (exception: Exception) {
                    if (exception is WrappedException && exception.wrappedException is CancellationException || exception is CancellationException) {
                        entity.level.server?.submitAsync {
                            for (i in 0..10) {
                                val vx = entity.random.nextGaussian() * 0.02
                                val vy = entity.random.nextGaussian() * 0.02
                                val vz = entity.random.nextGaussian() * 0.02
                                entity.level.addParticle(
                                    ParticleTypes.CRIT,
                                    entity.getRandomX(1.0),
                                    entity.randomY + 1.0,
                                    entity.getRandomZ(1.0),
                                    vx,
                                    vy,
                                    vz
                                )
                            }
                        }
                    } else if (exception is WrappedException && exception.wrappedException is TimeoutException || exception is TimeoutException) {
                        entity.level.server?.submitAsync {
                            for (i in 0..10) {
                                val vx = entity.random.nextGaussian() * 0.02
                                val vy = entity.random.nextGaussian() * 0.02
                                val vz = entity.random.nextGaussian() * 0.02
                                entity.level.addParticle(
                                    ParticleTypes.SMOKE,
                                    entity.getRandomX(1.0),
                                    entity.randomY + 1.0,
                                    entity.getRandomZ(1.0),
                                    vx,
                                    vy,
                                    vz
                                )
                            }
                        }
                    } else {
                        exception.printStackTrace()
                    }
                    serializedFrame = ""
                    taskHandler.reset()
                } finally {
                    characteristics.values.forEach { it.onDeactive(this@BotEnv) }
                    running = false
                }
            }
        }
    }

    fun tick() {
        if (running) {
            taskHandler.tick()
        }
    }

    fun <T : Any> submit(task: TickableTask<T>, willBlock: Boolean): TaskFuture<T> {
        if (runJob?.isCancelled == true) {
            throw CancellationException()
        }
        val cx = Context.getCurrentContext() as EnvContext
        cx.startTime = TimeSource.Monotonic.markNow()
        val result = taskHandler.submit(task)
        if (!tickable && !willBlock) {
            val pending = cx.captureContinuation()
            pending.applicationState = result
            throw pending
        }
        return result
    }

    suspend fun <T : Any> block(future: TaskFuture<T>): T {
        taskHandler.block(future)
        if (!future.isDone) {
            throw Suspension()
        }
        return future.result
    }

    fun add() {
        tickable = true
    }

    fun remove() {
        tickable = false
        taskHandler.suspend(false)
    }

    private fun getSerializedFrame(context: Context, continuation: Any): String {
        val baos = ByteArrayOutputStream()
        val sos = EnvOutputStream(this, baos, context.initStandardObjects())
        sos.writeObject(continuation)
        sos.writeObject(scope)
        sos.writeObject(cacheScope)
        return Base64.encodeBase64String(baos.toByteArray())
    }

    @Synchronized
    fun serialize() = CompoundTag().apply {
        putString("script", script)
        putString("serializedFrame", serializedFrame)
        put("tasks", taskHandler.serialize())
    }

    @Synchronized
    fun deserialize(compound: CompoundTag) {
        script = compound.getString("script")
        serializedFrame = compound.getString("serializedFrame")
        taskHandler.deserialize(compound.getCompound("tasks"))
    }

    @Synchronized
    fun terminateExecution() {
        check(running)
        taskHandler.suspend(true)
        runJob?.cancel()
    }

    @Synchronized
    fun launch() {
        if (running) {
            return
        }
        runJob = BOT_SCOPE.launch {
            run()
        }
    }

    class EnvContext(factory: ContextFactory) : Context(factory) {
        var startTime: TimeSource.Monotonic.ValueTimeMark? = null
        lateinit var environment: BotEnv
    }

    companion object {
        private val BOT_THREAD_ID = AtomicInteger(0)
        private val CTX_FACTORY: ContextFactory = object : ContextFactory() {
            override fun hasFeature(cx: Context, featureIndex: Int): Boolean {
                if (featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS) {
                    return true
                }
                return super.hasFeature(cx, featureIndex)
            }

            override fun makeContext(): Context {
                return EnvContext(this).apply { instructionObserverThreshold = 10000 }
            }

            override fun observeInstructionCount(cx: Context, instructionCount: Int) {
                val ecx = cx as EnvContext
                if (ecx.environment.runJob?.isCancelled == true) {
                    throw CancellationException()
                }
                if (ecx.startTime!!.elapsedNow() >= BotJS.CONFIG.executionTimeout) {
                    throw TimeoutException()
                }
            }

            override fun doTopCall(
                callable: Callable,
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable?,
                args: Array<out Any>
            ): Any {
                (cx as EnvContext).startTime = TimeSource.Monotonic.markNow()
                return super.doTopCall(callable, cx, scope, thisObj, args)
            }
        }
        private var BOT_DISPATCHER: CoroutineDispatcher? = null
        private lateinit var BOT_SCOPE: CoroutineScope

        init {
            ContextFactory.initGlobal(CTX_FACTORY)
        }

        fun onServerSetup(@Suppress("UNUSED_PARAMETER") event: ServerAboutToStartEvent) {
            BOT_DISPATCHER = Executors.newCachedThreadPool {
                Thread(it, "BotJS-BotThread-${BOT_THREAD_ID.getAndIncrement()}")
            }.asCoroutineDispatcher()
            BOT_SCOPE = CoroutineScope(SupervisorJob())
        }

        fun onServerStop(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
            BOT_THREAD_ID.set(0)
        }
    }
}