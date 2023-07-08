@file:OptIn(ExperimentalTime::class)

package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.storage.BotDataStorage
import com.sifsstudio.botjs.env.storage.BotSavedData
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.env.task.TaskHandler
import com.sifsstudio.botjs.env.task.TickableTask
import com.sifsstudio.botjs.util.ThreadLoop
import kotlinx.coroutines.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.apache.commons.codec.binary.Base64
import org.mozilla.javascript.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

class BotEnv(val entity: BotEntity) {

    val taskHandler = TaskHandler(this)
    private lateinit var scope: ScriptableObject
    lateinit var cacheScope: NativeObject
    private val abilities: MutableMap<String, AbilityBase> = mutableMapOf()

    var runJob: Job? = null
        private set
    var serializedFrame = ""

    val controller = Controller()

    //Environment characteristics
    //For quick matching
    private val chars: MutableMap<EnvCharacteristic.Key<*>, EnvCharacteristic> = mutableMapOf()

    fun chars(): Set<EnvCharacteristic.Key<*>> = chars.keys

    @Suppress("UNCHECKED_CAST")
    operator fun <T : EnvCharacteristic> get(key: EnvCharacteristic.Key<T>) = chars[key] as T?

    operator fun <T : EnvCharacteristic> set(key: EnvCharacteristic.Key<T>, char: T?) {
        if (char == null) {
            chars.remove(key)?.onRemovedFromEnv(this)
        } else {
            chars[key] = char
            char.onAddedToEnv(this)
        }
    }

    private suspend fun run() {
        // fixme: particle not generated
        suspendableContext sc@{ context ->
            context.optimizationLevel = -1
            chars.values.forEach { it.onActive(this@BotEnv) }
            while (true) {
                try {
                    if (serializedFrame.isEmpty()) {
                        LaunchMode.launchClean(context as EnvContext, this)
                    } else {
                        LaunchMode.launchResume(context as EnvContext, this)
                    }
                } catch (pending: ContinuationPending) {
                    val aS = pending.applicationState
                    check(aS == null || aS is TaskFuture<*>)
                    scope.discardRuntime() // Depends on the bot entity
                    serializedFrame = getSerializedFrame(context, pending.continuation, aS as TaskFuture<*>?)
                    if (ThreadLoop.Main.await(true) { controller.loaded }) {
                        continue
                    } else return@sc
                } catch (exception: Exception) {
                    if (exception is WrappedException && exception.wrappedException is CancellationException || exception is CancellationException) {
                        entity.genErrParticle(ParticleTypes.CRIT)
                    } else if (exception is WrappedException && exception.wrappedException is TimeoutException || exception is TimeoutException) {
                        entity.genErrParticle(ParticleTypes.SMOKE)
                    } else {
                        exception.printStackTrace()
                    }
                } finally {
                    chars.values.forEach { it.onDeactive(this@BotEnv) }
                }
                reset()
                break
            }
        }
    }

    private fun reset() {
        serializedFrame = ""
        taskHandler.reset()
    }

    private fun ScriptableObject.initRuntime() {
        defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
    }

    private fun ScriptableObject.discardRuntime() {
        delete("bot")
    }

    fun <T : Any> submit(task: TickableTask<T>, willBlock: Boolean): TaskFuture<T> {
        if (runJob?.isCancelled == true) {
            throw CancellationException()
        }
        val cx = Context.getCurrentContext() as EnvContext
        cx.startTime = TimeSource.Monotonic.markNow()
        val result = taskHandler.submit(task)
        if (!controller.runState.get().tickable && !willBlock) {
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

    //Serialization operation
    private fun getSerializedFrame(context: Context, continuation: Any, result: TaskFuture<*>?): String {
        val baos = ByteArrayOutputStream()
        val sos = EnvOutputStream(this, baos, context.initStandardObjects())
        sos.writeObject(continuation)
        sos.writeObject(scope)
        sos.writeObject(cacheScope)
        sos.writeBoolean(result != null)
        result?.let {
            sos.simpleFuture = true
            sos.writeObject(it)
        }
        return Base64.encodeBase64String(baos.toByteArray())
    }

    fun serialize() = BotSavedData(serializedFrame, taskHandler.serialize())

    fun deserialize(data: BotSavedData) {
        data.let {
            serializedFrame = it.frame
            taskHandler.deserialize(it.tasks)
        }
    }

    object LaunchMode {
        suspend fun launchClean(context: EnvContext, suspension: SuspensionContext) {
            with(suspension) {
                with(context.environment) {
                    check(serializedFrame.isEmpty())
                    scope = context.initStandardObjects()
                    scope.initRuntime()
                    val botScript = context.compileString(controller.script, "Bot Code", 0, null)
                    cacheScope = NativeObject()
                    context.runScriptSuspend(botScript, scope)
                }
            }
        }

        suspend fun launchResume(context: EnvContext, suspension: SuspensionContext) {
            with(suspension) {
                with(context.environment) {
                    check(serializedFrame.isNotEmpty())
                    val bais = ByteArrayInputStream(Base64.decodeBase64(serializedFrame))
                    val sis = EnvInputStream(this, bais, context.initStandardObjects().apply { initRuntime() })
                    val continuation = sis.readObject()
                    scope = sis.readObject() as ScriptableObject
                    scope.initRuntime()
                    cacheScope = sis.readObject() as NativeObject
                    val ret = if (sis.readBoolean()) {
                        sis.readObject() as TaskFuture<*>
                    } else taskHandler.resume()
                    context.resumeSuspend(continuation, scope, ret)
                }
            }
        }
    }

    //Handle operation from server thread
    //All state read/write across threads shall be executed on the main thread
    inner class Controller internal constructor() {
        var script = ""
        var resume = false

        //R/W across threads
        var runState = AtomicReference(EnvState.READY)
        var loaded = false
            private set

        private var reloadHandle: ThreadLoop.DisposableHandle? = null

        fun terminateExecution() {
            var cur = runState.get()
            val stateToIgnore = arrayOf(EnvState.READY, EnvState.UNLOADING)
            while (true) {
                if (cur in stateToIgnore) {
                    return
                }
                val witness = runState.compareAndExchangeAcquire(cur, EnvState.TERMINATING)
                if (witness == cur) {
                    // done
                    check(witness !in stateToIgnore)
                    if (witness == EnvState.TERMINATING) {
                        return
                    }

                    taskHandler.suspend(true)
                    runJob?.cancel()
                    break
                } else {
                    cur = witness
                }
            }
//            check(runState != EnvState.READY)
//            if(runState == EnvState.TERMINATING) return
//            runState = EnvState.TERMINATING
//            taskHandler.suspend(true)
//            runJob?.cancel()
        }

        fun launch() {
            if (runState.compareAndSet(EnvState.READY, EnvState.RUNNING)) {
                runJob = BOT_SCOPE.launch {
                    val entity = this@BotEnv.entity
                    var data = BotDataStorage.readData(entity)
                    data?.let { deserialize(it) }
                    run()
                    data = serialize()
                    BotDataStorage.writeData(entity, BotSavedData.serialize(data))
                    ThreadLoop.Main.await(true) {
                        runState.set(EnvState.READY)
                    }
                }
            }

//            if (runState.ordinal > 0) {
//                return
//            }
//            runState = EnvState.RUNNING
//            runJob = BOT_SCOPE.launch {
//                val entity = this@BotEnv.entity
//                var data = BotDataStorage.readData(entity)
//                data?.let { deserialize(it) }
//                run()
//                data = serialize()
//                BotDataStorage.writeData(entity, BotSavedData.serialize(data))
//                ThreadLoop.Main.await(true) { runState = EnvState.READY }
//            }
        }

        private fun relaunch(): Boolean {
            if (runState.get() != EnvState.READY) {
                return false
            }
            launch()
            return true

//            if(runState != EnvState.READY) {
//                return false
//            }
//            launch()
//            return true
        }

        fun add() {
            loaded = true

            when (runState.get()) {
                EnvState.UNLOADING -> {
                    if (reloadHandle == null) {
                        reloadHandle = ThreadLoop.Main.schedule(::relaunch)
                    }
                }

                EnvState.READY -> {
                    if (resume) {
                        launch()
                    }
                }

                else -> {}
            }
        }

        fun remove() {
            loaded = false

            if (runState.compareAndSet(EnvState.RUNNING, EnvState.UNLOADING)) {
                resume = true
                taskHandler.suspend(false)
            }
//            when(runState) {
//                EnvState.RUNNING -> {
//                    runState = EnvState.UNLOADING
//                    resume = true
//                    taskHandler.suspend(false)
//                }
//                else -> {}
//            }
            reloadHandle?.dispose()
            reloadHandle = null
        }

        fun tick() {
            if (runState.get() == EnvState.RUNNING) {
                taskHandler.tick()
            }
        }

        fun clearUpgrades() {
            abilities.clear()
            chars.forEach { (_, v) -> v.onRemovedFromEnv(this@BotEnv) }
            chars.clear()
        }

        fun install(ability: AbilityBase) = abilities.put(ability.id, ability)

        inline fun install(ability: (BotEnv) -> AbilityBase) = install(ability(this@BotEnv))

    }

    class EnvContext(factory: ContextFactory) : Context(factory) {
        var startTime: TimeSource.Monotonic.ValueTimeMark? = null
        lateinit var environment: BotEnv
    }

    enum class EnvState(val free: Boolean, val tickable: Boolean, val stopping: Boolean) {
        READY(true, false, false),
        RUNNING(false, true, false),
        TERMINATING(false, false, true),
        UNLOADING(false, false, true)
    }

    companion object {
        private val BOT_THREAD_ID = AtomicInteger(0)
        private val CTX_FACTORY: ContextFactory = object : ContextFactory() {
            override fun hasFeature(cx: Context, featureIndex: Int) =
                featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS || super.hasFeature(cx, featureIndex)

            override fun makeContext() = EnvContext(this).apply { instructionObserverThreshold = 10000 }

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
            //FIXME: Another mod using Rhino might break
            ContextFactory.initGlobal(CTX_FACTORY)
        }

        fun onServerSetup(@Suppress("UNUSED_PARAMETER") event: ServerAboutToStartEvent) {
            BOT_DISPATCHER = Executors.newCachedThreadPool {
                Thread(it, "BotJS-BotThread-${BOT_THREAD_ID.getAndIncrement()}")
            }.asCoroutineDispatcher()
            BOT_SCOPE = CoroutineScope(SupervisorJob() + BOT_DISPATCHER!!)
        }

        fun onServerStop(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
            BOT_THREAD_ID.set(0)
        }
    }
}