package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.task.*
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.apache.commons.codec.binary.Base64
import org.mozilla.javascript.*
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
    val taskHandler = TaskHandler(this)
    private lateinit var scope: ScriptableObject
    lateinit var cacheScope: NativeObject
    private val abilities: MutableMap<String, AbilityBase> = mutableMapOf()

    //For quick matching
    private val characteristics: MutableMap<EnvCharacteristic.Key<*>, EnvCharacteristic> = mutableMapOf()
    private var runFuture: Future<*>? = null
    var serializedFrame = ""

    fun install(ability: AbilityBase) = abilities.put(ability.id, ability)

    inline fun install(ability: (BotEnv) -> AbilityBase) = install(ability(this))

    fun characteristics(): Set<EnvCharacteristic.Key<*>> = characteristics.keys

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

    override fun run() {
        running = true
        val context = Context.enter()
        context.optimizationLevel = -1
        scope = context.initStandardObjects().apply {
            defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
        }
        characteristics.values.forEach { it.onActive(this) }
        if (serializedFrame.isEmpty()) {
            try {
                val botScript = context.compileString(script, "Bot Code", 0, null)
                context.executeScriptWithContinuations(botScript, scope)
            } catch (pending: ContinuationPending) {
                serializedFrame = getSerializedFrame(context, pending.continuation)
                if (pending.applicationState != null) {
                    check(pending.applicationState is TaskFuture<*>)
                    taskHandler.storeReturn(pending.applicationState as TaskFuture<*>)
                }
            } catch (exception: Exception) {
                if (exception is WrappedException && exception.wrappedException is InterruptedException) {
                    // DO NOTHING
                } else {
                    exception.printStackTrace()
                }
            } finally {
                synchronized(this) {
                    taskHandler.reset()
                }
                Context.exit()
                running = false
            }
        } else {
            val bais = ByteArrayInputStream(Base64.decodeBase64(serializedFrame))
            val sis = EnvInputStream(this, bais, scope)
            val continuation = sis.readObject()
            scope = (sis.readObject() as ScriptableObject).apply {
                defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
            }
            cacheScope = sis.readObject() as NativeObject
            try {
                val ret = taskHandler.resume()
                if(ret == Unit) {
                    return
                }
                context.resumeContinuation(continuation, scope, ret)
                serializedFrame = ""
            } catch (pending: ContinuationPending) {
                serializedFrame = getSerializedFrame(context, pending.continuation)
                if (pending.applicationState != null) {
                    check(pending.applicationState is TaskFuture<*>)
                    taskHandler.storeReturn(pending.applicationState as TaskFuture<*>)
                }
            } catch (exception: Exception) {
                if ((exception is WrappedException && exception.wrappedException is InterruptedException) || exception is InterruptedException) {
                    // DO NOTHING
                } else {
                    exception.printStackTrace()
                }
                serializedFrame = ""
            } finally {
                characteristics.values.forEach { it.onDeactive(this) }
                synchronized(this) {
                    taskHandler.reset()
                }
                Context.exit()
                running = false
            }
        }
    }

    fun tick() {
        tickable = true
        taskHandler.tick()
    }

    fun<T: Any> submit(task: TickableTask<T>): TaskFuture<T> {
        val result = taskHandler.submit(task)
        suspendIfNecessary(task)
        return result
    }

    fun <T : Any> block(future: TaskFuture<T>): T {
        val result = taskHandler.block(future)
        if (!future.isDone) {
            suspendExecution(null)
        }
        return result.result!!
    }

    fun add() {
        tickable = true
    }

    fun remove() {
        tickable = false
        taskHandler.reset()
    }

    private fun removeBotFromScope() {
        scope.delete("bot")
    }

    private fun suspendIfNecessary(data: Any?) {
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
        runFuture!!.cancel(true)
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